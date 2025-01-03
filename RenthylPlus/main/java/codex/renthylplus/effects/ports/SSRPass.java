/*
 * Copyright (c) 2024 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package codex.renthylplus.effects.ports;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.resources.tickets.ResourceTicket;
import codex.renthylplus.effects.JmeFilterPass;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import java.awt.Point;
import java.io.IOException;

/**
 * Screenspace reflection pass.
 * 
 * @author codex
 */
public class SSRPass extends JmeFilterPass {
    
    private boolean approximateNormals = false;
    private boolean approximateGlossiness = true;
    private float downSampleFactor = 1f;
    private Material ssrMat;
    private boolean fastBlur = false;
    private int raySteps = 16;
    private boolean sampleNearby = true;
    private float stepLength = 1.0f;
    private float blurScale = 1f;
    private float sigma = 5f;
    private float reflectionFactor = 1f;
    private Vector2f nearFade = new Vector2f(0.01f, 1.0f);
    private Vector2f farFade = new Vector2f(200f, 300f);
    private int blurPasses = 4;
    private Image.Format ssrImageFormat = Image.Format.RGBA16F;
    private boolean glossinessPackedInNormalB = true;
    
    private Material blurMat;
    private ResourceTicket<Texture2D> normals;
    private Subpass[] blurArray;
    private boolean updateFlag = true;

    public SSRPass() {}
    
    @Override
    protected void init(FrameGraph frameGraph) {
        normals = addInput("Normals");
        ssrMat = new Material(frameGraph.getAssetManager(), "RenthylPlus/MatDefs/Effects/SSR.j3md"); 
        blurMat = new Material(frameGraph.getAssetManager(), "RenthylPlus/MatDefs/Effects/SSRBlur.j3md"); 
        ssrMat.setInt("RaySamples", raySteps);
        ssrMat.setInt("NearbySamples", sampleNearby ? 4 : 0);
        ssrMat.setFloat("StepLength", stepLength);
        ssrMat.setFloat("ReflectionFactor", reflectionFactor);
        ssrMat.setBoolean("GlossinessPackedInNormalB", glossinessPackedInNormalB);
        ssrMat.setBoolean("RGNormalMap", glossinessPackedInNormalB);
        ssrMat.setBoolean("ApproximateNormals", approximateNormals);
        ssrMat.setVector2("NearReflectionsFade", nearFade);
        ssrMat.setVector2("FarReflectionsFade", farFade);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        if (updateFlag) {
            updateFilterChain();
        }
        reference(normals);
        super.prepare(context);
    }
    
    private void updateFilterChain() {
        
        clearSubpasses();
        Point size = new Point();
        
        Subpass ssrPass = add(new Subpass(ssrMat, true, true) {
            @Override
            public void beforeAcquire(FGRenderContext context) {
                TextureDef<Texture2D> def = getDef();
                size.x = (int)(def.getWidth() / downSampleFactor);
                size.y = (int)(def.getHeight() / downSampleFactor);
                def.setSize(size.x, size.y);
                def.setFormat(ssrImageFormat);
            }
            @Override
            public void beforeRender(FGRenderContext context) {
                ssrMat.setTexture("Normals", resources.acquire(normals));
            }
        });
        
        blurArray = new Subpass[blurPasses];
        for(int i = 0; i < blurPasses; i++) {
            Subpass prev;
            if (i == 0) {
                prev = ssrPass;
            } else {
                prev = blurArray[i-1];
            }
            boolean horizontal = (i & 1) == 0;
            Subpass p = blurArray[i] = add(new Subpass(blurMat, true, false) {
                @Override
                public void beforeAcquire(FGRenderContext context) {
                    getDef().setSize(size.x, size.y);
                }
                @Override
                public void beforeRender(FGRenderContext context) {
                    Material mat = getMaterial();
                    mat.setTexture("SSR", prev.getRenderedTexture());
                    mat.setBoolean("Horizontal", horizontal);
                    mat.setBoolean("FastBlur", fastBlur);
                    mat.setFloat("BlurScale", blurScale);
                    mat.setFloat("Sigma", sigma);
                }
            });
            p.getDef().setFormat(Image.Format.RGBA8);
        }
        
        updateFlag = false;
        
    }
    
