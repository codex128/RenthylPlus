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
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import java.io.IOException;

/**
 *
 * @author codex
 */
public class SSAOPass extends JmeFilterPass {
    
    private Material material;
    private ResourceTicket<Texture2D> normals;
    private final Vector3f frustumCorner = new Vector3f();
    private final Vector2f frustumNearFar = new Vector2f();
    private Vector2f[] samples = {new Vector2f(1.0f, 0.0f), new Vector2f(-1.0f, 0.0f), new Vector2f(0.0f, 1.0f), new Vector2f(0.0f, -1.0f)};
    private float sampleRadius = 5.1f;
    private float intensity = 1.5f;
    private float scale = 0.2f;
    private float bias = 0.1f;
    private boolean useOnlyAo = false;
    private boolean useAo = true;
    private Material ssaoMat;
    private float downSampleFactor = 1f;
    private boolean approximateNormals = false;

    /**
     * Create a Screen Space Ambient Occlusion Filter
     */
    public SSAOPass() {}

    /**
     * Create a Screen Space Ambient Occlusion Filter
     * @param sampleRadius The radius of the area where random samples will be picked. default 5.1f
     * @param intensity intensity of the resulting AO. default 1.2f
     * @param scale distance between occluders and occludee. default 0.2f
     * @param bias the width of the occlusion cone considered by the occludee. default 0.1f
     */
    public SSAOPass(float sampleRadius, float intensity, float scale, float bias) {
        this.sampleRadius = sampleRadius;
        this.intensity = intensity;
        this.scale = scale;
        this.bias = bias;
    }
    
    @Override
    protected void init(FrameGraph frameGraph) {
        
        normals = addInput("Normals");
        
        ssaoMat = new Material(frameGraph.getAssetManager(), "Common/MatDefs/SSAO/ssao.j3md");
        Texture random = frameGraph.getAssetManager().loadTexture("Common/MatDefs/SSAO/Textures/random.png");
        random.setWrap(Texture.WrapMode.Repeat);
        ssaoMat.setTexture("RandomMap", random);
        Subpass ssaoPass = add(new Subpass("ssao", ssaoMat, true, true) {
            @Override
            public void beforeAcquire(FGRenderContext context) {
                Camera cam = context.getViewPort().getCamera();
                TextureDef<Texture2D> def = getDef();
                float farY = (cam.getFrustumTop() / cam.getFrustumNear()) * cam.getFrustumFar();
                float farX = farY * (def.getWidth() / def.getHeight());
                frustumCorner.set(farX, farY, cam.getFrustumFar());
                frustumNearFar.set(cam.getFrustumNear(), cam.getFrustumFar());
                def.setWidth((int)(def.getWidth() / downSampleFactor));
                def.setHeight((int)(def.getHeight() / downSampleFactor));
                Texture2D norms = resources.acquireOrElse(normals, null);
                if (norms != null) {
                    ssaoMat.setTexture("Normals", norms);
                } else {
                    ssaoMat.clearParam("Normals");
                }
                ssaoMat.setVector3("FrustumCorner", frustumCorner);
                ssaoMat.setFloat("SampleRadius", sampleRadius);
                ssaoMat.setFloat("Intensity", intensity);
                ssaoMat.setFloat("Scale", scale);
                ssaoMat.setFloat("Bias", bias);
                ssaoMat.setVector2("FrustumNearFar", frustumNearFar);
                ssaoMat.setParam("Samples", VarType.Vector2Array, samples);
                ssaoMat.setBoolean("ApproximateNormals", norms == null);
            }
        });
        
        material = new Material(frameGraph.getAssetManager(), "Common/MatDefs/SSAO/ssaoBlur.j3md");
        add(new Subpass("blur", material) {
            @Override
            public void beforeRender(FGRenderContext context) {
                material.setTexture("SSAOMap", ssaoPass.getRenderedTexture());
                material.setBoolean("UseAo", useAo);
                material.setBoolean("UseOnlyAo", useOnlyAo);
                material.setVector2("FrustumNearFar", frustumNearFar);
                float xScale = 1.0f / getDef().getWidth();
                float yScale = 1.0f / getDef().getHeight();
                material.setFloat("XScale", 2f * xScale);
                material.setFloat("YScale", 2f * yScale);
            }
        });
        
    }
    @Override
    protected void prepare(FGRenderContext context) {
        super.prepare(context);
        referenceOptional(normals);
    }
    
