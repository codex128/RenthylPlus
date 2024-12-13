/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.vxgi;

import codex.boost.material.MaterialAdapter;
import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.GeometryQueue;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.ResourceTicket;
import codex.renthyl.util.GeometryRenderHandler;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.shader.VarType;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

/**
 * Renders direct lighting for the scene and constructs geometry
 * buffers suitable for indirect lighting calculations.
 * 
 * @author codex
 */
public class DirectLightingPass extends RenderPass implements GeometryRenderHandler {
    
    public static final String TECHNIQUE = "VXGI_DirectLighting";
    private static final MaterialAdapter adapter = new MaterialAdapter();
    
    static {
        adapter.add("Common/MatDefs/Light/PBRLighting.j3md", "RenthylPlus/MatDefs/VXGI/pbrDirect.j3md");
    }
    
    private ResourceTicket<GeometryQueue> geometry;
    private ResourceTicket<float[]> lights;
    private ResourceTicket<Texture2D> lightContribution;
    private ResourceTicket<Texture2D> color;
    private ResourceTicket<Texture2D> depth;
    private ResourceTicket<Texture2D> diffuse;
    private ResourceTicket<Texture2D> position;
    private ResourceTicket<Texture2D> normals;
    private ResourceTicket<Texture2D> material;
    private final TextureDef<Texture2D> colorDef = TextureDef.texture2D(Image.Format.RGBA16F);
    private final TextureDef<Texture2D> depthDef = TextureDef.texture2D(Image.Format.Depth16);
    private final TextureDef<Texture2D> diffuseDef = TextureDef.texture2D(Image.Format.RGB16F);
    private final TextureDef<Texture2D> positionDef = TextureDef.texture2D(Image.Format.RGB16F);
    private final TextureDef<Texture2D> normalDef = TextureDef.texture2D(Image.Format.RGBA16F);
    private final TextureDef<Texture2D> materialDef = TextureDef.texture2D(Image.Format.RGBA32F);
    private final Vector2f screenSize = new Vector2f();
    private AssetManager assetManager;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        geometry = addInput("Geometry");
        lights = addInput("Lights");
        lightContribution = addInput("LightContribution");
        color = addOutput("Color");
        depth = addOutput("Depth");
        diffuse = addOutput("Diffuse");
        position = addOutput("Position");
        normals = addOutput("Normals");
        material = addOutput("Material");
        assetManager = frameGraph.getAssetManager();
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(colorDef, color);
        declare(depthDef, depth);
        declare(diffuseDef, diffuse);
        declare(positionDef, position);
        declare(normalDef, normals);
        declare(materialDef, material);
        reserve(color, diffuse, depth, material);
        reference(geometry, lights);
        referenceOptional(lightContribution);
    }
    @Override
    protected void execute(FGRenderContext context) {
        screenSize.set(context.getWidth(), context.getHeight());
        colorDef.setSize(context.getWidth(), context.getHeight());
        depthDef.setSize(colorDef);
        diffuseDef.setSize(colorDef);
        positionDef.setSize(colorDef);
        normalDef.setSize(colorDef);
        materialDef.setSize(colorDef);
        FrameBuffer fb = getFrameBuffer(context, 1);
        fb.setMultiTarget(true);
        resources.acquireColorTargets(fb, color, diffuse, position, normals, material);
        resources.acquireDepthTarget(fb, depth);
        context.getRenderer().setFrameBuffer(fb);
        context.getRenderer().clearBuffers(true, true, true);
        context.getRenderManager().setForcedTechnique(TECHNIQUE);
        context.renderGeometry(resources.acquire(geometry), null, this);
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public boolean renderGeometry(RenderManager rm, Geometry g) {
        Material m = g.getMaterial();
        if (!adapter.adaptMaterial(assetManager, m, TECHNIQUE)) {
            return false;
        }
        float[] lightData = resources.acquire(lights);
        m.setInt("VXGI_LightDataSize", lightData.length);
        m.setParam("VXGI_LightData", VarType.FloatArray, lightData);
        m.setTexture("VXGI_LightContributionMap", resources.acquireOrElse(lightContribution, null));
        m.setVector2("VXGI_ScreenSize", screenSize);
        rm.renderGeometry(g);
        return true;
    }
    
}
