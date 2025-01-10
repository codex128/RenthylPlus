/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.tickets.ResourceTicket;
import com.jme3.math.Matrix4f;
import com.jme3.texture.TextureArray;

/**
 *
 * @author codex
 */
public class ShadowPackerPass extends RenderPass {
    
    private ResourceTicket<TextureArray> shadowMapArray;
    private ResourceTicket<Matrix4f[]> lightViewMatrices;
    private ResourceTicket<float[]> inverseLightRanges;
    private ResourceTicket<int[]> shadowMapIndices;
    
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
