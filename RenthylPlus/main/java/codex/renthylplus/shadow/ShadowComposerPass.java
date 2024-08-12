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
import com.jme3.texture.Texture2D;

/**
 *
 * @author gary
 */
public class ShadowComposerPass extends RenderPass {

    private ResourceTicket<GeometryQueue> receivers;
    private ResourceTicket<Texture2D> result;
    private TextureDef<Texture2D> texDef = TextureDef.texture2D();
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        addInputList("ShadowMaps");
        result = addOutput("Result");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(texDef, result);
        reserve(result);
        referenceOptional(getGroupArray("ShadowMaps"));
    }
    @Override
    protected void execute(FGRenderContext context) {
        texDef.setSize(context.getWidth(), context.getHeight());
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
}
