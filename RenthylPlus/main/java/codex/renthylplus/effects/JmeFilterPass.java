/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.effects;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.draw.RenderMode;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.modules.protocol.FilterProtocol;
import codex.renthyl.resources.tickets.ResourceTicket;
import codex.renthyl.resources.tickets.TicketSelector;
import codex.renthyl.resources.tickets.TicketSignature;
import com.jme3.export.InputCapsule;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author codex
 */
public abstract class JmeFilterPass extends RenderPass implements FilterProtocol {

    protected ResourceTicket<Texture2D> sceneColor, sceneDepth;
    protected ResourceTicket<Texture2D> result;
    private final ArrayList<Subpass> subpasses = new ArrayList<>();
    private boolean enabled = true;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        sceneColor = addInput("Color");
        sceneDepth = addInput("Depth");
        result = addOutput("Result");
        init(frameGraph);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        if (enabled) {
            result.setSource(null);
            boolean requireColor = false;
            boolean requireDepth = false;
            for (int i = 0; i < subpasses.size(); i++) {
                Subpass p = subpasses.get(i);
                if (i < subpasses.size()-1) {
                    declareTemporary(p.def, p.ticket);
                    reserve(p.ticket);
                }
                if (p.useColor) {
                    requireColor = true;
                }
                if (p.useDepth) {
                    requireDepth = true;
                }
            }
            declare(subpasses.get(subpasses.size()-1).def, result);
            reserve(result);
            if (requireColor) {
                reference(sceneColor);
            }
            if (requireDepth) {
                reference(sceneDepth);
            }
        } else if (result.getSource() != sceneColor) {
            // this is the API safe method, although it is slower than directly assigning the result's source
            getMainOutputGroup().makeInput(getMainInputGroup(), TicketSelector.is(sceneColor), TicketSelector.is(result));
        }
    }
    @Override
    protected void execute(FGRenderContext context) {
        
        if (!enabled) {
            return;
        }
        
        // acquire input textures
        Texture2D inColor = resources.acquireOrElse(sceneColor, null);
        Texture2D inDepth = resources.acquireOrElse(sceneDepth, null);
        int w = context.getWidth();
        int h = context.getHeight();
        Image.Format outFormat = null;
        if (inColor != null) {
            w = inColor.getImage().getWidth();
            h = inColor.getImage().getHeight();
            outFormat = inColor.getImage().getFormat();
        } else if (inDepth != null) {
            w = inDepth.getImage().getWidth();
            h = inDepth.getImage().getHeight();
        }
        
        // render each subpass in order
        for (int i = 0; i < subpasses.size(); i++) {
            
            // configure default size and format
            Subpass pass = subpasses.get(i);
            pass.def.setSize(w, h);
            if (outFormat != null && i == subpasses.size()-1) {
                pass.def.setFormat(outFormat);
            }
            pass.beforeAcquire(context);
            
            // resize camera to match framebuffer
            context.registerMode(RenderMode.cameraSize(pass.def.getWidth(), pass.def.getHeight()));
            
            // setup framebuffer
            FrameBuffer fb = getFrameBuffer(i, pass.def.getWidth(), pass.def.getHeight(), pass.def.getSamples());
            pass.targetTexture = resources.acquireColorTarget(fb, (i < subpasses.size()-1 ? pass.ticket : result));
            context.registerMode(RenderMode.frameBuffer(fb));
            context.clearBuffers(true, false, false);
            
            // set color parameters
            if (pass.useColor) {
                if (inColor == null) {
                    throw new NullPointerException("Scene color texture not defined.");
                }
                int colorSamples = inColor.getImage().getMultiSamples();
                if (colorSamples > 1) {
                    pass.material.setInt("NumSamples", colorSamples);
                } else {
                    pass.material.clearParam("NumSamples");
                }
                pass.material.setTexture("Texture", inColor);
            }
            
            // set depth parameters
            if (pass.useDepth) {
                if (inDepth == null) {
                    throw new NullPointerException("Scene depth texture not defined.");
                }
                int depthSamples = inDepth.getImage().getMultiSamples();
                if (depthSamples > 1) {
                    pass.material.setInt("NumSamplesDepth", depthSamples);
                } else {
                    pass.material.clearParam("NumSamplesDepth");
                }
                pass.material.setTexture("DepthTexture", inDepth);
            }
            
            // render
            pass.beforeRender(context);
            context.renderFullscreen(pass.material);
            pass.afterRender(context);
            
        }
        
        // release remaining temporary resources
        for (int i = 0; i < subpasses.size()-1; i++) {
            subpasses.get(i).releaseTargetTexture();
        }
        
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {
        subpasses.clear();
    }
    @Override
    protected void write(OutputCapsule out) throws IOException {
        super.write(out);
        out.write(enabled, "enabled", true);
    }
    @Override
    protected void read(InputCapsule in) throws IOException {
        super.read(in);
        setEnabled(in.readBoolean("enabled", true));
    }
    @Override
    public TicketSignature getRenderedSceneColor() {
        return new TicketSignature(true, TicketSelector.name("Color"));
    }
    @Override
    public TicketSignature getRenderedSceneDepth() {
        return new TicketSignature(true, TicketSelector.name("Depth"));
    }
    @Override
    public TicketSignature getFilteredResult() {
        return new TicketSignature(false, TicketSelector.name("Result"));
    }
    
    protected abstract void init(FrameGraph frameGraph);
    
    protected <T extends Subpass> T add(T pass) {
        subpasses.add(pass);
        return pass;
    }
    protected void clearSubpasses() {
        subpasses.clear();
    }
    
    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (frameGraph != null) {
                frameGraph.setLayoutUpdateNeeded();
            }
        }
    }
    public boolean isEnabled() {
        return enabled;
    }
    
    public class Subpass {
        
        private final Material material;
        private final ResourceTicket<Texture2D> ticket;
        private final TextureDef<Texture2D> def = TextureDef.texture2D();
        private final boolean useColor, useDepth;
        private Texture2D targetTexture;
        
        public Subpass(String name, Material material) {
            this(name, material, true, false);
        }
        public Subpass(String name, Material material, boolean useColor, boolean useDepth) {
            this.ticket = new ResourceTicket<>(name);
            this.material = material;
            this.useColor = useColor;
            this.useDepth = useDepth;
        }
        public Subpass(Material material) {
            this(null, material, true, false);
        }
        public Subpass(Material material, boolean useColor, boolean useDepth) {
            this(null, material, useColor, useDepth);
        }
        
        public void releaseTargetTexture() {
            if (targetTexture != null) {
                resources.release(ticket);
                targetTexture = null;
            }
        }
        
        public void beforeAcquire(FGRenderContext context) {}
        public void beforeRender(FGRenderContext context) {}
        public void afterRender(FGRenderContext context) {}
        
        public Material getMaterial() {
            return material;
        }
        public TextureDef<Texture2D> getDef() {
            return def;
        }
        public boolean isUseColor() {
            return useColor;
        }
        public boolean isUseDepth() {
            return useDepth;
        }
        public Texture2D getRenderedTexture() {
            return targetTexture;
        }
        
    }
    
}
