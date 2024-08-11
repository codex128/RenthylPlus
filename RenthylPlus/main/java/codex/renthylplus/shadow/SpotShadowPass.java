/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import codex.renthyl.GeometryQueue;
import com.jme3.light.Light;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.shadow.ShadowUtil;
import com.jme3.util.TempVars;

/**
 *
 * @author codex
 */
public class SpotShadowPass extends ShadowPass<SpotLight> {
    
    protected Camera shadowCam;    
    protected SpotLight light;
    protected final Vector3f[] points = new Vector3f[8];
    
    public SpotShadowPass(int shadowMapSize) {
        super(Light.Type.Spot, 1, shadowMapSize);
        shadowCam = new Camera(this.shadowMapSize, this.shadowMapSize);
        for (int i = 0; i < points.length; i++) {
            points[i] = new Vector3f();
        }
    }

    @Override
    protected void configureShadowCams(SpotLight light, Camera viewCam) {
        float zFar = zFarOverride;
        if (zFar == 0) {
            zFar = viewCam.getFrustumFar();
        }

        //We prevent computing the frustum points and splits with zeroed or negative near clip value
        float frustumNear = Math.max(viewCam.getFrustumNear(), 0.001f);
        ShadowUtil.updateFrustumPoints(viewCam, frustumNear, zFar, 1.0f, points);

        shadowCam.setFrustumPerspective(light.getSpotOuterAngle() * FastMath.RAD_TO_DEG * 2.0f, 1, 1f, light.getSpotRange());
        shadowCam.getRotation().lookAt(light.getDirection(), shadowCam.getUp());
        shadowCam.setLocation(light.getPosition());

        shadowCam.update();
        shadowCam.updateViewProjection();
    }

    @Override
    protected void updateShadowCam(ViewPort viewPort, GeometryQueue occluders, GeometryQueue receivers, int shadowMapIndex) {}

    @Override
    protected boolean isLightInView(Camera viewCam, SpotLight light) {
        TempVars vars = TempVars.get();
        boolean intersects = light.intersectsFrustum(viewCam,vars);
        vars.release();
        return intersects;
    }

    @Override
    protected Camera getShadowCam(int shadowMapIndex) {
        return shadowCam;
    }
    
    @Override
    protected void setupReceiverMaterial(Material material) {    
         material.setVector3("LightPos", lightSource.getPosition());
         material.setVector3("LightDir", lightSource.getDirection());
    }

    @Override
    protected void cleanupReceiverMaterial(Material material) {
        material.clearParam("LightPos");
        material.clearParam("LightDir");
    }
    
}