    /**
     * If true, any passed normals won't be used. Instead they will be approximated using the depth map
     * @param approximateNormals 
     */
    public void setApproximateNormals(boolean approximateNormals) {
        this.approximateNormals = approximateNormals;
        if(ssrMat != null){
            ssrMat.setBoolean("ApproximateNormals", approximateNormals);
        }
    }

    /**
     * Value to scale (down) the textures the filter uses.
     * Some values work better than others with approximateNormals. Good values: 1.5f, 3f;
     * @param downSampleFactor 
     */
    public void setDownSampleFactor(float downSampleFactor) {
        this.downSampleFactor = downSampleFactor;
    }
    
    /**
     * Using a faster blur function in the blur pass. Default false
     * @param fastBlur 
     */
    public void setFastBlur(boolean fastBlur) {
        this.fastBlur = fastBlur;
    }

    /**
     * Amount of steps
     * @param raySteps 
     */
    public void setRaySteps(int raySteps) {
        this.raySteps = raySteps;
        if(ssrMat != null){
            ssrMat.setInt("RaySamples", raySteps);
        }
    }

    /**
     * Whether to sample nearby ray hits for better accuracy
     * @param sampleNearby 
     */
    public void setSampleNearby(boolean sampleNearby) {
        this.sampleNearby = sampleNearby;
        if(ssrMat != null){
            ssrMat.setInt("NearbySamples", sampleNearby ? 4 : 0);
        }
    }

    /**
     * Length of each ray step
     * @param stepLength 
     */
    public void setStepLength(float stepLength) {
        this.stepLength = stepLength;
        if(ssrMat != null){
            ssrMat.setFloat("StepLength", stepLength);
        }
    }

    /**
     * Scale for blur. Only used if fastBlur is true
     * @param blurScale 
     */
    public void setBlurScale(float blurScale) {
        this.blurScale = blurScale;
    }

    /**
     * Sigma for regular gaussian blur. Only used if fastBlur is false
     * @param sigma 
     */
    public void setSigma(float sigma) {
        this.sigma = sigma;
    }

    public float getReflectionFactor() {
        return reflectionFactor;
    }

    /**
     * Sets the overall reflection amount. Scales between 0 and 1
     * @param reflectionFactor 
     */
    public void setReflectionFactor(float reflectionFactor) {
        this.reflectionFactor = reflectionFactor;
        if(ssrMat != null){
            ssrMat.setFloat("ReflectionFactor", reflectionFactor);
        }
    }
    
       
    
    public boolean isApproximateGlossiness() {
        return approximateGlossiness;
    }

    public void setApproximateGlossiness(boolean approximateGlossiness) {
        this.approximateGlossiness = approximateGlossiness;
        if(ssrMat != null){
            ssrMat.setBoolean("ApproximateGlossiness", approximateGlossiness);
        }
    }

    public Vector2f getNearFade() {
        return nearFade;
    }

    public void setNearFade(Vector2f nearFade) {
        this.nearFade = nearFade;
        if(ssrMat != null){
            ssrMat.setVector2("NearReflectionsFade", nearFade);
        }
    }

    public Vector2f getFarFade() {
        return farFade;
    }

    public void setFarFade(Vector2f farFade) {
        this.farFade = farFade;
        if(ssrMat != null){
            ssrMat.setVector2("FarReflectionsFade", farFade);
        }
    }

    public int getBlurPasses() {
        return blurPasses;
    }

    public void setBlurPasses(int blurPasses) {
        this.blurPasses = blurPasses;
        updateFlag = true;
    }

