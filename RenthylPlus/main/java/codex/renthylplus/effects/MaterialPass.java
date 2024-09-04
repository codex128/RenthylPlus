/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.effects;

import codex.boost.export.SavableObject;
import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.ResourceTicket;
import com.jme3.export.InputCapsule;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import java.io.IOException;

/**
 *
 * @author codex
 */
public class MaterialPass extends RenderPass {
    
    private ResourceTicket<Texture2D> in, out;
    private TextureDef<Texture2D> texDef = TextureDef.texture2D();
    private Material material;
    private int upsample = 0;
    private int width, height;
    private final Vector2f invSize = new Vector2f();
    private String textureParam = "Texture";
    private String texelSizeParam = "TexelSize";
    private String numSamplesParam = "NumSamples";
    
    public MaterialPass() {}
    public MaterialPass(Material material) {
        this(material, 0);
    }
    public MaterialPass(int upsample) {
        this(null, 0);
    }
    public MaterialPass(Material material, int upsample) {
        this.material = material;
        this.upsample = upsample;
    }
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        in = addInput("Input");
        out = addOutput("Output");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(texDef, out);
        reserve(out);
        reference(in);
    }
    @Override
    protected void execute(FGRenderContext context) {
        Texture2D inTex = resources.acquire(in);
        int w = shift(inTex.getImage().getWidth());
        int h = shift(inTex.getImage().getHeight());
        if (w == 0 || h == 0) {
            throw new IllegalStateException("Output texture width or height cannot be zero.");
        }
        if (w != width || h != height) {
            invSize.set(1, 1).divideLocal(w, h);
            width = w;
            height = h;
        }
        texDef.setSize(w, h);
        texDef.setFormat(inTex.getImage().getFormat());
        FrameBuffer fb = getFrameBuffer(w, h, 1);
        resources.acquireColorTarget(fb, out);
        context.getRenderer().setFrameBuffer(fb);
        context.getRenderer().clearBuffers(true, true, true);
        context.resizeCamera(w, h, false, false, false);
        if (material != null) {
            material.setTexture(textureParam, inTex);
            if (texelSizeParam != null) {
                material.setVector2(texelSizeParam, invSize);
            }
            if (numSamplesParam != null) {
                material.setInt(numSamplesParam, 1);
            }
            context.renderFullscreen(material);
        } else {
            context.renderTextures(inTex, null);
        }
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    protected void write(OutputCapsule out) throws IOException {
        out.write(TextureDef.saveTexture2D(texDef), "textureDef", null);
        out.write(material, "material", null);
        out.write(upsample, "upsample", 0);
        out.write(textureParam, "textureParam", "Texture");
        out.write(texelSizeParam, "texelSizeParam", "TexelSize");
        out.write(numSamplesParam, "numSamplesParam", "NumSamples");
    }
    @Override
    protected void read(InputCapsule in) throws IOException {
        texDef = SavableObject.readSavable(in, "textureDef", TextureDef.Texture2DCapsule.class, null).getTextureDef();
        material = SavableObject.readSavable(in, "material", Material.class, null);
        upsample = in.readInt("upsample", 0);
        textureParam = in.readString("textureParam", "Texture");
        texelSizeParam = in.readString("texelSizeParam", "TexelSize");
        numSamplesParam = in.readString("numSamplesParam", "NumSamples");
    }
    
    private int shift(int n) {
        if (upsample == 0) {
            return n;
        } else if (upsample > 0) {
            return n << upsample;
        } else {
            return n >> -upsample;
        }
    }
    
    public void setMaterial(Material material) {
        this.material = material;
    }
    public void setUpsample(int upsample) {
        this.upsample = upsample;
    }
    public void setTextureParam(String textureParam) {
        this.textureParam = textureParam;
    }
    public void setTexelSizeParam(String texelSizeParam) {
        this.texelSizeParam = texelSizeParam;
    }
    public void setNumSamplesParam(String numSamplesParam) {
        this.numSamplesParam = numSamplesParam;
    }
    
    public TextureDef<Texture2D> getTextureDef() {
        return texDef;
    }
    public Material getMaterial() {
        return material;
    }
    public int getUpsample() {
        return upsample;
    }
    public String getTextureParam() {
        return textureParam;
    }
    public String getTexelSizeParam() {
        return texelSizeParam;
    }
    public String getNumSamplesParam() {
        return numSamplesParam;
    }
    
}
