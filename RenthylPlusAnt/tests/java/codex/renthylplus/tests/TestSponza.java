/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.tests;

import codex.renthyl.FrameGraph;
import codex.renthyl.Renthyl;
import codex.renthylplus.RenthylPlus;
import com.github.stephengold.wrench.LwjglAssetLoader;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

/**
 *
 * @author codex
 */
public class TestSponza extends SimpleApplication {
    
    public static void main(String[] args) {
        TestSponza app = new TestSponza();
        app.start();
    }
    
    @Override
    public void simpleInitApp() {
        
        Renthyl.initialize(this);
        
        FrameGraph fg = RenthylPlus.deferred(assetManager);
        viewPort.setPipeline(fg);
        
        assetManager.registerLocator(System.getProperty("user.home"), FileLocator.class);
        assetManager.registerLoader(LwjglAssetLoader.class, "gltf");
        
        Spatial scene = assetManager.loadModel("java/assets/Sponza/Sponza.gltf");
        scene.setLocalScale(0.1f);
        rootNode.attachChild(scene);
        
        viewPort.setBackgroundColor(ColorRGBA.DarkGray);
        rootNode.addLight(new DirectionalLight(new Vector3f(1, -1, 1)));
        rootNode.addLight(new AmbientLight(ColorRGBA.White.mult(0.05f)));
        rootNode.addControl(new EnvironmentProbeControl(assetManager, 256));
        
        flyCam.setMoveSpeed(50);
        
    }
    
}
