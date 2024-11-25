/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.tests;

import codex.renthyl.FrameGraph;
import codex.renthyl.Renthyl;
import codex.renthyl.modules.OutputPass;
import codex.renthyl.modules.geometry.SceneEnqueuePass;
import codex.renthylplus.vxgi.LightArrayPass;
import codex.renthylplus.vxgi.LightGatherPass;
import codex.renthylplus.vxgi.VoxelDebugSlicePass;
import codex.renthylplus.vxgi.VoxelEnvSetupPass;
import codex.renthylplus.vxgi.VoxelVisualizerPass;
import codex.renthylplus.vxgi.VoxelizationPass;
import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import org.lwjgl.opengl.GL45;

/**
 *
 * @author codex
 */
public class TestVoxelConeTracing extends SimpleApplication {
    
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
        
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                for (int k = 0; k < 20; k++) {
                    Geometry g = new Geometry("box", new Box(1, 1, 1));
                    g.setLocalTranslation(-20 + i*2, -20 + j*2, -20 + k*2);
                    Material m = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
                    m.setColor("BaseColor", ColorRGBA.Blue);
                    g.setMaterial(m);
                    rootNode.attachChild(g);
                }
            }
        }
        
        rootNode.addLight(new DirectionalLight(new Vector3f(-1, -1, -1), ColorRGBA.White));
        rootNode.addLight(new AmbientLight(ColorRGBA.DarkGray));
        
        cam.setLocation(new Vector3f(10, 10, 10));
        flyCam.setMoveSpeed(30);
        
        FrameGraph fg = new FrameGraph(assetManager);
        viewPort.setPipeline(fg);
        
        SceneEnqueuePass enqueue = fg.add(SceneEnqueuePass.withDefaultQueue(true));
        VoxelEnvSetupPass voxelEnv = fg.add(new VoxelEnvSetupPass());
        LightGatherPass lightGather = fg.add(new LightGatherPass());
        LightArrayPass lightArray = fg.add(new LightArrayPass());
        VoxelizationPass voxels = fg.add(new VoxelizationPass());
        VoxelVisualizerPass vis = fg.add(new VoxelVisualizerPass());
        VoxelDebugSlicePass debug = fg.add(new VoxelDebugSlicePass());
        OutputPass out = fg.add(new OutputPass());
        
        lightArray.makeInput(lightGather, "Lights", "Lights");
        voxels.makeInput(enqueue, "Default", "Geometry");
        voxels.makeInput(lightArray, "LightArray", "Lights");
        voxels.makeInput(lightArray, "Ambient", "Ambient");
        voxels.makeInput(voxelEnv, "GridSize", "GridSize");
        voxels.makeInput(voxelEnv, "Camera", "Camera");
        voxels.makeInput(voxelEnv, "Bounds", "Bounds"); 
        vis.makeInput(voxels, "Voxels", "Voxels");
        vis.makeInput(voxelEnv, "Bounds", "Bounds");
        vis.makeInput(enqueue, "Default", "Geometry");
        debug.makeInput(voxels, "Voxels", "Voxels");
        //out.makeInput(voxels, "ScreenSpaceResult", "Color");
        out.makeInput(vis, "Color", "Color");
        //out.makeInput(debug, "Result", "Color");
        
    }
    @Override
    public void simpleUpdate(float tpf) {
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    }
    
}
