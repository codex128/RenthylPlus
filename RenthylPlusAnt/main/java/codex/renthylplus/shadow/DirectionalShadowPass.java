/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import codex.renthyl.FGRenderContext;
import codex.renthyl.GeometryQueue;
import codex.renthyl.resources.tickets.ResourceTicket;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.shadow.PssmShadowUtil;
import com.jme3.shadow.ShadowUtil;
import com.jme3.util.TempVars;

/**
 *
 * @author codex
 */
public class DirectionalShadowPass extends ShadowOcclusionPass<DirectionalLight> {
    
    private final Camera shadowCam;
    private final float[] splits;
    private final Vector3f[] points = new Vector3f[8];
    private float lambda = 0.65f;
    
    public DirectionalShadowPass(int shadowMapSize, int numSplits) {
        super(Light.Type.Directional, numSplits, shadowMapSize);
        shadowCam = new Camera(shadowMapSize, shadowMapSize);
        shadowCam.setParallelProjection(true);
        splits = new float[numShadowMaps + 1];
        for (int i = 0; i < points.length; i++) {
            points[i] = new Vector3f();
        }
    }
    
    @Override
    protected void execute(FGRenderContext context) {
        Camera viewCam = context.getCurrentCamera();
        float near = Math.max(viewCam.getFrustumNear(), 0.001f);
        shadowCam.setFrustumFar(viewCam.getFrustumFar());
        ShadowUtil.updateFrustumPoints(viewCam, near, viewCam.getFrustumFar(), 1.0f, points);
        PssmShadowUtil.updateFrustumSplits(splits, near, viewCam.getFrustumFar(), lambda);
        if (viewCam.isParallelProjection()) {
            float factor = 1f / (viewCam.getFrustumFar() - near);
            for (int i = 0; i < numShadowMaps; i++) {
                splits[i] *= factor;
            }
        }
        super.execute(context);
    }
    @Override
    protected boolean lightSourceInsideFrustum(Camera cam, DirectionalLight light) {
        return true;
    }
    @Override
    protected Camera getShadowCamera(FGRenderContext context, Camera viewCam,
            GeometryQueue occluders, GeometryQueue receivers, DirectionalLight light, int index) {
        shadowCam.getRotation().lookAt(light.getDirection(), shadowCam.getUp());
        shadowCam.update();
        shadowCam.updateViewProjection();
        fitCameraToQueues(occluders, receivers, shadowCam, points, shadowMapDef.getMapDef().getWidth());
        return shadowCam;
    }
    @Override
    protected ShadowMap acquireShadowMap(Camera cam, DirectionalLight light, ResourceTicket<ShadowMap> ticket, int i) {
        ShadowMap map = super.acquireShadowMap(cam, light, ticket, i);
        map.setSplit(splits[i]);
        return map;
    }
    
