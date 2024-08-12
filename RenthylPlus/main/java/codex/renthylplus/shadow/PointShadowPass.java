/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import codex.renthyl.FGRenderContext;
import codex.renthyl.GeometryQueue;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

/**
 *
 * @author codex
 */
public class PointShadowPass extends ShadowOcclusionPass<PointLight> {
    
    private static final int NUM_CAMS = 6;
    
    private final Camera[] shadowCams = new Camera[NUM_CAMS];
    private final float[] radii = new float[NUM_CAMS];
    
    public PointShadowPass(int size) {
        super(Light.Type.Point, NUM_CAMS, size);
        for (int i = 0; i < shadowCams.length; i++) {
            Camera c = shadowCams[i] = new Camera(shadowMapDef.getMapDef().getWidth(), shadowMapDef.getMapDef().getWidth());
            c.setAxes(Vector3f.UNIT_X.mult(-1f), Vector3f.UNIT_Z.mult(-1f), Vector3f.UNIT_Y.mult(-1f));
            radii[i] = -1;
        }
    }
    
    @Override
    protected Camera getShadowCamera(FGRenderContext context, GeometryQueue occluders, PointLight light, int index) {
        Camera c = shadowCams[index];
        if (radii[index] != light.getRadius() || !c.getLocation().equals(light.getPosition())) {
            radii[index] = light.getRadius();
            c.setFrustumPerspective(90, 1, 0.1f, light.getRadius());
            c.setLocation(light.getPosition());
            c.update();
            c.updateViewProjection();
        }
        return c;
    }
    @Override
    protected boolean lightSourceInsideFrustum(Camera cam, PointLight light) {
        return cam.contains(light.getPosition());
    }
    
}
