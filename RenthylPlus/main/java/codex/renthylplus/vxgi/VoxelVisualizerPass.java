/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.vxgi;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.GeometryQueue;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.draw.RenderMode;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.tickets.ResourceTicket;
import codex.renthyl.util.GeometryRenderHandler;
import com.jme3.bounding.BoundingBox;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Vector3f;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture3D;

/**
 *
 * @author codex
 */
public class VoxelVisualizerPass extends RenderPass {

    private ResourceTicket<Texture3D> voxels;
    private ResourceTicket<GeometryQueue> geometry;
    private ResourceTicket<BoundingBox> bounds;
    private ResourceTicket<Texture2D> color;
    private ResourceTicket<Texture2D> depth;
    private final TextureDef<Texture2D> colorDef = TextureDef.texture2D();
    private final TextureDef<Texture2D> depthDef = TextureDef.texture2D(Image.Format.Depth);
    private Material material;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        voxels = addInput("Voxels");
        geometry = addInput("Geometry");
        bounds = addInput("Bounds");
        color = addOutput("Color");
        depth = addOutput("Depth");
        material = new Material(frameGraph.getAssetManager(), "RenthylPlus/MatDefs/VXGI/voxelDebug.j3md");
        material.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(colorDef, color);
        declare(depthDef, depth);
        reserve(color, depth);
        reference(voxels, geometry, bounds);
    }
    @Override
    protected void execute(FGRenderContext context) {
        
        colorDef.setSize(context.getWidth(), context.getHeight());
        depthDef.setSize(colorDef);
        FrameBuffer fb = getFrameBuffer(context, 1);
        resources.acquireColorTarget(fb, color);
        resources.acquireDepthTarget(fb, depth);
        context.registerMode(RenderMode.frameBuffer(fb));
        context.clearBuffers();
        
        BoundingBox box = resources.acquire(bounds);
        Vector3f min = box.getMin(new Vector3f());
        Vector3f max = box.getMax(new Vector3f());
        material.setTexture("VoxelMap", resources.acquire(voxels));
        material.setVector3("GridMin", min);
        material.setVector3("GridMax", max);
        context.registerMode(RenderMode.forcedMaterial(material));
        
        GeometryQueue queue = resources.acquire(geometry);
        queue.render(context, GeometryRenderHandler.DEFAULT);
        
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
}
