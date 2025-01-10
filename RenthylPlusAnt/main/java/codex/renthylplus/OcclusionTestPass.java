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
import codex.renthyl.definitions.BufferDef;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.tickets.ResourceTicket;
import com.jme3.bounding.BoundingBox;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture2D;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Tests bounding boxes against a depth texture to determine
 * if geometries are visible.
 * 
 * @author codex
 */
public class OcclusionTestPass extends RenderPass {
    
    private static ImmediateMatDef matdef;
    
    private ResourceTicket<Texture2D> depth;
    private ResourceTicket<GeometryQueue> geometry;
    private ResourceTicket<GeometryQueue> visible;
    private final GeometryQueue visibleQueue = new GeometryQueue();
    private final ResourceTicket<IntBuffer> occlusionStates = new ResourceTicket<>("_occlusionStates");
    private final ResourceTicket<FloatBuffer> meshData = new ResourceTicket<>("_meshData");
    private final BufferDef<IntBuffer> occlusionStatesDef = BufferDef.ints();
    private final BufferDef<FloatBuffer> meshDataDef = BufferDef.floats();
    private final Geometry bbGeometry = new Geometry();
    private GraphSource<Float> boundPadding;
    
    public OcclusionTestPass() {
        occlusionStatesDef.setInitToZero(true);
        occlusionStatesDef.setPadding(10);
        meshDataDef.setPadding(10);
    }
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        depth = addInput("Depth");
        geometry = addInput("Geometry");
        visible = addOutput("Visible");
        Mesh mesh = new Mesh();
        mesh.setDynamic();
        mesh.setMode(Mesh.Mode.Points);
        bbGeometry.setMesh(mesh);
        if (matdef == null) {
            matdef = new ImmediateMatDef(frameGraph.getAssetManager(), "OcclusionTest")
                .addParam(VarType.IntArray, "Occlusion")
                .addParam(VarType.Float, "Padding", 0f);
            matdef.createTechnique()
                .setVersions(450, 410, 330)
                .setVertexShader("")
                .setGeometryShader("")
                .setFragmentShader("")
                .add();
        }
        Material mat = matdef.createMaterial();
        mat.getAdditionalRenderState().setDepthTest(false);
        mat.getAdditionalRenderState().setDepthWrite(false);
        mat.getAdditionalRenderState().setColorWrite(false);
        bbGeometry.setMaterial(mat);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declarePrimitive(visible);
        declareTemporary(occlusionStatesDef, occlusionStates);
        declareTemporary(meshDataDef, meshData);
        reference(depth, geometry);
    }
    @Override
    protected void execute(FGRenderContext context) {
        GeometryQueue queue = resources.acquire(geometry);
        int n = queue.getNumGeometries();
        occlusionStatesDef.setSize(n);
        meshDataDef.setSize(n);
        IntBuffer states = resources.acquire(occlusionStates);
        FloatBuffer data = resources.acquire(meshData);
        BoundingBox box = new BoundingBox();
        int id = 0;
        for (Geometry g : queue) {
            g.getWorldBound().clone(box);
            Vector3f p = box.getCenter();
            data.put(p.x).put(p.y).put(p.z)
                .put(box.getXExtent()).put(box.getYExtent()).put(box.getZExtent())
                .put(id++);
        }
        data.flip();
        bbGeometry.getMesh().setBuffer(VertexBuffer.Type.Position, 7, data);
        bbGeometry.getMaterial().setFloat("Padding", GraphSource.get(boundPadding, 0f, context));
        context.getRenderer().setFrameBuffer(getFrameBuffer(2, 2, 1));
        context.getRenderManager().renderGeometry(bbGeometry);
        states.position(0);
        for (Geometry g : queue) {
            if (states.get() > 0) {
                visibleQueue.add(g);
            }
        }
    }
    @Override
    protected void reset(FGRenderContext context) {
        visibleQueue.clear();
    }
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
    public void setBoundPadding(GraphSource<Float> boundPadding) {
        this.boundPadding = boundPadding;
    }
    public void setBufferPadding(int padding) {
        occlusionStatesDef.setPadding(padding);
        meshDataDef.setPadding(padding);
    }
    
}
