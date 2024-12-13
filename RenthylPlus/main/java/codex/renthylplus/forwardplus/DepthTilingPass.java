/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.forwardplus;

import codex.jmecompute.ArgType;
import codex.jmecompute.UniversalShaderLoader;
import codex.jmecompute.WorkSize;
import codex.jmecompute.opengl.GLComputeShader;
import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.ResourceTicket;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

/**
 *
 * @author codex
 */
public class DepthTilingPass extends RenderPass {

    private ResourceTicket<Texture2D> sceneDepth;
    private ResourceTicket<Texture2D> depthRangeTiles;
    private final TextureDef<Texture2D> tileDef = TextureDef.texture2D();
    private GLComputeShader shader;
    private int tileSize = 16;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        sceneDepth = addInput("SceneDepth");
        depthRangeTiles = addOutput("DepthRangeTiles");
        tileDef.setMagFilter(Texture.MagFilter.Nearest);
        tileDef.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        tileDef.setFormat(Image.Format.RG32F);
        shader = UniversalShaderLoader.loadOpenGLCompute(frameGraph.getAssetManager(),
                "RenthylPlus/Shaders/DepthTileCompute.glsl");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(tileDef, depthRangeTiles);
        reserve(depthRangeTiles);
        reference(sceneDepth);
    }
    @Override
    protected void execute(FGRenderContext context) {
        Texture2D inTex = resources.acquire(sceneDepth);
        int w = inTex.getImage().getWidth() / tileSize;
        int h = inTex.getImage().getHeight() / tileSize;
        tileDef.setSize(w, h);
        shader.set("DepthTexture", ArgType.Texture, inTex);
        shader.set("TileTexture", ArgType.Texture, resources.acquire(depthRangeTiles));
        shader.execute(new WorkSize(w, h, 1).offloadToLocal(tileSize));
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
    public void setTileSize(int size) {
        assert size > 0;
        this.tileSize = size;
    }
    
}
