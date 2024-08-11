/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import codex.renthyl.FrameGraph;
import codex.renthyl.GeometryQueue;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.util.TempVars;

/**
 *
 * @author codex
 */
public class PointShadowPass extends ShadowPass<PointLight> {
    
    private final Camera[] shadowCams = new Camera[6];
    
    public PointShadowPass(int size) {
        super(Light.Type.Point, 6, size);
    }

    @Override
    protected void initialize(FrameGraph frameGraph) {
        super.initialize(frameGraph);
        for (int i = 0; i < shadowCams.length; i++) {
            shadowCams[i] = new Camera(shadowMapSize, shadowMapSize);
        }
    }
    @Override
    protected void configureShadowCams(PointLight light, Camera viewCam) {
        // bottom
        shadowCams[0].setAxes(Vector3f.UNIT_X.mult(-1f), Vector3f.UNIT_Z.mult(-1f), Vector3f.UNIT_Y.mult(-1f));
        // top
        shadowCams[1].setAxes(Vector3f.UNIT_X.mult(-1f), Vector3f.UNIT_Z, Vector3f.UNIT_Y);
        // forward
        shadowCams[2].setAxes(Vector3f.UNIT_X.mult(-1f), Vector3f.UNIT_Y, Vector3f.UNIT_Z.mult(-1f));
        // backward
        shadowCams[3].setAxes(Vector3f.UNIT_X, Vector3f.UNIT_Y, Vector3f.UNIT_Z);
        // left
        shadowCams[4].setAxes(Vector3f.UNIT_Z, Vector3f.UNIT_Y, Vector3f.UNIT_X.mult(-1f));
        // right
        shadowCams[5].setAxes(Vector3f.UNIT_Z.mult(-1f), Vector3f.UNIT_Y, Vector3f.UNIT_X);
        // all
        for (Camera c : shadowCams) {
            c.setFrustumPerspective(90f, 1f, 0.1f, light.getRadius());
            c.setLocation(light.getPosition());
            c.update();
            c.updateViewProjection();
        }
    }
    @Override
    protected boolean isLightInView(Camera cam, PointLight light) {
        if (light == null) {
            return false;
        }
        TempVars vars = TempVars.get();
        boolean intersects = light.intersectsFrustum(cam, vars);
        vars.release();
        return intersects;
    }
    @Override
    protected Camera getShadowCam(int shadowMapIndex) {
        return shadowCams[shadowMapIndex];
    }
    @Override
    protected void updateShadowCam(ViewPort viewPort, GeometryQueue occluders, GeometryQueue receivers, int shadowMapIndex) {}
    
    @Override
    protected void setupReceiverMaterial(Material material) {
        material.setVector3("LightPos", lightSource.getPosition());
    }

    @Override
    protected void cleanupReceiverMaterial(Material material) {
        material.clearParam("LightPos");        
    }
    
}
