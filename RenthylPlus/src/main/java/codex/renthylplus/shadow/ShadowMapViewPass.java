/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.tickets.ResourceTicket;

/**
 *
 * @author codex
 */
public class ShadowMapViewPass extends RenderPass {

    private ResourceTicket<ShadowMap> shadowMap;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        shadowMap = addInput("ShadowMap");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        referenceOptional(shadowMap);
    }
    @Override
    protected void execute(FGRenderContext context) {
        ShadowMap map = resources.acquireOrElse(shadowMap, null);
        if (map != null) {
            context.getScreen().setVisualizeAsDepth(true);
            context.renderTextures(resources.acquire(shadowMap).getMap(), null);
        }
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public boolean isUsed() {
        return true;
    }
    
}
