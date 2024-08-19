/*
 * Copyright (c) 2024, codex
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package codex.renthyl.tests;

import codex.renthyl.FrameGraph;
import codex.renthylplus.RenthylPlus;
import codex.renthyl.Renthyl;
import codex.renthyl.client.MatParamTargetControl;
import codex.renthyl.modules.Attribute;
import codex.renthyl.modules.ModuleLocator;
import codex.renthylplus.shadow.ShadowMapViewer;
import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.RectangleMesh;
import com.jme3.shader.VarType;
import com.jme3.system.AppSettings;

/**
 *
 * @author codex
 */
public class TestDeferred extends TestApplication implements ActionListener {
    
    private FrameGraph fg;
    private BitmapText hud;
    private SpotLight spot;
    private PointLight point;
    private boolean moveLight = true;
    
    public static void main(String[] args){
        TestDeferred app = new TestDeferred();
        AppSettings settings = app.applySettings();
        settings.setFrameRate(-1);
        settings.setVSync(false);
        app.start();
    }
    
    @Override
    public void testInitApp() {
        
        Renthyl.initialize(this);
        
        fg = RenthylPlus.deferred(assetManager);
        viewPort.setPipeline(fg);
        rootNode.attachChild(fg.getDebugNode());
        
        Spatial tank2 = loadTank();
        tank2.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        //setupCam(tank2);
        flyCam.setMoveSpeed(30);
        flyCam.setDragToRotate(true);
        //setupLight();
        rootNode.addControl(new EnvironmentProbeControl(assetManager, 256));
        rootNode.addLight(new AmbientLight(ColorRGBA.White.mult(.01f)));
        loadSky();
        hud = loadText("", 5, windowSize.y-5, -1);
        reloadHud();
        
        Spatial tank = loadTank();
        tank.setLocalTranslation(20, 0, 0);
        //tank.setQueueBucket(RenderQueue.Bucket.Transparent);
        tank.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        
        float floorSize = 100;
        RectangleMesh floorMesh = new RectangleMesh(new Vector3f(-floorSize, 0, -floorSize),
                new Vector3f(floorSize, 0, -floorSize), new Vector3f(-floorSize, 0, floorSize));
        floorMesh.flip();
        Geometry floor = new Geometry("floor", floorMesh);
        floor.setLocalTranslation(0, -5, 0);
        Material floorMat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        floorMat.setColor("BaseColor", ColorRGBA.Green);
        floorMat.setFloat("Metallic", .5f);
        floorMat.setFloat("Roughness", .5f);
        floor.setMaterial(floorMat);
        floor.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        rootNode.attachChild(floor);
        
        spot = new SpotLight();
        spot.setPosition(new Vector3f(10, 10, -10));
        spot.setDirection(new Vector3f(-1, -1, 1));
        spot.setSpotOuterAngle(FastMath.PI*0.3f);
        spot.setSpotInnerAngle(FastMath.PI*0.2f);
        spot.setSpotRange(500);
        rootNode.addLight(spot);
        //fg.setSetting("PointLightShadowCaster", spot);
        
        point = new PointLight();
        point.setPosition(new Vector3f(10, 10, -10));
        point.setRadius(5000);
        rootNode.addLight(point);
        fg.setSetting("PointLightShadowCaster2", point);
        
        //rootNode.addLight(new AmbientLight(ColorRGBA.White.mult(0.05f)));
        
        int viewerSize = 200;
        Geometry viewer1 = loadTextureViewer(windowSize.x-viewerSize, 0, viewerSize, viewerSize);
        MatParamTargetControl viewerTarget1 = new MatParamTargetControl("ColorMap", VarType.Texture2D);
        fg.get(ModuleLocator.by(Attribute.class, "GBufferDebug")).addTarget(viewerTarget1);
        viewer1.addControl(viewerTarget1);
        
        Geometry viewer2 = loadDepthViewer(windowSize.x-viewerSize, viewerSize, viewerSize, viewerSize);
        ShadowMapViewer viewerTarget2 = new ShadowMapViewer("DepthMap", VarType.Texture2D);
        fg.get(ModuleLocator.by(Attribute.class, "ShadowDepthDebug")).addTarget(viewerTarget2);
        viewer2.addControl(viewerTarget2);
        
        Geometry viewer3 = loadTextureViewer(windowSize.x-viewerSize, viewerSize*2, viewerSize, viewerSize);
        MatParamTargetControl viewerTarget3 = new MatParamTargetControl("ColorMap", VarType.Texture2D);
        fg.get(ModuleLocator.by(Attribute.class, "LightContributionDebug")).addTarget(viewerTarget3);
        viewer3.addControl(viewerTarget3);
        
        fg.enableFeature("UseLightTextures", true);
        fg.setSetting("GBufferDebug", 0);
        
        inputManager.addMapping("UseLightTextures", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("UseLightTiling", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("gbufUp", new KeyTrigger(KeyInput.KEY_3));
        inputManager.addMapping("GrabLight", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addListener(this, "UseLightTextures", "UseLightTiling", "gbufUp", "GrabLight");
        
    }
    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("UseLightTextures") && isPressed) {
            fg.toggleFeature("UseLightTextures");
            reloadHud();
        } else if (name.equals("UseLightTiling") && isPressed) {
            fg.toggleFeature("UseLightTiling");
            reloadHud();
        } else if (name.equals("gbufUp") && isPressed) {
            int n = fg.getSetting("GBufferDebug");
            fg.setSetting("GBufferDebug", wrap(n+1, 0, 4));
        } else if (name.equals("GrabLight") && isPressed) {
            moveLight = !moveLight;
        }
    }
    @Override
    public void simpleUpdate(float tpf) {
        if (moveLight) {
            //spot.setPosition(cam.getLocation());
            //spot.setDirection(cam.getDirection());
            point.setPosition(cam.getLocation());
        }
    }
    
    private void reloadHud() {
        hud.setText("UseLightTextures: "+fg.isFeatureEnabled("UseLightTextures")
                + "\nUseLightTiles: "+fg.isFeatureEnabled("UseLightTiling"));
    }
    
    private static int wrap(int value, int min, int max) {
        if (value < min) return max-min+value+1;
        else if (value > max) return min+value-max-1;
        else return value;
    }
    
}
