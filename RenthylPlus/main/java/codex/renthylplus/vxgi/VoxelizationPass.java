/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.vxgi;

import codex.jmecompute.ArgType;
import codex.jmecompute.ImageBindState;
import codex.jmecompute.UniversalShaderLoader;
import codex.jmecompute.WorkSize;
import codex.jmecompute.opengl.GLComputeShader;
import codex.jmecompute.opengl.Glsl;
import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.GeometryQueue;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.ResourceTicket;
import codex.renthyl.util.GeometryRenderHandler;
import com.jme3.bounding.BoundingBox;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.shader.VarType;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture3D;
import java.util.HashSet;
import org.lwjgl.opengl.GL45;

/**
 *
 * @author codex
 */
public class VoxelizationPass extends RenderPass implements GeometryRenderHandler {
    
    private ResourceTicket<GeometryQueue> geometry;
    private ResourceTicket<float[]> lights;
    private ResourceTicket<ColorRGBA> ambient;
    private ResourceTicket<Integer> gridSize;
    private ResourceTicket<BoundingBox> voxelBounds;
    private ResourceTicket<Texture3D> lightContribution;
    private ResourceTicket<Texture3D> temporalVoxels;
    private ResourceTicket<Texture3D> voxels;
    private ResourceTicket<Texture2D> renderTarget;
    private final TextureDef<Texture3D> voxelDef = TextureDef.texture3D(Image.Format.RGBA32F);
    private final TextureDef<Texture2D> renderTargetDef = TextureDef.texture2D();
    private final Vector3f boundMin = new Vector3f();
    private final Vector3f boundMax = new Vector3f();
    private Material material;
    private GLComputeShader mipmapper;
    private HashSet<Integer> mipsGenerated = new HashSet<>();
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        geometry = addInput("Geometry");
        lights = addInput("Lights");
        ambient = addInput("Ambient");
        gridSize = addInput("GridSize");
        voxelBounds = addInput("Bounds");
        lightContribution = addInput("LightContribution");
        temporalVoxels = addInput("TemporalVoxels");
        voxels = addOutput("Voxels");
        renderTarget = addOutput("ScreenSpaceResult");
        mipmapper = UniversalShaderLoader.loadOpenGLCompute(frameGraph.getAssetManager(),
                "RenthylPlus/MatDefs/VXGI/voxelMipmap.glsl", Glsl.V450);
        material = new Material(frameGraph.getAssetManager(), "RenthylPlus/MatDefs/VXGI/voxelize.j3md");
        RenderState rs = material.getAdditionalRenderState();
        rs.setDepthTest(false);
        rs.setDepthWrite(false);
        rs.setFaceCullMode(RenderState.FaceCullMode.Off);
        voxelDef.setMagFilter(Texture.MagFilter.Bilinear);
        voxelDef.setMinFilter(Texture.MinFilter.Trilinear); // trilinear minification generates mipmaps
        voxelDef.setWrap(Texture.WrapMode.EdgeClamp);
        voxelDef.setAccess(Image.Access.ReadWrite);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(voxelDef, voxels);
        declare(renderTargetDef, renderTarget);
        reserve(voxels, renderTarget);
        reference(geometry, lights, voxelBounds);
        referenceOptional(gridSize, ambient, lightContribution, temporalVoxels);
    }
    @Override
    protected void execute(FGRenderContext context) {
        
        // set voxel grid size from source
        int n = resources.acquireOrElse(gridSize, VoxelEnvSetupPass.DEFAULT_VOXEL_GRID_SIZE);
        voxelDef.setCube(n);
        renderTargetDef.setSquare(n);
        Texture3D voxelMap = resources.acquire(voxels);
        voxelMap.getImage().setAccess(Image.Access.ReadWrite);
        
        // get voxel grid bounds
        BoundingBox bounds = resources.acquire(voxelBounds);
        bounds.getMin(boundMin);
        bounds.getMax(boundMax);
        
        // setup camera
        context.resizeCamera(n, n, false, false, false);
        
        // acquire framebuffer for rasterizing geometry into the voxel grid
        FrameBuffer fb = getFrameBuffer(n, n, 1);
        resources.acquireColorTarget(fb, renderTarget);
        context.getRenderer().setFrameBuffer(fb);
        context.getRenderer().clearBuffers(true, true, true);
        
        // setup material
        float[] lightArray = resources.acquire(lights);
        material.setParam("LightData", VarType.FloatArray, lightArray);
        material.setInt("LightDataSize", lightArray.length);
        material.setTexture("LightContributionMap", resources.acquireOrElse(lightContribution, null));
        material.setColor("AmbientLight", resources.acquire(ambient));
        material.setTexture("VoxelMap", voxelMap);
        material.setVector3("GridMin", boundMin);
        material.setVector3("GridMax", boundMax);
        material.setInt("GridSize", n);
        material.setTexture("TemporalVoxelMap", resources.acquireOrElse(temporalVoxels, null));
        context.getRenderManager().setForcedMaterial(material);
        
        // rasterize geometries into the voxel grid
        context.renderGeometry(resources.acquire(geometry), null, this);
        
        int voxId = voxelMap.getImage().getId();
        if (!mipsGenerated.contains(voxId)) {
            GL45.glGenerateTextureMipmap(voxId);
            mipsGenerated.add(voxId);
        }
        
        // mipmap the voxel grid
        voxelMap.getImage().setAccess(null);
        mipmapper.set("UpperLevel", ArgType.Image, voxelMap);
        mipmapper.set("LowerLevel", ArgType.Image, voxelMap);
        WorkSize work = new WorkSize();
        ImageBindState bind = new ImageBindState(0, -1, ImageBindState.Access.ReadWrite);
        for (int i = 0; n >= 2; i++) {
            bind.setAccess(ImageBindState.Access.ReadOnly);
            mipmapper.getUniform("UpperLevel").setImageBind(bind);
            bind.setLevel(i + 1);
            bind.setAccess(ImageBindState.Access.WriteOnly);
            mipmapper.getUniform("LowerLevel").setImageBind(bind);
            mipmapper.execute(work.set((n = n >> 1), 1));
        }
        
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
        //material.getAdditionalRenderState().set(g.getMaterial().getAdditionalRenderState());
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
