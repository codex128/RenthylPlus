/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.vxgi;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.client.GraphSource;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.tickets.ResourceTicket;
import com.jme3.bounding.BoundingBox;
import com.jme3.math.Vector3f;

/**
 *
 * @author codex
 */
public class VoxelEnvSetupPass extends RenderPass {
    
    public static final int DEFAULT_VOXEL_GRID_SIZE = 64;
    public static final BoundingBox DEFAULT_VOXEL_BOUNDS = new BoundingBox(Vector3f.ZERO, 20, 20, 20);
    
    private ResourceTicket<Integer> voxelGridSize;
    private ResourceTicket<BoundingBox> voxelBounds;
    private GraphSource<Integer> gridSize;
    private GraphSource<BoundingBox> bounds;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        voxelGridSize = addOutput("GridSize");
        voxelBounds = addOutput("Bounds");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declarePrimitive(voxelGridSize, voxelBounds);
    }
    @Override
    protected void execute(FGRenderContext context) {
        BoundingBox box = GraphSource.get(bounds, DEFAULT_VOXEL_BOUNDS, context);
        int size = GraphSource.get(gridSize, DEFAULT_VOXEL_GRID_SIZE, context);
        if (size <= 0) {
            throw new IllegalArgumentException("Voxel grid size cannot be less than zero.");
        }
        resources.setPrimitive(voxelGridSize, size);
        resources.setPrimitive(voxelBounds, box);
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}

    public void setGridSize(GraphSource<Integer> gridSize) {
        this.gridSize = gridSize;
    }
    public void setBounds(GraphSource<BoundingBox> bounds) {
        this.bounds = bounds;
    }
    
}
