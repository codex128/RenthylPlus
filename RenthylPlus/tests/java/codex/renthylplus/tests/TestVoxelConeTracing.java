/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.tests;

import codex.jmecompute.UniversalShaderLoader;
import codex.jmecompute.opengl.GLComputeManager;
import codex.renthyl.FrameGraph;
import codex.renthyl.Renthyl;
import codex.renthyl.client.GraphSetting;
import codex.renthyl.client.GraphSource;
import codex.renthyl.modules.Junction;
import codex.renthyl.modules.OutputPass;
import codex.renthyl.modules.cache.CacheRead;
import codex.renthyl.modules.cache.CacheWrite;
import codex.renthyl.modules.geometry.GeometryDepthPass;
import codex.renthyl.modules.geometry.SceneEnqueuePass;
import codex.renthyl.util.IndexSwitch;
import codex.renthylplus.shadow.ShadowComposerPass;
import codex.renthylplus.shadow.ShadowMapViewPass;
import codex.renthylplus.shadow.SpotShadowPass;
import codex.renthylplus.vxgi.DirectLightingPass;
import codex.renthylplus.vxgi.LightArrayPass;
import codex.renthylplus.vxgi.LightGatherPass;
import codex.renthylplus.vxgi.VoxelDebugSlicePass;
import codex.renthylplus.vxgi.VoxelEnvSetupPass;
import codex.renthylplus.vxgi.IndirectLightingPass;
import codex.renthylplus.vxgi.VoxelShadowComposerPass;
import codex.renthylplus.vxgi.VoxelVisualizerPass;
import codex.renthylplus.vxgi.VoxelizationPass;
import com.github.stephengold.wrench.LwjglAssetLoader;
import com.jme3.app.DetailedProfilerState;
import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.KeyTrigger;
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
import com.jme3.texture.Texture3D;

/**
 *
 * @author codex
 */
public class TestVoxelConeTracing extends SimpleApplication {
    
    private int frame = 0;
    private final int numLights = 0;
    