    public void fitCameraToQueues(GeometryQueue occluders, GeometryQueue receivers, Camera shadowCam, Vector3f[] points, float shadowMapSize) {
        
        /*
         * Copyright (c) 2009-2021 jMonkeyEngine
         * All rights reserved.
         */
        
        int numOccluders = occluders.getNumGeometries();
        if (numOccluders == 0) {
            return;
        }

        if (shadowCam.isParallelProjection()) {
            shadowCam.setFrustum(-shadowCam.getFrustumFar(), shadowCam.getFrustumFar(), -1, 1, 1, -1);
        }
        
        Matrix4f viewProjMatrix = shadowCam.getViewProjectionMatrix();
        BoundingBox splitBB = ShadowUtil.computeBoundForPoints(points, viewProjMatrix);
        TempVars vars = TempVars.get();
        BoundingBox casterBB = occluders.getWorldBound(null);
        BoundingBox receiverBB = new BoundingBox();

        shadowCam.setProjectionMatrix(null);

        int receiverCount = 0;
        for (Geometry g : receivers) {
            // transform geometry bound to projection space
            BoundingVolume recvBox = g.getWorldBound().transform(viewProjMatrix, vars.bbox);
            if (!Float.isNaN(recvBox.getCenter().x) && !Float.isInfinite(recvBox.getCenter().x)
                    && splitBB.intersects(recvBox)) {
                receiverBB.mergeLocal(recvBox);
                receiverCount++;
            }
        }

        //Nehon 08/18/2010 this is to avoid shadow bleeding when the ground is set to only receive shadows
        if (numOccluders != receiverCount) {
            casterBB.setXExtent(casterBB.getXExtent() + 2.0f);
            casterBB.setYExtent(casterBB.getYExtent() + 2.0f);
            casterBB.setZExtent(casterBB.getZExtent() + 2.0f);
        }

        Vector3f casterMin = casterBB.getMin(vars.vect1);
        Vector3f casterMax = casterBB.getMax(vars.vect2);
        Vector3f receiverMin = receiverBB.getMin(vars.vect3);
        Vector3f receiverMax = receiverBB.getMax(vars.vect4);
        Vector3f splitMin = splitBB.getMin(vars.vect5);
        Vector3f splitMax = splitBB.getMax(vars.vect6);
        splitMin.z = 0;

        Matrix4f projMatrix = shadowCam.getProjectionMatrix();
        Vector3f cropMin = vars.vect7;
        Vector3f cropMax = vars.vect8;

        // IMPORTANT: Special handling for Z values
        cropMin.x = Math.max(Math.max(casterMin.x, receiverMin.x), splitMin.x);
        cropMax.x = Math.min(Math.min(casterMax.x, receiverMax.x), splitMax.x);
        cropMin.y = Math.max(Math.max(casterMin.y, receiverMin.y), splitMin.y);
        cropMax.y = Math.min(Math.min(casterMax.y, receiverMax.y), splitMax.y);
        cropMin.z = Math.min(casterMin.z, splitMin.z);
        cropMax.z = Math.min(receiverMax.z, splitMax.z);

        // Create the crop matrix.
        float scaleX, scaleY, scaleZ;
        float offsetX, offsetY, offsetZ;

        float deltaCropX = cropMax.x - cropMin.x;
        float deltaCropY = cropMax.y - cropMin.y;
        scaleX = deltaCropX == 0 ? 0 : 2.0f / deltaCropX;
        scaleY = deltaCropY == 0 ? 0 : 2.0f / deltaCropY;

        //Shadow map stabilization approximation from shaderX 7
        //from Practical Cascaded Shadow maps adapted to PSSM
        //scale stabilization
        float halfTextureSize = shadowMapSize * 0.5f;

        if (halfTextureSize != 0 && scaleX >0 && scaleY>0) {
            float scaleQuantizer = 0.1f;
            scaleX = 1.0f / FastMath.ceil(1.0f / scaleX * scaleQuantizer) * scaleQuantizer;
            scaleY = 1.0f / FastMath.ceil(1.0f / scaleY * scaleQuantizer) * scaleQuantizer;
        }

        offsetX = -0.5f * (cropMax.x + cropMin.x) * scaleX;
        offsetY = -0.5f * (cropMax.y + cropMin.y) * scaleY;

        //Shadow map stabilization approximation from shaderX 7
        //from Practical Cascaded Shadow maps adapted to PSSM
        //offset stabilization
        if (halfTextureSize != 0  && scaleX >0 && scaleY>0) {
            offsetX = FastMath.ceil(offsetX * halfTextureSize) / halfTextureSize;
            offsetY = FastMath.ceil(offsetY * halfTextureSize) / halfTextureSize;
        }

        float deltaCropZ = cropMax.z - cropMin.z;
        scaleZ = deltaCropZ == 0 ? 0 : 1.0f / deltaCropZ;
        offsetZ = -cropMin.z * scaleZ;
        
        Matrix4f cropMatrix = vars.tempMat4;
        cropMatrix.set(scaleX, 0f, 0f, offsetX,
                0f, scaleY, 0f, offsetY,
                0f, 0f, scaleZ, offsetZ,
                0f, 0f, 0f, 1f);

        Matrix4f result = new Matrix4f();
        result.set(cropMatrix);
        result.multLocal(projMatrix);
        vars.release();

        shadowCam.setProjectionMatrix(result);
        
    }
    
}