    public Image.Format getSsrImageFormat() {
        return ssrImageFormat;
    }

    public void setSsrImageFormat(Image.Format ssrImageFormat) {
        this.ssrImageFormat = ssrImageFormat;
    }

    public boolean isGlossinessPackedInNormalB() {
        return glossinessPackedInNormalB;
    }

    public void setGlossinessPackedInNormalB(boolean glossinessPackedInNormalB) {
        this.glossinessPackedInNormalB = glossinessPackedInNormalB;
        if(ssrMat != null){
            ssrMat.setBoolean("GlossinessPackedInNormalB", glossinessPackedInNormalB);
            ssrMat.setBoolean("RGNormalMap", glossinessPackedInNormalB);
        }
    }

    @Override
    public SSRPass clone(){
        SSRPass clone = new SSRPass();
        clone.approximateNormals = approximateNormals;
        clone.approximateGlossiness = approximateGlossiness;
        clone.sampleNearby = sampleNearby;
        clone.fastBlur = fastBlur;
        clone.downSampleFactor = downSampleFactor;
        clone.stepLength = stepLength;
        clone.blurScale = blurScale;
        clone.sigma = sigma;
        clone.reflectionFactor = reflectionFactor;
        clone.raySteps = raySteps;
        clone.blurPasses = blurPasses;
        clone.glossinessPackedInNormalB = glossinessPackedInNormalB;
        clone.ssrImageFormat = ssrImageFormat;
        clone.nearFade = nearFade.clone();
        clone.farFade = farFade.clone();
        return clone;
    }
    
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(approximateNormals, "approximateNormals", true);
        oc.write(approximateGlossiness, "approximateGlossiness", true);
        oc.write(sampleNearby, "sampleNearby", true);
        oc.write(fastBlur, "fastBlur", true);
        oc.write(downSampleFactor, "downSampleFactor", 1f);
        oc.write(stepLength, "stepLength", 1f);
        oc.write(blurScale, "blurScale", 1f);
        oc.write(sigma, "sigma", 5f);
        oc.write(reflectionFactor, "reflectionFactor", 1f);
        oc.write(raySteps, "raySteps", 16);
        oc.write(blurPasses, "blurPasses", 2);
        oc.write(glossinessPackedInNormalB, "glossinessPackedInNormalB", false);
        oc.write(ssrImageFormat.toString(), "ssrImageFormat", Image.Format.RGBA16F.toString());
        oc.write(new float[]{nearFade.x, nearFade.y}, "nearFade", new float[]{0.01f, 1.0f});
        oc.write(new float[]{farFade.x, farFade.y}, "farFade", new float[]{200f, 300f});
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        approximateNormals = ic.readBoolean("approximateNormals", true);
        approximateGlossiness = ic.readBoolean("intensity", true);
        sampleNearby = ic.readBoolean("sampleNearby", true);
        fastBlur = ic.readBoolean("fastBlur", true);
        downSampleFactor = ic.readFloat("downSampleFactor", 1f);
        stepLength = ic.readFloat("stepLength", 1f);
        blurScale = ic.readFloat("blurScale", 1f);
        sigma = ic.readFloat("sigma", 5f);
        reflectionFactor = ic.readFloat("reflectionFactor", 1f);
        raySteps = ic.readInt("raySteps", 16);
        blurPasses = ic.readInt("blurPasses", 2);
        glossinessPackedInNormalB = ic.readBoolean("glossinessPackedInNormalB", false);
        String format = ic.readString("ssrImageFormat", Image.Format.RGBA16F.toString());
        ssrImageFormat = Image.Format.valueOf(format);
        float[] nearArray = ic.readFloatArray("nearFade", new float[]{0.01f, 1.0f});
        nearFade = new Vector2f(nearArray[0], nearArray[1]);
        float[] farArray = ic.readFloatArray("farFade", new float[]{200f, 300f});
        farFade = new Vector2f(farArray[0], farArray[1]);
    }
    
}
