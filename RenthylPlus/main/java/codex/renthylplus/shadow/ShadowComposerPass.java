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
import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Vector2f;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import java.util.HashMap;

/**
 *
 * @author gary
 */
public class ShadowComposerPass extends RenderPass {
    
    private static final int MAX_SHADOW_LIGHTS = 32;
    
    private ResourceTicket<GeometryQueue> receivers;
    private final ResourceTicket<Texture2D> sceneDepth = new ResourceTicket<>();
    private ResourceTicket<Texture2D> lightContribution;
    private ResourceTicket<HashMap<Light, Integer>> lightShadowIndices;
    private final TextureDef<Texture2D> sceneDepthDef = TextureDef.texture2D();
    private final TextureDef<Texture2D> contributionDef = TextureDef.texture2D();
    private final RenderState renderState = new RenderState();
    private final HashMap<Light, Integer> indexMap = new HashMap<>();
    private Material material;
    private final Vector2f tempInvRange = new Vector2f();
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        receivers = addInput("Receivers");
        addInputList("ShadowMaps");
        lightContribution = addOutput("LightContribution");
        lightShadowIndices = addOutput("LightShadowIndices");
        sceneDepthDef.setFormat(Image.Format.Depth);
        contributionDef.setFormat(Image.Format.RGBA32F);
        contributionDef.setMagFilter(Texture.MagFilter.Nearest);
        contributionDef.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        renderState.setBlendMode(RenderState.BlendMode.Off);
        renderState.setDepthTest(false);
        renderState.setDepthWrite(false);
        material = new Material(frameGraph.getAssetManager(), "RenthylPlus/MatDefs/Shadows/ShadowCompose.j3md");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declareTemporary(sceneDepthDef, sceneDepth);
        declare(contributionDef, lightContribution);
        declare(null, lightShadowIndices);
        reserve(lightContribution);
        reference(receivers);
        referenceOptional(getGroupArray("ShadowMaps"));
    }
    @Override
    protected void execute(FGRenderContext context) {
        
        int w = context.getWidth();
        int h = context.getHeight();
        sceneDepthDef.setSize(w, h);
        contributionDef.setSize(w, h);
        
        // render the geometries to a depth texture
        FrameBuffer sceneDepthFb = getFrameBuffer("SceneDepth", w, h, 1);
        Texture2D depth = resources.acquireDepthTarget(sceneDepthFb, sceneDepth);
        context.getRenderer().setFrameBuffer(sceneDepthFb);
        context.getRenderer().clearBuffers(true, true, true);
        GeometryQueue rec = resources.acquire(receivers);
        context.renderGeometry(rec, null, null);
        
        // setup render parameters
        FrameBuffer composerFb = getFrameBuffer("Composer", w, h, 1);
        resources.acquireColorTarget(composerFb, lightContribution);
        context.getRenderer().setFrameBuffer(composerFb);
        context.getRenderer().clearBuffers(true, true, true);
        context.getRenderManager().setForcedRenderState(renderState);
        material.setTexture("SceneDepthMap", depth);
        material.setMatrix4("CamViewProjectionInverse", context.getViewPort().getCamera().getViewProjectionMatrix().invert());
        
        // fullscreen render for each shadow map
        int nextIndex = 0;
        
        ShadowMap[] maps = acquireArrayOrElse("ShadowMaps", n -> new ShadowMap[n], null);
        for (ShadowMap m : maps) {
            if (m == null) {
                continue;
            }
            Integer i = indexMap.get(m.getLight());
            if (i == null && nextIndex < MAX_SHADOW_LIGHTS) {
                i = nextIndex++;
                indexMap.put(m.getLight(), i);
            }
            if (i != null) {
                material.setTexture("ShadowMap", m.getMap());
                material.setMatrix4("LightViewProjectionMatrix", m.getProjection());
                material.setInt("LightType", m.getLight().getType().getId());
                material.setInt("LightIndex", i);
                material.setVector2("LightRangeInverse", inverse(m.getRange(), tempInvRange));
                context.renderFullscreen(material);
                renderState.setBlendMode(RenderState.BlendMode.Additive);
            }
        }
        renderState.setBlendMode(RenderState.BlendMode.Off);
        
        resources.setPrimitive(lightShadowIndices, indexMap);
        
    }
    @Override
    protected void reset(FGRenderContext context) {
        indexMap.clear();
    }
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
    private static Vector2f inverse(Vector2f vec, Vector2f store) {
        if (store == null) {
            store = new Vector2f();
        }
        store.x = 1f / vec.x;
        store.y = 1f / vec.y;
        return store;
    }
    
}
