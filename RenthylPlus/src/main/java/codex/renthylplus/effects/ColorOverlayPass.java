/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.effects;

import codex.boost.material.ImmediateMatDef;
import codex.boost.material.ImmediateShader;
import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.client.GraphSource;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.draw.RenderMode;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.tickets.ResourceTicket;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector4f;
import com.jme3.shader.Shader;
import com.jme3.shader.VarType;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture2D;

/**
 *
 * @author codex
 */
public class ColorOverlayPass extends RenderPass {

    private static ImmediateMatDef matdef;
    
    private ResourceTicket<Texture2D> color, result;
    private final TextureDef<Texture2D> resultDef = TextureDef.texture2D();
    private GraphSource<ColorRGBA> colorSource;
    private Material material;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        color = addInput("Color");
        result = addOutput("Result");
        if (matdef == null) {
            ImmediateShader frag = new ImmediateShader(Shader.ShaderType.Fragment)
                .includeGlslCompat()
                .uniform("sampler2D", "m_Texture")
                .uniform("vec4", "m_Color")
                .varying("vec2", "texCoord")
                .main()
                    .assign("gl_FragColor", "mix(texture2D(m_Texture, texCoord), m_Color, m_Color.a)")
                .end();
            matdef = new ImmediateMatDef(frameGraph.getAssetManager(), "ColorOverlay")
                .addParam(VarType.Texture2D, "Texture")
                .addParam(VarType.Vector4, "Color", Vector4f.ZERO);
            matdef.createTechnique()
                .setVersions(450, 310, 150)
                .setVertexShader("RenthylCore/MatDefs/Fullscreen/Screen.vert")
                .setShader(frag)
                .add();
        }
        material = matdef.createMaterial();
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(resultDef, result);
        reserve(result);
        reference(color);
    }
    @Override
    protected void execute(FGRenderContext context) {
        Texture2D inTex = resources.acquire(color);
        int w = inTex.getImage().getWidth();
        int h = inTex.getImage().getHeight();
        resultDef.setSize(w, h);
        resultDef.setFormat(inTex.getImage().getFormat());
        context.registerMode(RenderMode.cameraSize(w, h));
        FrameBuffer fb = getFrameBuffer(w, h, 1);
        resources.acquireColorTarget(fb, result);
        context.getRenderer().setFrameBuffer(fb);
        context.registerMode(RenderMode.frameBuffer(fb));
        context.clearBuffers();
        material.setTexture("Texture", inTex);
        ColorRGBA overlay = GraphSource.get(colorSource, null, frameGraph, context.getViewPort());
        if (overlay == null) {
            overlay = ColorRGBA.BlackNoAlpha;
        }
        material.setColor("Color", overlay);
        context.renderFullscreen(material);
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
}