    public static void main(String[] args) {
        TestVoxelConeTracing app = new TestVoxelConeTracing();
        AppSettings settings = new AppSettings(true);
        settings.setWidth(768);
        settings.setHeight(768);
        settings.setVSync(false);
        settings.setFrameRate(0);
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
        
        SpotLight spot = new SpotLight();
        spot.setPosition(new Vector3f(15, 15, 15));
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
                
        stateManager.attach(new DetailedProfilerState());
        
        cam.setLocation(new Vector3f(-25, 25, -25));
        cam.setFov(100);
        flyCam.setMoveSpeed(50);
        flyCam.setDragToRotate(true);
        
        FrameGraph fg = new FrameGraph(assetManager);
        viewPort.setPipeline(fg);
        
        GraphSetting<String> voxelCacheKey = new GraphSetting<>("TemporalVoxels", "TemporalVoxels");
        
        CacheRead<Texture3D> voxelRead = fg.add(new CacheRead<>(Texture3D.class, voxelCacheKey));
        SceneEnqueuePass enqueue = fg.add(SceneEnqueuePass.withDefaultQueue(true));
        GeometryDepthPass depth = fg.add(new GeometryDepthPass());
        SpotShadowPass spotShadows = fg.add(new SpotShadowPass(1024));
        VoxelEnvSetupPass voxelEnv = fg.add(new VoxelEnvSetupPass());
        ShadowComposerPass shadows = fg.add(new ShadowComposerPass());
        VoxelShadowComposerPass voxShadows = fg.add(new VoxelShadowComposerPass());
        LightGatherPass lightGather = fg.add(new LightGatherPass());
        LightArrayPass lightArray = fg.add(new LightArrayPass());
        DirectLightingPass direct = fg.add(new DirectLightingPass());
        VoxelizationPass voxels = fg.add(new VoxelizationPass());
        VoxelVisualizerPass vis = fg.add(new VoxelVisualizerPass());
        VoxelDebugSlicePass sliceDebug = fg.add(new VoxelDebugSlicePass());
        IndirectLightingPass indirect = fg.add(new IndirectLightingPass());
        Junction outJunct = fg.add(new Junction(6, 1));
        OutputPass out = fg.add(new OutputPass());
        ShadowMapViewPass shadowDebug = fg.add(new ShadowMapViewPass());
        CacheWrite voxelWrite = fg.add(new CacheWrite(voxelCacheKey));
        
        depth.makeInput(enqueue, "Default", "Geometry");
        spotShadows.makeInput(enqueue, "Default", "Occluders");
        shadows.makeInput(depth, "Depth", "ReceiverDepth");
        shadows.makeGroupInputToList(spotShadows, "ShadowMaps", "ShadowMaps");
        voxShadows.makeInput(voxelEnv, "GridSize", "GridSize");
        voxShadows.makeInput(voxelEnv, "Bounds", "Bounds");
        voxShadows.makeGroupInputToList(spotShadows, "ShadowMaps", "ShadowMaps");
        lightArray.makeInput(lightGather, "Lights", "Lights");
        lightArray.makeInput(voxShadows, "LightShadowIndices", "Shadows");
        direct.makeInput(enqueue, "Default", "Geometry");
        direct.makeInput(lightArray, "LightArray", "Lights");
        direct.makeInput(shadows, "LightContribution", "LightContribution");
        voxels.makeInput(enqueue, "Default", "Geometry");
        voxels.makeInput(lightArray, "LightArray", "Lights");
        voxels.makeInput(lightArray, "Ambient", "Ambient");
        voxels.makeInput(voxShadows, "LightContribution", "LightContribution");
        voxels.makeInput(voxelEnv, "GridSize", "GridSize");
        voxels.makeInput(voxelEnv, "Bounds", "Bounds");
        voxels.makeInput(voxelRead, CacheRead.OUTPUT, "TemporalVoxels");
        //sliceDebug.makeInput(voxels, "Voxels", "Voxels");
        sliceDebug.makeInput(voxShadows, "LightContribution", "Voxels");
        vis.makeInput(voxels, "Voxels", "Voxels");
        vis.makeInput(voxelEnv, "Bounds", "Bounds");
        vis.makeInput(enqueue, "Default", "Geometry");
        indirect.makeInput(direct, "Color", "SceneColor");
        indirect.makeInput(direct, "Depth", "SceneDepth");
        indirect.makeInput(direct, "Diffuse", "Diffuse");
        indirect.makeInput(direct, "Position", "Position");
        indirect.makeInput(direct, "Normals", "Normals");
        indirect.makeInput(direct, "Material", "Material");
        indirect.makeInput(voxels, "Voxels", "Voxels");
        indirect.makeInput(voxelEnv, "Bounds", "Bounds");
        indirect.makeInput(voxelEnv, "GridSize", "GridSize");
        outJunct.makeInput(indirect, "Result", Junction.getInput(0));
        outJunct.makeInput(shadows, "LightContribution", Junction.getInput(1));
        outJunct.makeInput(vis, "Color", Junction.getInput(2));
        outJunct.makeInput(sliceDebug, "Result", Junction.getInput(3));
        outJunct.makeInput(depth, "Depth", Junction.getInput(4));
        outJunct.makeInput(direct, "Color", Junction.getInput(5));
        out.makeInput(outJunct, Junction.getOutput(), "Color");
        voxelWrite.makeInput(voxels, "Voxels", CacheWrite.INPUT);
        //shadowDebug.makeInput(spotShadows, "ShadowMaps[0]", "ShadowMap");
        
        enqueue.setFrustumCulling(false);
        voxelEnv.setBounds(GraphSource.value(new BoundingBox(Vector3f.ZERO, 40, 40, 40)));
        voxelEnv.setGridSize(GraphSource.value(256));
        new IndexSwitch(inputManager, new KeyTrigger(KeyInput.KEY_SPACE)).setJunction(outJunct);
        spotShadows.setLightSource(GraphSource.value(spot));
        
    }
    @Override
    public void simpleUpdate(float tpf) {
        if (++frame < 5) {
            cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
        }
    }
    
}
