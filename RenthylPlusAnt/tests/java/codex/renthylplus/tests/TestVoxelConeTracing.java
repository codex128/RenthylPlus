/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.tests;

import codex.jmecompute.UniversalShaderLoader;
import codex.jmecompute.opengl.GLComputeManager;
import codex.renthyl.FrameGraph;
import codex.renthyl.Renthyl;
import codex.renthyl.client.GraphSource;
import codex.renthyl.modules.ControlRenderPass;
import codex.renthyl.modules.OutputPass;
import codex.renthyl.modules.geometry.GeometryDepthPass;
import codex.renthyl.modules.geometry.SceneEnqueuePass;
import codex.renthyl.resources.tickets.DynamicTicketList;
import codex.renthyl.resources.tickets.TicketSelector;
import codex.renthylplus.shadow.ShadowComposerPass;
import codex.renthylplus.shadow.ShadowManager;
import codex.renthylplus.vxgi.LightArrayPass;
import codex.renthylplus.vxgi.LightGatherPass;
import codex.renthylplus.vxgi.VoxelConeTracer;
import com.github.stephengold.wrench.LwjglAssetLoader;
import com.jme3.app.SimpleApplication;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;

/**
 *
 * @author codex
 */
public class TestVoxelConeTracing extends SimpleApplication {
    
    private int frame = 0;
    private final int numLights = 0;
    private SpotLight spot;
    
    public static void main(String[] args) {
        TestVoxelConeTracing app = new TestVoxelConeTracing();
        AppSettings settings = new AppSettings(true);
        settings.setWidth(768);
        settings.setHeight(768);
        settings.setRenderer(AppSettings.LWJGL_OPENGL45);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }
    
    @Override
    public void simpleInitApp() {
        
        Renthyl.initialize(this);
        GLComputeManager.initialize(this);
        UniversalShaderLoader.register(assetManager);
        
        assetManager.registerLoader(LwjglAssetLoader.class,
                "3ds", "3mf", "blend", "bvh", "dae", "fbx", "glb", "gltf",
                "lwo", "meshxml", "mesh.xml", "obj", "ply", "stl");
        
        for (int i = 0; i < 0; i++) {
            Geometry g = new Geometry("box", new Box(5, 5, 5));
            g.setLocalTranslation(FastMath.rand.nextFloat(-20, 20), FastMath.rand.nextFloat(-20, 20), FastMath.rand.nextFloat(-20, 20));
            Material m = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
            m.setColor("BaseColor", ColorRGBA.randomColor());
            m.setFloat("Metallic", 0.5f);
            m.setFloat("Roughness", 0f);
            g.setMaterial(m);
            rootNode.attachChild(g);
        }
        
        Spatial temple = assetManager.loadModel("Models/temple.gltf");
        temple.setLocalScale(40f);
//        for (Spatial s : new SceneGraphIterator(temple)) {
//            if (s instanceof Geometry) {
//                Material m = ((Geometry)s).getMaterial();
//                m.setFloat("Metallic", 0.5f);
//                m.setFloat("Roughness", 0.3f);
//            }
//        }
        rootNode.attachChild(temple);
        
        spot = new SpotLight();
        spot.setPosition(new Vector3f(10, 10, 10));
        spot.setDirection(new Vector3f(-1f, -1f, -0.7f).normalizeLocal());
        spot.setColor(ColorRGBA.White.mult(4f));
        spot.setSpotRange(1000f);
        spot.setSpotOuterAngle(FastMath.PI*0.25f);
        spot.setSpotInnerAngle(FastMath.PI*0.05f);
        rootNode.addLight(spot);
        //rootNode.addLight(new DirectionalLight(new Vector3f(-1, -1, -1)));
        //rootNode.addLight(new AmbientLight(ColorRGBA.White.mult(.1f)));
        
        for (int i = 0; i < numLights; i++) {
            float angle = (FastMath.TWO_PI / numLights) * i;
            Vector3f pos = new Vector3f(FastMath.cos(angle), 0f, FastMath.sin(angle));
            pos.multLocal(25f).setY(-35f);
            PointLight pl = new PointLight(pos, ColorRGBA.randomColor(), 20f);
            rootNode.addLight(pl);
        }
                
        //stateManager.attach(new DetailedProfilerState());
        
        cam.setLocation(new Vector3f(-25, 25, -25));
        cam.setFov(100);
        flyCam.setMoveSpeed(50);
        flyCam.setDragToRotate(true);
        
        FrameGraph fg = new FrameGraph(assetManager);
        viewPort.setPipeline(fg);
        
        fg.add(new ControlRenderPass());
        SceneEnqueuePass enqueue = fg.add(SceneEnqueuePass.withSingleQueue());
        GeometryDepthPass depth = fg.add(new GeometryDepthPass());
        ShadowManager shadowMaps = fg.add(new ShadowManager());
        ShadowComposerPass shadows = fg.add(new ShadowComposerPass());
        LightGatherPass lightGather = fg.add(new LightGatherPass());
        LightArrayPass lightArray = fg.add(new LightArrayPass());
        VoxelConeTracer vct = fg.add(new VoxelConeTracer()).create();
        OutputPass out = fg.add(new OutputPass());
        
        // depth pre-pass
        depth.makeInput(enqueue, SceneEnqueuePass.SINGLE_QUEUE, "Geometry");
        
        // calculate screen shadows
        shadowMaps.makeInput(enqueue, SceneEnqueuePass.SINGLE_QUEUE, "Occluders");
        shadowMaps.makeInput(enqueue, SceneEnqueuePass.SINGLE_QUEUE, "Receivers");
        shadows.makeInput(depth, "Depth", "ReceiverDepth");
        shadowMaps.getOutputGroup(DynamicTicketList.class, "ShadowMaps").registerTargetList(
                shadows.getInputGroup(DynamicTicketList.class, "ShadowMaps"));
        
        // lights
        lightArray.makeInput(lightGather, "Lights", "Lights");
        lightArray.makeInput(shadows, "LightShadowIndices", "Shadows");
        
        // voxel cone tracing
        vct.makeInput(enqueue, SceneEnqueuePass.SINGLE_QUEUE, "Geometry");
        vct.makeInput(depth, "Depth", "Depth");
        vct.makeInput(lightArray, "LightArray", "Lights");
        vct.makeInput(shadows, "LightContribution", "LightContribution");
        vct.getInputGroup("ShadowMaps").makeInput(shadowMaps.getOutputGroup("ShadowMaps"),
                TicketSelector.All, TicketSelector.All);
        shadowMaps.getOutputGroup(DynamicTicketList.class, "ShadowMaps").registerTargetList(
                vct.getInputGroup(DynamicTicketList.class, "ShadowMaps"));
        
        out.makeInput(vct, "Result", "Color");
        //out.makeInput(depth, "Depth", "Color");
        
        shadowMaps.addSpotLight(GraphSource.value(spot), 1024);
        
    }
    @Override
    public void simpleUpdate(float tpf) {
        if (++frame < 5) {
            cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
        }
        //spot.setPosition(cam.getLocation());
        //spot.setDirection(cam.getDirection());
    }
    
}
