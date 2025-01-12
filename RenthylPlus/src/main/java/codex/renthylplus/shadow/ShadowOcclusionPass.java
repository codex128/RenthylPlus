/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.GeometryQueue;
import codex.renthyl.client.GraphSource;
import codex.renthyl.draw.RenderMode;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.tickets.ResourceTicket;
import codex.renthyl.resources.tickets.TicketArray;
import codex.renthyl.resources.tickets.TicketGroup;
import codex.renthyl.util.GeometryRenderHandler;
import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Plane;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.scene.Geometry;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;
import com.jme3.util.TempVars;

/**
 *
 * @author gary
 * @param <T>
 */
public abstract class ShadowOcclusionPass <T extends Light> extends RenderPass implements GeometryRenderHandler {
    
    protected final Light.Type lightType;
    protected final int numShadowMaps;
    protected final ShadowMapDef shadowMapDef = new ShadowMapDef();
    private final RenderState renderState = new RenderState();
    private ResourceTicket<T> light;
    private ResourceTicket<GeometryQueue> occluders, receivers;
    private TicketArray<ShadowMap> shadowMaps;
    private GraphSource<T> lightSource;
    private Material material;
    
    private float renderDistance = -1;

    public ShadowOcclusionPass(Light.Type lightType, int numShadowMaps, int shadowMapSize) {
        this.lightType = lightType;
        this.numShadowMaps = numShadowMaps;
        this.shadowMapDef.getMapDef().setSquare(shadowMapSize);
        this.shadowMapDef.getMapDef().setWrap(Texture.WrapMode.EdgeClamp);
        //renderState.setFaceCullMode(RenderState.FaceCullMode.Front);
        renderState.setColorWrite(false);
        renderState.setDepthWrite(true);
        renderState.setDepthTest(true);
        //renderState.setFaceCullMode(RenderState.FaceCullMode.Front);
    }
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        occluders = addInput("Occluders");
        receivers = addInput("Receivers");
        shadowMaps = addOutputGroup(new TicketArray<>("ShadowMaps", numShadowMaps));
        material = new Material(frameGraph.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        for (ResourceTicket<ShadowMap> t : shadowMaps) {
            declare(shadowMapDef, t);
            reserve(t);
        }
        reference(occluders);
    }
    @Override
    protected void execute(FGRenderContext context) {
        RenderManager rm = context.getRenderManager();
        Camera viewCam = rm.getCurrentCamera();
        T l = resources.acquireOrElse(light, (lightSource != null
                ? lightSource.getGraphValue(context) : null));
        GeometryQueue occluderQueue = resources.acquire(occluders);
        GeometryQueue receiverQueue = resources.acquire(receivers);
        //ResourceTicket<ShadowMap>[] mapTickets = getGroupArray("ShadowMaps");
        TempVars vars = TempVars.get();
        if (l == null || !l.intersectsFrustum(viewCam, vars)) {
            vars.release();
            // acquire maps even though they aren't being rendered to
            for (int i = 0; i < numShadowMaps; i++) {
                Camera shadowCam = getShadowCamera(context, viewCam, occluderQueue, receiverQueue, l, i);
                acquireShadowMap(shadowCam, l, shadowMaps.get(i), i);
            }
            return;
        }
        vars.release();
        boolean containsAll = lightSourceInsideFrustum(viewCam, l);
        Renderer renderer = context.getRenderer();
        FrameBuffer originalFb = renderer.getCurrentFrameBuffer();
        context.registerMode(RenderMode.forcedRenderState(renderState));
        context.registerMode(RenderMode.forcedTechnique("PreShadow"));
        context.registerMode(RenderMode.forcedMaterial(material));
        int w = shadowMapDef.getMapDef().getWidth();
        int h = shadowMapDef.getMapDef().getHeight();
        for (int i = 0; i < numShadowMaps; i++) {
            FrameBuffer fb = getFrameBuffer(i, w, h, 1);
            Camera shadowCam = getShadowCamera(context, viewCam, occluderQueue, receiverQueue, l, i);
            // always acquire shadow maps so that errors don't occur down the pipeline
            ShadowMap map = acquireShadowMap(shadowCam, l, shadowMaps.get(i), i);
            if (containsAll || frustumIntersect(viewCam, shadowCam)) {
                rm.setCamera(shadowCam, false);
                FrameBuffer.RenderBuffer current = fb.getDepthTarget();
                if (current == null || current.getTexture() != map.getMap()) {
                    fb.setDepthTarget(FrameBuffer.FrameBufferTarget.newTarget(map.getMap()));
                    fb.setUpdateNeeded();
                }
                renderer.setFrameBuffer(fb);
                context.clearBuffers(false, true, false);
                occluderQueue.render(context, this);
            }
        }
        renderer.setFrameBuffer(originalFb);
        context.setCamera(viewCam);
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public void renderGeometry(FGRenderContext context, Geometry g) {
        context.getRenderManager().renderGeometry(g);
    }
    
    protected abstract boolean lightSourceInsideFrustum(Camera cam, T light);
    protected abstract Camera getShadowCamera(FGRenderContext context, Camera viewCam, GeometryQueue occluders, GeometryQueue receivers, T light, int index);
    
    protected boolean frustumIntersect(Camera cam1, Camera cam2) {
        return true;
    }
    protected ShadowMap acquireShadowMap(Camera cam, T light, ResourceTicket<ShadowMap> ticket, int i) {
        ShadowMap map = resources.acquire(ticket);
        map.setLight(light);
        map.setProjection(cam.getViewProjectionMatrix());
        map.setRange(cam.getFrustumNear(), cam.getFrustumFar());
        return map;
    }
    
    public void setLightSource(GraphSource<T> lightSource) {
        this.lightSource = lightSource;
    }
    public void setRenderDistance(float renderDistance) {
        this.renderDistance = renderDistance;
    }
    
    public GraphSource<T> getLightSource() {
        return lightSource;
    }
    public float getRenderDistance() {
        return renderDistance;
    }
    
    public static boolean pointInsideFrustum(Camera cam, Vector3f point) {
        for (int i = 0; i < 6; i++) {
            if (cam.getWorldPlane(i).whichSide(point) == Plane.Side.Negative) {
                return false;
            }
        }
        return true;
    }
    
}
