/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.vxgi;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.GeometryQueue;
import codex.renthyl.client.GraphSource;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.ResourceTicket;
import codex.renthyl.util.Defines;
import codex.renthyl.util.GeometryRenderHandler;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.light.LightList;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.TechniqueDef;
import com.jme3.material.logic.TechniqueDefLogic;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.scene.Geometry;
import com.jme3.shader.DefineList;
import com.jme3.shader.Shader;
import com.jme3.shader.VarType;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture3D;
import java.util.EnumSet;

/**
 *
 * @author codex
 */
public class VoxelizationPass extends RenderPass implements GeometryRenderHandler {
    
    private ResourceTicket<GeometryQueue> geometry;
    private ResourceTicket<float[]> lights;
    private ResourceTicket<ColorRGBA> ambient;
    private ResourceTicket<Integer> gridSize;
    private ResourceTicket<Camera> camera;
    private ResourceTicket<BoundingBox> voxelBounds;
    private ResourceTicket<Texture2D> lightContribution;
    private ResourceTicket<Texture3D> voxels;
    private ResourceTicket<Texture2D> renderTarget;
    private final TextureDef<Texture3D> voxelDef = TextureDef.texture3D(Image.Format.RGBA32F);
    private final TextureDef<Texture2D> renderTargetDef = TextureDef.texture2D();
    private Material material;
    private final Vector3f boundMin = new Vector3f();
    private final Vector3f boundMax = new Vector3f();
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        geometry = addInput("Geometry");
        lights = addInput("Lights");
        ambient = addInput("Ambient");
        gridSize = addInput("GridSize");
        camera = addInput("Camera");
        voxelBounds = addInput("Bounds");
        lightContribution = addInput("LightContribution");
        voxels = addOutput("Voxels");
        renderTarget = addOutput("ScreenSpaceResult");
        material = new Material(frameGraph.getAssetManager(), "RenthylPlus/MatDefs/VXGI/voxelize.j3md");
        RenderState rs = material.getAdditionalRenderState();
        rs.setDepthTest(false);
        rs.setDepthWrite(false);
        rs.setFaceCullMode(RenderState.FaceCullMode.Off);
        voxelDef.setMagFilter(Texture.MagFilter.Bilinear);
        voxelDef.setMinFilter(Texture.MinFilter.Trilinear);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(voxelDef, voxels);
        declare(renderTargetDef, renderTarget);
        reserve(voxels, renderTarget);
        reference(geometry, lights, camera, voxelBounds);
        referenceOptional(gridSize, ambient, lightContribution);
    }
    @Override
    protected void execute(FGRenderContext context) {
        
        // set voxel grid size from source
        int n = resources.acquireOrElse(gridSize, VoxelEnvSetupPass.DEFAULT_VOXEL_GRID_SIZE);
        voxelDef.setCube(n);
        renderTargetDef.setSquare(n);
        Texture3D voxelMap = resources.acquire(voxels);
        
        // get voxel grid bounds
        BoundingBox bounds = resources.acquire(voxelBounds);
        bounds.getMin(boundMin);
        bounds.getMax(boundMax);
        
        // setup camera
        //Camera cam = resources.acquire(camera);
        //context.getRenderManager().setCamera(cam, cam.isParallelProjection());
        System.out.println("voxelization");
        
        // acquire framebuffer for rasterizing geometry into the voxel grid
        FrameBuffer fb = getFrameBuffer(n, n, 1);
        resources.acquireColorTarget(fb, renderTarget);
        context.getRenderer().setFrameBuffer(fb);
        context.getRenderer().clearBuffers(true, true, true);
        
        // setup material
        float[] lightArray = resources.acquire(lights);
        material.setParam("LightData", VarType.FloatArray, lightArray);
        material.setInt("NumLights", lightArray.length / LightArrayPass.FLOATS_PER_LIGHT);
        material.setTexture("LightContributionMap", resources.acquireOrElse(lightContribution, null));
        material.setColor("AmbientLight", resources.acquire(ambient));
        material.setTexture("VoxelMap", voxelMap);
        material.setVector3("GridMin", boundMin);
        material.setVector3("GridMax", boundMax);
        material.setInt("GridSize", n);
        context.getRenderManager().setForcedMaterial(material);
        
        System.out.println(context.getRenderManager().getCurrentCamera().getWidth());
        
        // rasterize geometries into the voxel grid
        //resources.acquire(geometry).render(context.getRenderManager(), this);
        context.resizeCamera(n, n, false, false, false);
        //context.resizeCamera(768, 768, true, false, true);
        
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public boolean renderGeometry(RenderManager rm, Geometry g) {
        // adapt material to geometry's pbr material
        transferParam(g, VarType.Vector4, "BaseColor", ColorRGBA.White);
        transferParam(g, VarType.Texture2D, "BaseColorMap", null);
        transferParam(g, VarType.Float, "AlphaDiscardThreshold", null);
        if (transferParam(g, VarType.Vector4, "Emissive", null)
                || transferParam(g, VarType.Texture2D, "EmissiveMap", null)) {
            transferParam(g, VarType.Float, "EmissivePower", 2f);
            transferParam(g, VarType.Float, "EmissiveIntensity", 5f);
        }
        rm.renderGeometry(g);
        return true;
    }
    
    private boolean transferParam(Geometry g, VarType type, String paramName, Object defVal) {
        Object param = g.getMaterial().getParamValue(paramName);
        if (param != null) {
            material.setParam(paramName, type, param);
            return true;
        } else if (defVal != null) {
            material.setParam(paramName, type, defVal);
            return true;
        } else {
            material.clearParam(paramName);
            return false;
        }
    }
    
}
