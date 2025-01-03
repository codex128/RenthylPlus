/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.forwardplus;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.tickets.ResourceTicket;
import com.jme3.texture.Texture2D;

/**
 *
 * @author codex
 */
public class LightSortingPass extends RenderPass {

    private ResourceTicket<Texture2D> depth;
    private ResourceTicket<Texture2D> lightIndex;
    
    
    @Override
    protected void initialize(FrameGraph frameGraph) {}
    @Override
    protected void prepare(FGRenderContext context) {}
    @Override
    protected void execute(FGRenderContext context) {}
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
}
