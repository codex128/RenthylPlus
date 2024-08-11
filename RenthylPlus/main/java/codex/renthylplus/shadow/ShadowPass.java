/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.GeometryQueue;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.ResourceTicket;
import com.jme3.asset.AssetManager;
import com.jme3.light.Light;
import com.jme3.light.LightFilter;
import com.jme3.light.NullLightFilter;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector2f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.GeometryRenderHandler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.shadow.CompareMode;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture.ShadowCompareMode;
import com.jme3.texture.Texture2D;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author codex
 * @param <T>
 */
public abstract class ShadowPass <T extends Light> extends RenderPass {
    
    private static final LightFilter NULL_LIGHT_FILTER = new NullLightFilter();
    
    private final Light.Type lightType;
    
    private AssetManager assetManager;
    private ResourceTicket<GeometryQueue> occluders, receivers;
    private ResourceTicket<T> light;
    private final TextureDef<Texture2D> shadowMapDef = TextureDef.texture2D();
    private final OcclusionHandler occlusionHandler = new OcclusionHandler();
    private final ReceiverHandler receiverHandler = new ReceiverHandler();
    private final HashSet<Material> matCache = new HashSet<>();
    private Texture[] shadowMaps;
    protected T lightSource;
    
    private Material preShadowMat, postShadowMat;
    protected int nbShadowMaps, shadowMapSize;
    private Texture2D dummyTex;
    private Matrix4f[] lightViewProjectionMatrices;
    private final RenderState forcedRenderState = new RenderState();
    
    protected float zFarOverride = 0;
    private Vector2f fadeInfo;
    private float fadeLength;
    private boolean needsFallbackMaterial = false;
    
    private float shadowIntensity = 0.7f;
    private float edgesThickness = 1.0f;
    private boolean renderBackFacesShadows = false;
    private EdgeFilteringMode edgeFilteringMode = EdgeFilteringMode.Bilinear;
    private CompareMode shadowCompareMode = CompareMode.Hardware;

    public ShadowPass(Light.Type lightType, int nbShadowMaps, int shadowMapSize) {
        this.lightType = lightType;
        this.nbShadowMaps = nbShadowMaps;
        this.shadowMapSize = shadowMapSize;
        switch (this.lightType) {
            case Directional:
            case Point:
            case Spot: break;
            default: throw new IllegalArgumentException(lightType+" cannot cast shadows.");
        }
    }
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        
        occluders = addInput("Occluders");
        receivers = addInput("Receivers");
        addOutputGroup("ShadowMaps", nbShadowMaps);
        
        assetManager = frameGraph.getAssetManager();
        
        shadowMaps = new Texture2D[nbShadowMaps];
        lightViewProjectionMatrices = new Matrix4f[nbShadowMaps];

        //DO NOT COMMENT THIS (it prevents the OSX incomplete read-buffer crash)
        //dummyTex = new Texture2D(shadowMapSize, shadowMapSize, Format.RGBA8);

        preShadowMat = new Material(assetManager, "Common/MatDefs/Shadow/PreShadow.j3md");
        postShadowMat = new Material(assetManager, "Common/MatDefs/Shadow/PostShadow.j3md");
        postShadowMat.setFloat("ShadowMapSize", shadowMapSize);
        
        shadowMapDef.setSquare(shadowMapSize);
        shadowMapDef.setFormat(Image.Format.Depth);

