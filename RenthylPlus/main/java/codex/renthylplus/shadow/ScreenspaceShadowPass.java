/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.modules.RenderPass;
import codex.boost.material.ImmediateMatDef;
import codex.renthyl.resources.tickets.DynamicTicketList;
import codex.renthyl.resources.tickets.ResourceTicket;
import com.jme3.material.Material;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

/**
 * Applies shadow maps directly to the screenspace.
 * 
 * @author codex
 */
public class ScreenspaceShadowPass extends RenderPass {
    
    private static ImmediateMatDef matdef;
    
    private ResourceTicket<Texture2D> color, depth;
    private ResourceTicket<Texture2D> result;
    private DynamicTicketList<ShadowMap> shadowMaps;
    private final TextureDef<Texture2D> resultDef = TextureDef.texture2D();
    private Material material;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        color = addInput("RecieverColor");
        depth = addInput("RecieverDepth");
        shadowMaps = addInputGroup(new DynamicTicketList<>("ShadowMaps"));
        result = addOutput("Result");
        resultDef.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        resultDef.setMagFilter(Texture.MagFilter.Bilinear);
        if (matdef == null) {
            matdef = new ImmediateMatDef(frameGraph.getAssetManager(), "ScreenspaceShadows")
                    .addParam(VarType.Texture2D, "SceneDepth")
                    .addParam(VarType.Texture2D, "ShadowMap")
                    .addParam(VarType.Matrix4, "ViewProjectionMatrixInverse")
                    .addParam(VarType.Matrix4, "LightViewProjectionMatrix")
                    .addParam(VarType.Float, "ShadowIntensity", 1f);
            matdef.createTechnique()
                    .setVersions(450, 310, 150)
                    .setVertexShader("RenthylPlus/MatDefs/Fullscreen/Screen.vert")
                    .setFragmentShader("RenthylPlus/MatDefs/Shadow/ScreenspaceShadow.frag")
                    .add();
        }
        material = matdef.createMaterial();
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(resultDef, result);
        reference(color, depth);
        referenceOptional(shadowMaps);
    }
    @Override
    protected void execute(FGRenderContext context) {}
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
}
