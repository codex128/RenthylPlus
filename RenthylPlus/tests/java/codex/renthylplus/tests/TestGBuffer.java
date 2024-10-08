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
package codex.renthylplus.tests;

import codex.renthyl.FrameGraph;
import codex.renthylplus.RenthylPlus;
import codex.renthyl.Renthyl;
import codex.renthylplus.deferred.GBufferPass;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.RectangleMesh;

/**
 *
 * @author codex
 */
public class TestGBuffer extends TestApplication {

    public static void main(String[] args) {
        TestGBuffer app = new TestGBuffer();
        app.applySettings();
        app.start();
    }
    
    @Override
    protected void testInitApp() {
        
        Renthyl.initialize(this);
        FrameGraph fg = RenthylPlus.Test.testGBuffer(assetManager);
        fg.setSetting("GBuffer", 0);
        viewPort.setPipeline(fg);
        
        GBufferPass.adaptAllMaterials(assetManager);
        
        
        
        setupAll();
        
        
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
        //floorMat.setFloat("EmissiveIntensity", 2.0f);
        //floorMat.setTexture("LightMap", assetManager.loadTexture("Models/Tank/Tank_Occ_Rough_Metal.png"));
        floor.setMaterial(floorMat);
        floor.setShadowMode(RenderQueue.ShadowMode.Cast);
        rootNode.attachChild(floor);
        
        ActionListener action = (String name, boolean isPressed, float tpf) -> {
            if (isPressed) {
                int n = fg.getSetting("GBuffer");
                if (name.equals("up")) {
                    fg.setSetting("GBuffer", wrap(n+1, 0, 4));
                } else if (name.equals("down")) {
                    fg.setSetting("GBuffer", wrap(n-1, 0, 4));
                }
            }
        };
        
        inputManager.addMapping("up", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("down", new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addListener(action, "up", "down");
        
    }
    
    private static int wrap(int value, int min, int max) {
        if (value < min) return max-min+value+1;
        else if (value > max) return min+value-max-1;
        else return value;
    }
    
}