    /**
     * Return the bias<br>
     * see {@link  #setBias(float bias)}
     * @return  bias
     */
    public float getBias() {
        return bias;
    }

    /**
     * Sets the width of the occlusion cone considered by the occludee default is 0.1f
     *
     * @param bias the desired width (default=0.1)
     */
    public void setBias(float bias) {
        this.bias = bias;
        if (ssaoMat != null) {
            ssaoMat.setFloat("Bias", bias);
        }
    }

    /**
     * returns the ambient occlusion intensity
     * @return intensity
     */
    public float getIntensity() {
        return intensity;
    }

    /**
     * Sets the Ambient occlusion intensity default is 1.5
     *
     * @param intensity the desired intensity (default=1.5)
     */
    public void setIntensity(float intensity) {
        this.intensity = intensity;
        if (ssaoMat != null) {
            ssaoMat.setFloat("Intensity", intensity);
        }

    }

    /**
     * returns the sample radius<br>
     * see {link setSampleRadius(float sampleRadius)}
     * @return the sample radius
     */
    public float getSampleRadius() {
        return sampleRadius;
    }

    /**
     * Sets the radius of the area where random samples will be picked default 5.1f 
     *
     * @param sampleRadius the desired radius (default=5.1)
     */
    public void setSampleRadius(float sampleRadius) {
        this.sampleRadius = sampleRadius;
        if (ssaoMat != null) {
            ssaoMat.setFloat("SampleRadius", sampleRadius);
        }

    }

    /**
     * returns the scale<br>
     * see {@link #setScale(float scale)}
     * @return scale
     */
    public float getScale() {
        return scale;
    }

    /**
     * 
     * Returns the distance between occluders and occludee. default 0.2f
     *
     * @param scale the desired distance (default=0.2)
     */
    public void setScale(float scale) {
        this.scale = scale;
        if (ssaoMat != null) {
            ssaoMat.setFloat("Scale", scale);
        }
    }

    /**
     * debugging only , will be removed
     * @return true if using ambient occlusion
     */
    public boolean isUseAo() {
        return useAo;
    }

    /**
     * debugging only , will be removed
     *
     * @param useAo true to enable, false to disable (default=true)
     */
    public void setUseAo(boolean useAo) {
        this.useAo = useAo;
        if (material != null) {
            material.setBoolean("UseAo", useAo);
        }

    }

    public void setApproximateNormals(boolean approximateNormals) {
        this.approximateNormals = approximateNormals;
        if (ssaoMat != null) {
            ssaoMat.setBoolean("ApproximateNormals", approximateNormals);
        }
    }

    public boolean isApproximateNormals() {
        return approximateNormals;
    }

    /**
     * debugging only , will be removed
     * @return useOnlyAo
     */
    public boolean isUseOnlyAo() {
        return useOnlyAo;
    }

    /**
     * debugging only , will be removed
     *
     * @param useOnlyAo true to enable, false to disable (default=false)
     */
    public void setUseOnlyAo(boolean useOnlyAo) {
        this.useOnlyAo = useOnlyAo;
        if (material != null) {
            material.setBoolean("UseOnlyAo", useOnlyAo);
        }
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(sampleRadius, "sampleRadius", 5.1f);
        oc.write(intensity, "intensity", 1.5f);
        oc.write(scale, "scale", 0.2f);
        oc.write(bias, "bias", 0.1f);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        sampleRadius = ic.readFloat("sampleRadius", 5.1f);
        intensity = ic.readFloat("intensity", 1.5f);
        scale = ic.readFloat("scale", 0.2f);
        bias = ic.readFloat("bias", 0.1f);
    }
    
}
