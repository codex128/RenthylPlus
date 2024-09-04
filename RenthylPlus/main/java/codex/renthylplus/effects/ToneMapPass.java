/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.effects;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.ResourceTicket;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

/**
 *
 * @author codex
 */
public class ToneMapPass extends RenderPass {
    
    private ResourceTicket<Texture2D> color, toneMap, result;
    private final TextureDef<Texture2D> texDef = TextureDef.texture2D();
    private Material material;
    private int width, height;
    private final Vector2f texelSize = new Vector2f();
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        color = addInput("Color");
        toneMap = addInput("ToneMap");
        result = addOutput("Result");
        material = new Material(frameGraph.getAssetManager(), "RenthylPlus/MatDefs/Effects/ToneMap.j3md");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(texDef, result);
        reserve(result);
        reference(color, toneMap);
    }
    @Override
    protected void execute(FGRenderContext context) {
        Texture2D colorTex = resources.acquire(color);
        Texture2D toneMapTex = resources.acquire(toneMap);
        Image img = colorTex.getImage();
        int w = img.getWidth();
        int h = img.getHeight();
        texDef.setSize(w, h);
        texDef.setFormat(img.getFormat());
        FrameBuffer fb = getFrameBuffer(w, h, 1);
        resources.acquireColorTarget(fb, result);
        context.getRenderer().setFrameBuffer(fb);
        context.getRenderer().clearBuffers(true, true, true);
        context.resizeCamera(w, h, false, false, false);
        material.setTexture("ColorMap", colorTex);
        material.setTexture("ToneMap", toneMapTex);
        material.setVector2("ToneTexelSize", calculateTexelSize(toneMapTex));
        context.renderFullscreen(material);
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
    private Vector2f calculateTexelSize(Texture2D toneMapTex) {
        int w = toneMapTex.getImage().getWidth();
        int h = toneMapTex.getImage().getHeight();
        if (w != width || h != height) {
            texelSize.set(1, 1).divideLocal(w, h);
            width = w;
            height = h;
        }
        return texelSize;
    }
    
}
