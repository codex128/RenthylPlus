/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.draw.RenderMode;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.tickets.DynamicTicketList;
import codex.renthyl.resources.tickets.ResourceTicket;
import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Vector2f;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

/**
 *
 * @author gary
 */
public class ShadowComposerPass extends RenderPass {
    
    private static final int MAX_SHADOW_LIGHTS = 32;
    
    private ResourceTicket<Texture2D> recieverDepth;
    private ResourceTicket<Texture2D> lightContribution;
    private ResourceTicket<Light[]> lightShadowIndices;
    private DynamicTicketList<ShadowMap> shadowMaps;
    private final TextureDef<Texture2D> contributionDef = TextureDef.texture2D();
    private final RenderState renderState = new RenderState();
    private Light[] indexMap;
    private Material material;
    private final Vector2f tempInvRange = new Vector2f();
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        recieverDepth = addInput("ReceiverDepth");
        shadowMaps = addInputGroup(new DynamicTicketList<>("ShadowMaps"));
        lightContribution = addOutput("LightContribution");
        lightShadowIndices = addOutput("LightShadowIndices");
        contributionDef.setFormat(Image.Format.R32F);
        contributionDef.setMagFilter(Texture.MagFilter.Nearest);
        contributionDef.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        renderState.setBlendMode(RenderState.BlendMode.Off);
        renderState.setDepthTest(false);
        renderState.setDepthWrite(false);
        material = new Material(frameGraph.getAssetManager(), "RenthylPlus/MatDefs/Shadows/ShadowCompose.j3md");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(contributionDef, lightContribution);
        declarePrimitive(lightShadowIndices);
        reserve(lightContribution);
        reference(recieverDepth);
        referenceOptional(shadowMaps);
    }
    @Override
    protected void execute(FGRenderContext context) {
        
        int w = context.getWidth();
        int h = context.getHeight();
        contributionDef.setSize(w, h);
        
        // setup render parameters
        FrameBuffer composerFb = getFrameBuffer("Composer", w, h, 1);
        resources.acquireColorTarget(composerFb, lightContribution);
        context.registerMode(RenderMode.frameBuffer(composerFb));
        context.clearBuffers();
        context.registerMode(RenderMode.forcedRenderState(renderState));
        material.setTexture("SceneDepthMap", resources.acquire(recieverDepth));
        material.setMatrix4("CamViewProjectionInverse", context.getViewPort().getCamera().getViewProjectionMatrix().invert());
        
        // fullscreen render for each shadow map
        int nextIndex = 0;
        ShadowMap[] maps = acquireArrayOrElse(shadowMaps, n -> new ShadowMap[n], null);
        indexMap = new Light[Math.min(maps.length, MAX_SHADOW_LIGHTS)];
        for (ShadowMap m : maps) {
            if (m == null) {
                System.out.println("shadow map is null: skipping");
                continue;
            }
            int i = indexOf(indexMap, m.getLight());
            if (i < 0 && nextIndex < MAX_SHADOW_LIGHTS) {
                i = nextIndex++;
                indexMap[i] = m.getLight();
            }
            if (i >= 0) {
                material.setTexture("ShadowMap", m.getMap());
                material.setMatrix4("LightViewProjectionMatrix", m.getProjection());
                material.setInt("LightType", m.getLight().getType().getId());
                material.setInt("LightIndex", i);
                material.setVector2("LightRangeInverse", m.getInverseRange(tempInvRange));
                context.renderFullscreen(material);
                renderState.setBlendMode(RenderState.BlendMode.Additive);
            }
        }
        renderState.setBlendMode(RenderState.BlendMode.Off);
        
        resources.setPrimitive(lightShadowIndices, indexMap);
        
    }
    @Override
    protected void reset(FGRenderContext context) {
        indexMap = null;
    }
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
    private static int indexOf(Object[] array, Object obj) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == obj) {
                return i;
            }
        }
        return -1;
    }
    
}
