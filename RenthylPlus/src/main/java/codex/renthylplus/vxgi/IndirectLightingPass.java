/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.vxgi;

import codex.jmecompute.ArgType;
import codex.jmecompute.DivisionType;
import codex.jmecompute.UniversalShaderLoader;
import codex.jmecompute.WorkSize;
import codex.jmecompute.opengl.GLComputeShader;
import codex.jmecompute.opengl.Glsl;
import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.client.GraphSource;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.tickets.ResourceTicket;
import codex.renthyl.resources.tickets.TicketArray;
import codex.renthyl.resources.tickets.TicketSelector;
import com.jme3.bounding.BoundingBox;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture3D;
import com.jme3.texture.TextureImage;

/**
 *
 * @author codex
 */
public class IndirectLightingPass extends RenderPass {
    
    private static final float APERTURE_TAN = FastMath.tan(30f * FastMath.DEG_TO_RAD);
    private static final Vector2f SPEC_RANGE = new Vector2f(0.01f, 30f * FastMath.DEG_TO_RAD);
    private static final float[] tracePattern = {
        0, 0, 1, // normal-aligned
        1, 0, 1,
       -1, 0, 1,
        0, 1, 1,
        0,-1, 1,
    };
    
    private ResourceTicket<Texture2D> sceneColor, sceneDepth;
    private TicketArray<Texture2D> materials;
    private ResourceTicket<Texture3D> voxels;
    private ResourceTicket<BoundingBox> voxelBounds;
    private ResourceTicket<Integer> gridSize;
    private ResourceTicket<Texture2D> result;
    private final TextureDef<Texture2D> resultDef = TextureDef.texture2D(Image.Format.RGBA32F);
    private final Matrix4f camInverse = new Matrix4f();
    private final Vector3f gridMin = new Vector3f();
    private final Vector3f gridMax = new Vector3f();
    private final WorkSize work = new WorkSize();
    private TextureImage resultImg;
    private GLComputeShader shader;
    private GraphSource<Float> traceQuality;
    private GraphSource<Vector2f> specularAngleRange;
    private float time = 0;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        // TODO: group gbuffers into one group
        sceneColor = addInput("SceneColor");
        sceneDepth = addInput("SceneDepth");
        materials = addInputGroup(new TicketArray<>("Material", "Diffuse", "Position", "Normals", "Material"));
        voxels = addInput("Voxels");
        voxelBounds = addInput("Bounds");
        gridSize = addInput("GridSize");
        result = addOutput("Result");
        shader = UniversalShaderLoader.loadOpenGLCompute(
                frameGraph.getAssetManager(), "RenthylPlus/MatDefs/VXGI/pbrIndirect.glsl", Glsl.V450);
        shader.setDefine("NUM_TRACES", tracePattern.length / 3);
        shader.set("TraceDirections", ArgType.FloatArray, DivisionType.Vec3, tracePattern);
        shader.set("TraceTangent", ArgType.Float, APERTURE_TAN);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(resultDef, result);
        reserve(result);
        reference(sceneColor, sceneDepth, voxels, voxelBounds, gridSize);
        reference(materials);
    }
    @Override
    protected void execute(FGRenderContext context) {
        
        Texture2D inColor = resources.acquire(sceneColor);
        int w = inColor.getImage().getWidth();
        int h = inColor.getImage().getHeight();
        resultDef.setSize(w, h);
        
        Camera cam = context.getViewPort().getCamera();
        cam.getViewProjectionMatrix().invert(camInverse);
        BoundingBox box = resources.acquire(voxelBounds);
        box.getMin(gridMin);
        box.getMax(gridMax);
        
        if (resultImg == null) {
            resultImg = new TextureImage(resources.acquire(result), TextureImage.Access.WriteOnly);
        } else {
            resultImg.setTexture(resources.acquire(result));
        }
        
        shader.set("ColorMap", ArgType.Texture, resources.acquire(sceneColor));
        shader.set("DepthMap", ArgType.Texture, resources.acquire(sceneDepth));
        shader.set("DiffuseMap", ArgType.Texture, resources.acquire(materials.select(TicketSelector.name("Diffuse"))));
        shader.set("PositionMap", ArgType.Texture, resources.acquire(materials.select(TicketSelector.name("Position"))));
        shader.set("NormalMap", ArgType.Texture, resources.acquire(materials.select(TicketSelector.name("Normals"))));
        shader.set("MaterialMap", ArgType.Texture, resources.acquire(materials.select(TicketSelector.name("Material"))));
        shader.set("VoxelMap", ArgType.Texture, resources.acquire(voxels));
        shader.set("CameraMatrixInverse", ArgType.Matrix4, camInverse);
        shader.set("CameraPosition", ArgType.Vector3, cam.getLocation());
        shader.set("GridMin", ArgType.Vector3, gridMin);
        shader.set("GridMax", ArgType.Vector3, gridMax);
        shader.set("GridSize", ArgType.Int, resources.acquire(gridSize));
        shader.set("TraceQuality", ArgType.Float, GraphSource.get(traceQuality, 1f, context));
        shader.set("SpecularAngleRange", ArgType.Vector2, GraphSource.get(specularAngleRange, SPEC_RANGE, context));
        shader.set("IndirectFactor", ArgType.Float, 2.0f);
        shader.set("Target", ArgType.Image, resultImg);
        shader.set("Time", ArgType.Float, time);
        shader.execute(work.setGlobal(w, h, 1).setLocal(tracePattern.length/3 + 1, 1, 1));
        
        time += context.getTpf();
        
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
}
