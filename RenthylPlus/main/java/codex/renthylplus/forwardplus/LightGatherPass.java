/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.forwardplus;

import codex.jmecompute.ArgType;
import codex.jmecompute.WorkSize;
import codex.jmecompute.opengl.GLComputeShader;
import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.definitions.BufferDef;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.ResourceTicket;
import com.jme3.light.LightList;
import com.jme3.math.FastMath;
import com.jme3.texture.Texture2D;
import java.nio.IntBuffer;

/**
 *
 * @author codex
 */
public class LightGatherPass extends RenderPass {
    
    private ResourceTicket<LightList> lights;
    private ResourceTicket<Texture2D> depthRangeTiles;
    private ResourceTicket<IntBuffer> overlap;
    private final BufferDef<IntBuffer> overlapDef = BufferDef.ints();
    private GLComputeShader shader;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        lights = addInput("Lights");
        depthRangeTiles = addInput("DepthRangeTiles");
        overlap = addOutput("Overlap");
        shader = GLComputeShader.load(frameGraph.getAssetManager(), "RenthylPlus/Shaders/ForwardPlus/LightTileGather.glsl");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(overlapDef, overlap);
        reserve(overlap);
        reference(lights, depthRangeTiles);
    }
    @Override
    protected void execute(FGRenderContext context) {
        
        LightList lightList = resources.acquire(lights);
        Texture2D tiles = resources.acquire(depthRangeTiles);
        int w = tiles.getImage().getWidth();
        int h = tiles.getImage().getHeight();
        
        // Number of overlap integers is equal to the number of tiles
        // times the number of lights divided by 32 (integer size) rounded up.
        // Each bit corresponds to a light, with extra bits always set to zero.
        overlapDef.setSize(w * h * (int)FastMath.ceil((float)lightList.size() / Integer.SIZE));
        overlapDef.setInitToZero(true);
        
        shader.set("tileImage", ArgType.Texture, tiles);
        shader.set("numOverlapUnits", ArgType.Int, overlapDef.getSize());
        shader.set("overlap", ArgType.IntBuffer, resources.acquire(overlap));
        shader.execute(new WorkSize(w, h, 1).offloadToLocal(2));
        
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
}
