/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.vxgi;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.client.GraphSource;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.ResourceTicket;
import com.jme3.bounding.BoundingBox;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

/**
 *
 * @author codex
 */
public class VoxelEnvSetupPass extends RenderPass {
    
    public static final int DEFAULT_VOXEL_GRID_SIZE = 64;
    public static final BoundingBox DEFAULT_VOXEL_BOUNDS = new BoundingBox(Vector3f.ZERO, 20, 20, 20);
    
    private ResourceTicket<Camera> voxelCamera;
    private ResourceTicket<Integer> voxelGridSize;
    private ResourceTicket<BoundingBox> voxelBounds;
    private GraphSource<Integer> gridSize;
    private GraphSource<BoundingBox> bounds;
    private Camera camera;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        voxelCamera = addOutput("Camera");
        voxelGridSize = addOutput("GridSize");
        voxelBounds = addOutput("Bounds");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declarePrimitive(voxelCamera, voxelGridSize, voxelBounds);
    }
    @Override
    protected void execute(FGRenderContext context) {
        BoundingBox box = GraphSource.get(bounds, DEFAULT_VOXEL_BOUNDS, context);
        int size = GraphSource.get(gridSize, DEFAULT_VOXEL_GRID_SIZE, context);
        if (camera == null) {
            camera = context.getViewPort().getCamera().clone();
            camera.resize(size, size, true);
            camera.setAxes(Vector3f.UNIT_X, Vector3f.UNIT_Y, Vector3f.UNIT_Z);
            camera.setParallelProjection(true);
            camera.setFrustumNear(1f);
        } else if (size != camera.getWidth() || size != camera.getHeight()) {
            camera.resize(size, size, false);
        }
        camera.setLocation(box.getCenter(new Vector3f()).subtractLocal(0f, 0f, box.getZExtent() + camera.getFrustumNear()));
        resources.setPrimitive(voxelCamera, camera);
        resources.setPrimitive(voxelGridSize, size);
        resources.setPrimitive(voxelBounds, box);
        //context.resizeCamera(768, 768, false, false, false);
        context.resizeCamera(size, size, false, false, true);
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
}
