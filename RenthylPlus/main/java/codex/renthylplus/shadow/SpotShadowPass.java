/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import codex.renthyl.FGRenderContext;
import codex.renthyl.GeometryQueue;
import com.jme3.light.Light;
import com.jme3.light.SpotLight;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

/**
 *
 * @author codex
 */
public class SpotShadowPass extends ShadowOcclusionPass<SpotLight> {
    
    private Camera shadowCam;
    private final Vector3f direction = new Vector3f();
    private float range = -1;
    private float outerAngle = -1;
    
    public SpotShadowPass(int size) {
        super(Light.Type.Spot, 1, size);
        shadowCam = new Camera(shadowMapDef.getMapDef().getWidth(), shadowMapDef.getMapDef().getHeight());
    }

    @Override
    protected Camera getShadowCamera(FGRenderContext context, GeometryQueue occluders, SpotLight light, int index) {
        
        //if (range != light.getSpotRange() || outerAngle != light.getSpotOuterAngle()
        //        || !shadowCam.getLocation().equals(light.getPosition()) || !direction.equals(light.getDirection())) {
            range = light.getSpotRange();
            outerAngle = light.getSpotOuterAngle();
            direction.set(light.getDirection());
            shadowCam.setFrustumPerspective(outerAngle * FastMath.RAD_TO_DEG * 2, 1, 1, range);
            shadowCam.getRotation().lookAt(direction, shadowCam.getUp());
            shadowCam.setLocation(light.getPosition());
            shadowCam.update();
            shadowCam.updateViewProjection();
        //}
        
        return shadowCam;
        
    }
    @Override
    protected boolean lightSourceInsideFrustum(Camera cam, SpotLight light) {
        return ShadowOcclusionPass.pointInsideFrustum(cam, light.getPosition());
    }
    
}
