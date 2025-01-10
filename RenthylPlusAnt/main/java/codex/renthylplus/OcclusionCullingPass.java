/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus;

import codex.boost.material.ImmediateMatDef;
import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.GeometryQueue;
import codex.renthyl.client.GraphSource;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.draw.RenderMode;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.tickets.ResourceTicket;
import codex.renthyl.util.GeometryRenderHandler;
import com.jme3.material.Material;
import com.jme3.renderer.queue.NullComparator;
import com.jme3.scene.Geometry;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

/**
 *
 * @author codex
 */
public class OcclusionCullingPass extends RenderPass {
    
    public static final String VISIBLE = OcclusionCullingPass.class.getSimpleName() + "$visible";
    private static final NullComparator NULL_COMPARATOR = new NullComparator();
    private static final VisibilityHandler VISIBLE_HANDLER = new VisibilityHandler(true);
    private static final VisibilityHandler INVISIBLE_HANDLER = new VisibilityHandler(false);
    private static ImmediateMatDef matdef;

    private ResourceTicket<GeometryQueue> geometry;
    private ResourceTicket<GeometryQueue> visible, invisible;
    private final ResourceTicket<Texture2D> depth = new ResourceTicket<>("_depth");
    private final TextureDef<Texture2D> depthDef = TextureDef.texture2D(Image.Format.Depth);
    private final GeometryQueue visibleQueue = new GeometryQueue(NULL_COMPARATOR);
    private final GeometryQueue invisibleQueue = new GeometryQueue(NULL_COMPARATOR);
    private Material material;
    private GraphSource<Float> downsampling;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        geometry = addInput("Geometry");
        visible = addOutput("Visible");
        invisible = addOutput("Invisible");
        if (matdef == null) {
            matdef = new ImmediateMatDef(frameGraph.getAssetManager(), "OcclusionDepth");
            matdef.createTechnique()
                .setVersions(450, 310, 150)
                .setFragmentShader("")
                .setVertexShader("")
                .setGeometryShader("")
                .add();
        }
        material = matdef.createMaterial();
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declarePrimitive(visible, invisible);
        declareTemporary(depthDef, depth);
    }
    @Override
    protected void execute(FGRenderContext context) {
        float d = downsampling.getGraphValue(frameGraph, context.getViewPort());
        int w = (int)(context.getWidth() * d);
        int h = (int)(context.getHeight() * d);
        depthDef.setSize(w, h);
        context.registerMode(RenderMode.cameraSize(w, h));
        
        // render visible geometries
        FrameBuffer fb = getFrameBuffer(w, h, 1);
        resources.acquireDepthTarget(fb, depth);
        context.registerMode(RenderMode.frameBuffer(fb));
        context.clearBuffers();
        context.registerMode(RenderMode.forcedMaterial(material));
        GeometryQueue geoms = resources.acquire(geometry);
        geoms.render(context, VISIBLE_HANDLER);
        
        // render invisible geometries
        
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
    private static final class VisibilityHandler implements GeometryRenderHandler {
        
        private final boolean renderVisible;

        public VisibilityHandler(boolean renderVisible) {
            this.renderVisible = renderVisible;
        }
        
        @Override
        public void renderGeometry(FGRenderContext context, Geometry g) {
            Boolean visible = g.getUserData(VISIBLE);
            if (renderVisible == (visible == null || visible)) {
                context.getRenderManager().renderGeometry(g);
            }
        }
        
    }
    
}
