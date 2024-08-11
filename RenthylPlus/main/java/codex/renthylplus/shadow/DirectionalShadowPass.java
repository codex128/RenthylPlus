/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import codex.renthyl.GeometryQueue;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.OpaqueComparator;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.shadow.PssmShadowUtil;
import com.jme3.shadow.ShadowUtil;
import static com.jme3.shadow.ShadowUtil.computeBoundForPoints;
import com.jme3.util.TempVars;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 *
 * @author codex
 */
public class DirectionalShadowPass extends ShadowPass<DirectionalLight> {
    
    private float lambda = 0.65f;    
    private Camera shadowCam;
    private ColorRGBA splits;
    private float[] splitsArray;
    private final Vector3f[] points = new Vector3f[8];
    private boolean stabilize = true;
    
    public DirectionalShadowPass(int size, int splits) {
        super(Light.Type.Directional, splits, size);
        if (splits < 1 || splits > 4) {
            throw new IllegalArgumentException("Number of splits must be between 1 and 4 (inclusive).");
        }
    }

    @Override
    protected void configureShadowCams(DirectionalLight light, Camera viewCam) {
        
        float zFar = zFarOverride;
        if (zFar == 0) {
            zFar = viewCam.getFrustumFar();
        }

        //We prevent computing the frustum points and splits with zeroed or negative near clip value
        float frustumNear = Math.max(viewCam.getFrustumNear(), 0.001f);
        ShadowUtil.updateFrustumPoints(viewCam, frustumNear, zFar, 1.0f, points);

        shadowCam.setFrustumFar(zFar);
        shadowCam.getRotation().lookAt(light.getDirection(), shadowCam.getUp());
        shadowCam.update();
        shadowCam.updateViewProjection();

        PssmShadowUtil.updateFrustumSplits(splitsArray, frustumNear, zFar, lambda);

        // in parallel projection shadow position goes from 0 to 1
        if (viewCam.isParallelProjection()) {
            for (int i = 0; i < nbShadowMaps; i++) {
                splitsArray[i] = splitsArray[i]/(zFar- frustumNear);
            }
        }

        switch (splitsArray.length) {
            case 5:
                splits.a = splitsArray[4];
            case 4:
                splits.b = splitsArray[3];
            case 3:
                splits.g = splitsArray[2];
            case 2:
            case 1:
                splits.r = splitsArray[1];
                break;
        }
        
    }
    
    @Override
    protected boolean isLightInView(Camera viewCam, DirectionalLight light) {
        return true;
    }

    @Override
    protected Camera getShadowCam(int shadowMapIndex) {
        return shadowCam;
    }

    @Override
    protected void updateShadowCam(ViewPort viewPort, GeometryQueue occluders, GeometryQueue receivers, int shadowMapIndex) {
        Camera viewCam = viewPort.getCamera();
        ShadowUtil.updateFrustumPoints(viewCam, splitsArray[shadowMapIndex], splitsArray[shadowMapIndex+1], 1.0f, points);
        updateShadowCamera(viewPort, receivers, shadowCam, points, occluders, zFarOverride);
    }
    
    @Override
    protected void setupReceiverMaterial(Material mat) {
        super.setupReceiverMaterial(mat);
        mat.setColor("Splits", splits);
        mat.setVector3("LightDir", lightSource.getDirection());
    }
    
    @Override
    protected void cleanupReceiverMaterial(Material mat) {
        super.cleanupReceiverMaterial(mat);
        mat.clearParam("Splits");
        mat.clearParam("LightDir");
    }
    
    public static void updateShadowCamera(ViewPort viewPort, GeometryQueue receivers, Camera shadowCam,
            Vector3f[] points, GeometryQueue splitOccluders, float shadowMapSize) {

        boolean ortho = shadowCam.isParallelProjection();

        shadowCam.setProjectionMatrix(null);

        if (ortho) {
            shadowCam.setFrustum(-shadowCam.getFrustumFar(), shadowCam.getFrustumFar(), -1, 1, 1, -1);
        }

        // create transform to rotate points to viewspace
        Matrix4f viewProjMatrix = shadowCam.getViewProjectionMatrix();

        BoundingBox splitBB = computeBoundForPoints(points, viewProjMatrix);

        TempVars vars = TempVars.get();

        BoundingBox casterBB = new BoundingBox();
        BoundingBox receiverBB = new BoundingBox();

        int casterCount = 0, receiverCount = 0;

        for (int i = 0; i < receivers.size(); i++) {
            // convert bounding box to light's viewproj space
            Geometry receiver = receivers.get(i);
            BoundingVolume bv = receiver.getWorldBound();
            BoundingVolume recvBox = bv.transform(viewProjMatrix, vars.bbox);

            if (splitBB.intersects(recvBox)) {
                //Nehon : prevent NaN and infinity values to screw the final bounding box
                if (!Float.isNaN(recvBox.getCenter().x) && !Float.isInfinite(recvBox.getCenter().x)) {
                    receiverBB.mergeLocal(recvBox);
                    receiverCount++;
                }
            }
        }

        // collect splitOccluders through scene recursive traverse
//        ShadowUtil.OccludersExtractor occExt = new ShadowUtil.OccludersExtractor(viewProjMatrix, casterCount, splitBB, casterBB, splitOccluders, vars);
//        for (Spatial scene : viewPort.getScenes()) {
//            occExt.addOccluders(scene);
//        }
        casterCount = splitOccluders.getNumGeometries();

        if (casterCount == 0) {
            vars.release();
            return;
        }

        //Nehon 08/18/2010 this is to avoid shadow bleeding when the ground is set to only receive shadows
        if (casterCount != receiverCount) {
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

//        if (!ortho) {
//            shadowCam.setFrustumPerspective(45, 1, 1, splitMax.z);
//        }

        Matrix4f projMatrix = shadowCam.getProjectionMatrix();

        Vector3f cropMin = vars.vect7;
        Vector3f cropMax = vars.vect8;

        // IMPORTANT: Special handling for Z values
        cropMin.x = max(max(casterMin.x, receiverMin.x), splitMin.x);
        cropMax.x = min(min(casterMax.x, receiverMax.x), splitMax.x);

        cropMin.y = max(max(casterMin.y, receiverMin.y), splitMin.y);
        cropMax.y = min(min(casterMax.y, receiverMax.y), splitMax.y);

        cropMin.z = min(casterMin.z, splitMin.z);
        cropMax.z = min(receiverMax.z, splitMax.z);


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
