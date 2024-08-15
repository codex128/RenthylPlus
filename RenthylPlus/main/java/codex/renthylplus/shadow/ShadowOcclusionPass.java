/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.GeometryQueue;
import codex.renthyl.client.GraphSource;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.ResourceTicket;
import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
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
public abstract class ShadowOcclusionPass <T extends Light> extends RenderPass {
    
    protected final Light.Type lightType;
    protected final int numShadowMaps;
    protected final ShadowMapDef shadowMapDef = new ShadowMapDef();
    private final RenderState renderState = new RenderState();
    private ResourceTicket<T> light;
    private ResourceTicket<GeometryQueue> occluders;
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
    }
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        occluders = addInput("Occluders");
        addOutputGroup("ShadowMaps", numShadowMaps);
        material = new Material(frameGraph.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        for (ResourceTicket t : getGroupArray("ShadowMaps")) {
            declare(shadowMapDef, t);
            reserve(t);
        }
        reference(occluders);
    }
    @Override
    protected void execute(FGRenderContext context) {
        Camera viewCam = context.getViewPort().getCamera();
        T l = resources.acquireOrElse(light, (lightSource != null
                ? lightSource.getGraphValue(frameGraph, context.getViewPort()) : null));
        GeometryQueue occluderQueue = resources.acquire(occluders);
        ResourceTicket<ShadowMap>[] mapTickets = getGroupArray("ShadowMaps");
        TempVars vars = TempVars.get();
        if (l == null || !l.intersectsFrustum(viewCam, vars)) {
            vars.release();
            resources.setUndefined(mapTickets);
            return;
        }
        vars.release();
        boolean containsAll = lightSourceInsideFrustum(viewCam, l);
        RenderManager rm = context.getRenderManager();
        Renderer renderer = context.getRenderer();
        rm.setForcedRenderState(renderState);
        rm.setForcedMaterial(material);
        int w = shadowMapDef.getMapDef().getWidth();
        int h = shadowMapDef.getMapDef().getHeight();
        for (int i = 0; i < numShadowMaps; i++) {
            Camera shadowCam = getShadowCamera(context, occluderQueue, l, i);
            // get the framebuffer now, so it won't be culled
            FrameBuffer fb = getFrameBuffer(i, w, h, 1);
            if (containsAll || frustumIntersect(viewCam, shadowCam)) {
                shadowCam.resize(w, h, true, true);
                shadowCam.update();
                shadowCam.updateViewProjection();
                ShadowMap map = resources.acquire(mapTickets[i]);
                map.setLight(l);
                map.setProjection(shadowCam.getViewProjectionMatrix());
                map.setRange(shadowCam.getFrustumNear(), shadowCam.getFrustumFar());
                FrameBuffer.RenderBuffer current = fb.getDepthTarget();
                if (current == null || current.getTexture() != map.getMap()) {
                    fb.setDepthTarget(FrameBuffer.target(map.getMap()));
                    fb.setUpdateNeeded();
                }
                renderer.setFrameBuffer(fb);
                renderer.clearBuffers(true, true, true);
                rm.setCamera(shadowCam, false);
                System.out.println("rendering "+occluderQueue.getNumGeometries()+" geometries for shadows");
                System.out.println("  framebuffer: "+fb.getWidth()+", "+fb.getHeight());
                System.out.println("  texture: "+map.getMap().getImage().getWidth()+", "+map.getMap().getImage().getHeight());
                System.out.println("  camera: "+shadowCam.getWidth()+", "+shadowCam.getHeight());
                System.out.println("  current camera is shadow cam: "+(rm.getCurrentCamera() == shadowCam));
                System.out.println("  rendered shadow range: "+map.getRange());
                context.renderGeometry(occluderQueue, shadowCam, (RenderManager rm1, Geometry geom) -> {
                    System.out.println("    rendered shadow occlusion geometry.");
                    rm1.renderGeometry(geom);
                    return true;
                });
            } else {
                resources.setUndefined(mapTickets[i]);
            }
        }
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
    protected abstract boolean lightSourceInsideFrustum(Camera cam, T light);
    protected abstract Camera getShadowCamera(FGRenderContext context, GeometryQueue occluders, T light, int index);
    
    private boolean frustumIntersect(Camera cam1, Camera cam2) {
        return true;
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
    
}