        setShadowCompareMode(shadowCompareMode);
        setEdgeFilteringMode(edgeFilteringMode);
        setShadowIntensity(shadowIntensity);
        initForcedRenderState();
        setRenderBackFacesShadows(renderBackFacesShadows);
        
    }
    @Override
    protected void prepare(FGRenderContext context) {
        for (ResourceTicket<Texture2D> t : getGroupArray("ShadowMaps")) {
            declare(shadowMapDef, t);
            reserve(t);
        }
        reference(occluders, light);
    }
    @Override
    protected void execute(FGRenderContext context) {
        
        ViewPort vp = context.getViewPort();
        GeometryQueue occluderQueue = resources.acquireOrElse(occluders, null);
        GeometryQueue receiverQueue = resources.acquireOrElse(receivers, null);
        lightSource = resources.acquire(light);
        
        if (occluderQueue == null || receiverQueue == null
                || !occluderQueue.containsGeometry() || !receiverQueue.containsGeometry()) {
            acquireArray("ShadowMaps", new Texture2D[nbShadowMaps]);
            return;
        }
        
        if (lightType != Light.Type.Directional) {
            float zFar = setCameraFar(vp.getCamera(), zFarOverride);
            boolean intersect = isLightInView(vp.getCamera(), lightSource);
            setCameraFar(vp.getCamera(), zFar);
            acquireArray("ShadowMaps", new Texture2D[nbShadowMaps]);
            if (!intersect) return;
        }
        
        RenderManager rm = context.getRenderManager();

        configureShadowCams(lightSource, vp.getCamera());
        
        rm.setForcedMaterial(preShadowMat);
        rm.setForcedTechnique("PreShadow");
        
        for (int i = 0; i < nbShadowMaps; i++) {
            updateShadowCam(vp, occluderQueue, receiverQueue, i);
            Camera shadowCam = getShadowCam(i);
            lightViewProjectionMatrices[i].set(shadowCam.getViewProjectionMatrix());
            if (shadowCam != rm.getCurrentCamera()) {
                rm.setCamera(shadowCam, false);
            }
            FrameBuffer fb = getFrameBuffer(i, shadowMapSize, shadowMapSize, 1);
            shadowMaps[i] = resources.acquireDepthTarget(fb, getGroupArray("ShadowMaps")[i]);
            rm.getRenderer().setFrameBuffer(fb);
            rm.getRenderer().clearBuffers(true, true, true);
            rm.setForcedRenderState(forcedRenderState);
            context.renderGeometry(occluderQueue, shadowCam, occlusionHandler);
        }
        
        rm.setCamera(vp.getCamera(), false);
        if (needsFallbackMaterial) {
            rm.setForcedMaterial(postShadowMat);
        }
        rm.setForcedTechnique("PostShadow");
        
        context.renderGeometry(occluderQueue, vp.getCamera(), receiverHandler);

        for (Material m : matCache) {
            cleanupReceiverMaterial(m);
        }
        matCache.clear();
        
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
    protected abstract void configureShadowCams(T light, Camera viewCam);
    protected abstract void updateShadowCam(ViewPort viewPort, GeometryQueue occluders, GeometryQueue receivers, int shadowMapIndex);
    protected abstract boolean isLightInView(Camera viewCam, T light);
    protected abstract Camera getShadowCam(int shadowMapIndex);
    
    private void initForcedRenderState() {
        forcedRenderState.setFaceCullMode(RenderState.FaceCullMode.Front);
        forcedRenderState.setColorWrite(false);
        forcedRenderState.setDepthWrite(true);
        forcedRenderState.setDepthTest(true);
    }
    private float setCameraFar(Camera cam, float far) {
        float prev = cam.getFrustumFar();
        if (far != prev) {
            cam.setFrustumFar(far);
        }
        return prev;
    }
    
    protected void setupReceiverMaterial(Material mat) {
        mat.setFloat("ShadowMapSize", shadowMapSize);
        for (int i = 0; i < nbShadowMaps; i++) {
            mat.setMatrix4("LightViewProjectionMatrix"+i, lightViewProjectionMatrices[i]);
            mat.setTexture("ShadowMap"+i, shadowMaps[i]);
        }
        mat.setBoolean("HardwareShadows", shadowCompareMode == CompareMode.Hardware);
        mat.setInt("FilterMode", edgeFilteringMode.getMaterialParamValue());
        mat.setFloat("PCFEdge", edgesThickness);
        mat.setFloat("ShadowIntensity", shadowIntensity);
        if (fadeInfo != null) {
            mat.setVector2("FadeInfo", fadeInfo);
        }
    }
    protected void cleanupReceiverMaterial(Material mat) {
        for (int i = 0; i < nbShadowMaps; i++) {
            mat.clearParam("lightViewProjectionMatrix"+i);
            mat.clearParam("ShadowMap"+i);
        }
        mat.clearParam("FadeInfo");
    }
    
    public void setEdgeFilteringMode(EdgeFilteringMode filterMode) {
        if (filterMode == null) {
            throw new IllegalArgumentException("filterMode cannot be null");
        }
        this.edgeFilteringMode = filterMode;
        postShadowMat.setInt("FilterMode", filterMode.getMaterialParamValue());
        postShadowMat.setFloat("PCFEdge", edgesThickness);
        if (shadowCompareMode == CompareMode.Hardware) {
            if (filterMode == EdgeFilteringMode.Bilinear) {
                shadowMapDef.setMagFilter(MagFilter.Bilinear);
                shadowMapDef.setMinFilter(MinFilter.BilinearNoMipMaps);
            } else {
                shadowMapDef.setMagFilter(MagFilter.Nearest);
                shadowMapDef.setMinFilter(MinFilter.NearestNoMipMaps);
            }
        }
    }
    public void setShadowCompareMode(CompareMode compareMode) {
        if (compareMode == null) {
            throw new IllegalArgumentException("Shadow compare mode cannot be null");
        }
        this.shadowCompareMode = compareMode;
        if (compareMode == CompareMode.Hardware) {
            shadowMapDef.setShadowCompare(ShadowCompareMode.LessOrEqual);
            if (edgeFilteringMode == EdgeFilteringMode.Bilinear) {
                shadowMapDef.setMagFilter(MagFilter.Bilinear);
                shadowMapDef.setMinFilter(MinFilter.BilinearNoMipMaps);
            } else {
                shadowMapDef.setMagFilter(MagFilter.Nearest);
                shadowMapDef.setMinFilter(MinFilter.NearestNoMipMaps);
            }
        } else {
            shadowMapDef.setShadowCompare(ShadowCompareMode.Off);
            shadowMapDef.setMagFilter(MagFilter.Nearest);
            shadowMapDef.setMinFilter(MinFilter.NearestNoMipMaps);
        }
        postShadowMat.setBoolean("HardwareShadows", compareMode == CompareMode.Hardware);
    }
    public void setRenderBackFacesShadows(boolean renderBackFacesShadows) {
        if (this.renderBackFacesShadows = renderBackFacesShadows) {
            forcedRenderState.setPolyOffset(5, 3);
            forcedRenderState.setFaceCullMode(RenderState.FaceCullMode.Back);
        } else {
            forcedRenderState.setPolyOffset(0, 0);
            forcedRenderState.setFaceCullMode(RenderState.FaceCullMode.Front);
        }
    }
    public void setShadowIntensity(float shadowIntensity) {
        this.shadowIntensity = shadowIntensity;
        postShadowMat.setFloat("ShadowIntensity", shadowIntensity);
    }
    public void setShadowZExtend(float zFar) {
        this.zFarOverride = zFar;
        if (zFarOverride == 0) {
            fadeInfo = null;
        } else if (fadeInfo != null) {
            fadeInfo.set(zFarOverride - fadeLength, 1f / fadeLength);
        }
    }
    public void setShadowZFadeLength(float length) {
        if (length == 0) {
            fadeInfo = null;
            fadeLength = 0;
            postShadowMat.clearParam("FadeInfo");
        } else {
            if (zFarOverride == 0) {
                fadeInfo = new Vector2f(0, 0);
            } else {
                fadeInfo = new Vector2f(zFarOverride - length, 1.0f / length);
            }
            fadeLength = length;
            postShadowMat.setVector2("FadeInfo", fadeInfo);
        }
    }
    
    public EdgeFilteringMode getEdgeFilteringMode() {
        return edgeFilteringMode;
    }
    public CompareMode getShadowCompareMode() {
        return shadowCompareMode;
    }
    
    private class OcclusionHandler implements GeometryRenderHandler {
        
        @Override
        public boolean renderGeometry(RenderManager rm, Geometry geom) {
            Spatial spatial = geom;
            while (spatial != null) {
                ShadowMode sm = spatial.getShadowMode();
                if (sm == ShadowMode.Inherit) {
                    spatial = spatial.getParent();
                    continue;
                }
                if (sm == ShadowMode.CastAndReceive || sm == ShadowMode.Cast) {
                    rm.renderGeometry(geom);
                    return true;
                }
                break;
            }
            return false;
        }
        
    }
    private class ReceiverHandler implements GeometryRenderHandler {

        @Override
        public boolean renderGeometry(RenderManager rm, Geometry geom) {
            Spatial spatial = geom;
            while (spatial != null) {
                ShadowMode sm = spatial.getShadowMode();
                if (sm == ShadowMode.Inherit) {
                    spatial = spatial.getParent();
                    continue;
                }
                if (sm == ShadowMode.CastAndReceive || sm == ShadowMode.Cast) {
                    if (matCache.add(geom.getMaterial())) {
                        setupReceiverMaterial(geom.getMaterial());
                    }
                    rm.renderGeometry(geom);
                    return true;
                }
                break;
            }
            return false;
        }
        
    }
    
}
