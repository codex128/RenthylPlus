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
import codex.renthyl.draw.RenderMode;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.tickets.DefinedTicketList;
import codex.renthyl.resources.tickets.ResourceTicket;
import codex.renthyl.resources.tickets.TicketSelector;
import codex.renthyl.util.GeometryRenderHandler;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.shader.VarType;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import java.util.ArrayList;

/**
 * Renders direct lighting for the scene and constructs geometry
 * buffers suitable for indirect lighting calculations.
 * 
 * @author codex
 */
public class DirectLightingPass extends RenderPass implements GeometryRenderHandler {
    
    public static final RenderMode<String> TECHNIQUE = RenderMode.forcedTechnique("VXGI_DirectLighting");
    private static final MaterialAdapter adapter = new MaterialAdapter();
    
    static {
        adapter.add("Common/MatDefs/Light/PBRLighting.j3md", "RenthylPlus/MatDefs/VXGI/pbrDirect.j3md");
    }
    
    private ResourceTicket<GeometryQueue> geometry;
    private ResourceTicket<float[]> lights;
    private ResourceTicket<Texture2D> lightContribution;
    private ResourceTicket<Texture2D> color;
    private ResourceTicket<Texture2D> depth;
    private DefinedTicketList<Texture2D, TextureDef<Texture2D>> materials;
    private final TextureDef<Texture2D> colorDef = TextureDef.texture2D(Image.Format.RGBA16F);
    private final TextureDef<Texture2D> depthDef = TextureDef.texture2D(Image.Format.Depth16);
    private final ArrayList<ResourceTicket<Texture2D>> colorTargets = new ArrayList<>();
    private final Vector2f screenSize = new Vector2f();
    private AssetManager assetManager;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        geometry = addInput("Geometry");
        lights = addInput("Lights");
        lightContribution = addInput("LightContribution");
        color = addOutput("Color");
        depth = addOutput("Depth");
        materials = addOutputGroup(new DefinedTicketList<>("Material"));
        materials.add("Diffuse", TextureDef.texture2D(Image.Format.RGBA16F));
        materials.add("Position", TextureDef.texture2D(Image.Format.RGBA16F));
        materials.add("Normals", TextureDef.texture2D(Image.Format.RGBA16F));
        materials.add("Material", TextureDef.texture2D(Image.Format.RGBA32F));
        assetManager = frameGraph.getAssetManager();
    }
    @Override
    protected void prepare(FGRenderContext context) {
        // TODO: group gbuffers into one ticket group
        declare(colorDef, color);
        declare(depthDef, depth);
        materials.declareAll(resources, this);
        reserve(color, depth);
        reserve(materials);
        reference(geometry, lights);
        referenceOptional(lightContribution);
    }
    @Override
    protected void execute(FGRenderContext context) {
        screenSize.set(context.getWidth(), context.getHeight());
        colorDef.setSize(context.getWidth(), context.getHeight());
        depthDef.setSize(colorDef);
        for (TextureDef<Texture2D> d : materials.getDefs()) {
            d.setSize(colorDef);
        }
        FrameBuffer fb = getFrameBuffer(context, 1);
        fb.setMultiTarget(true);
        colorTargets.add(color);
        resources.acquireColorTargets(fb, materials.select(TicketSelector.All, colorTargets));
        resources.acquireDepthTarget(fb, depth);
        context.registerMode(RenderMode.frameBuffer(fb));
        context.clearBuffers();
        context.registerMode(TECHNIQUE);
        resources.acquire(geometry).render(context, this);
    }
    @Override
    protected void reset(FGRenderContext context) {
        colorTargets.clear();
    }
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public void renderGeometry(FGRenderContext context, Geometry g) {
        Material m = g.getMaterial();
        if (!adapter.adaptMaterial(assetManager, m, TECHNIQUE.getTargetValue())) {
            return;
        }
        float[] lightData = resources.acquire(lights);
        m.setInt("VXGI_LightDataSize", lightData.length);
        m.setParam("VXGI_LightData", VarType.FloatArray, lightData);
        m.setTexture("VXGI_LightContributionMap", resources.acquireOrElse(lightContribution, null));
        m.setVector2("VXGI_ScreenSize", screenSize);
        context.getRenderManager().renderGeometry(g);
    }
    
}
