/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.GeometryQueue;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.ResourceTicket;
import com.jme3.light.Light;
import com.jme3.material.RenderState;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
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
    private final RenderState forcedRenderState = new RenderState();
    private ResourceTicket<T> light;
    private ResourceTicket<GeometryQueue> occluders;
    
    private float renderDistance = -1;

    public ShadowOcclusionPass(Light.Type lightType, int numShadowMaps, int shadowMapSize) {
        this.lightType = lightType;
        this.numShadowMaps = numShadowMaps;
        this.shadowMapDef.getMapDef().setSquare(shadowMapSize);
    }
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        occluders = addInput("Occluders");
        addOutputGroup("ShadowMaps", numShadowMaps);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        ResourceTicket[] maps = getGroupArray("ShadowMaps");
        ResourceTicket[] mats = getGroupArray("ProjectionMatrices");
        for (int i = 0; i < numShadowMaps; i++) {
            declare(shadowMapDef, maps[i]);
            declare(null, mats[i]);
            reserve(maps[i]);
        }
        reference(occluders);
    }
    @Override
    protected void execute(FGRenderContext context) {
        Camera viewCam = context.getViewPort().getCamera();
        T l = resources.acquire(light);
        GeometryQueue occluderQueue = resources.acquire(occluders);
        ResourceTicket<ShadowMap>[] mapTickets = getGroupArray("ShadowMaps");
        if (lightType != Light.Type.Directional) {
            TempVars vars = TempVars.get();
            if (!l.intersectsFrustum(viewCam, vars)) {
                vars.release();
                resources.setUndefined(mapTickets);
                return;
            }
            vars.release();
        }
        boolean containsAll = lightSourceInsideFrustum(viewCam, l);
        RenderManager rm = context.getRenderManager();
        Renderer renderer = context.getRenderer();
        rm.setForcedRenderState(forcedRenderState);
        for (int i = 0; i < numShadowMaps; i++) {
            Camera shadowCam = getShadowCamera(context, occluderQueue, l, i);
            // get the framebuffer now, so it won't be culled
            FrameBuffer fb = getFrameBuffer(i, shadowMapDef.getMapDef().getWidth(), shadowMapDef.getMapDef().getHeight(), 1);
            if (containsAll || frustumIntersect(viewCam, shadowCam)) {
                ShadowMap map = resources.acquire(mapTickets[i]);
                map.setLight(l);
                map.setProjection(shadowCam.getViewProjectionMatrix());
                FrameBuffer.RenderBuffer current = fb.getDepthTarget();
                if (current == null || current.getTexture() != map.getMap()) {
                    fb.setDepthTarget(FrameBuffer.target(map.getMap()));
                    fb.setUpdateNeeded();
                }
                renderer.setFrameBuffer(fb);
                renderer.clearBuffers(true, true, true);
                context.setCamera(shadowCam, false, false);
                context.renderGeometry(occluderQueue, shadowCam, null);
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
    
    public void setRenderDistance(float renderDistance) {
        this.renderDistance = renderDistance;
    }
    
    public float getRenderDistance() {
        return renderDistance;
    }
    
}
