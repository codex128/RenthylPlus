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
import codex.renthyl.GeometryQueue;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.draw.RenderMode;
import codex.renthyl.resources.tickets.ResourceTicket;
import codex.renthyl.util.GeometryRenderHandler;
import codex.renthylplus.effects.JmeFilterPass;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.post.filters.BloomFilter;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import java.io.IOException;

/**
 *
 * @author codex
 */
public class BloomPass extends JmeFilterPass {
    
    private BloomFilter.GlowMode glowMode = BloomFilter.GlowMode.Scene;
    private float blurScale = 1.5f;
    private float exposurePower = 5.0f;
    private float exposureCutOff = 0.0f;
    private float bloomIntensity = 2.0f;
    private float downSamplingFactor = 1;
    private Material extractMat;
    private Material vBlurMat;
    private Material hBlurMat;
    private Material finalMat;
    
    private ResourceTicket<GeometryQueue> geometry;
    private ResourceTicket<Texture2D> objectGlow;
    private final ResourceTicket<Texture2D> geometryResult = new ResourceTicket<>("_geometry_result");
    private final ResourceTicket<Texture2D> geometryDepth = new ResourceTicket<>("_geometry_depth");
    private final TextureDef<Texture2D> colorDef = TextureDef.texture2D(Image.Format.RGBA16F);
    private final TextureDef<Texture2D> depthDef = TextureDef.texture2D(Image.Format.Depth);
    private Texture2D glowMap;
    private int screenWidth;
    private int screenHeight;
    
    /**
     * Creates a Bloom filter
     */
    public BloomPass() {}

    /**
     * Creates the bloom filter with the specified glow mode
     *
     * @param glowMode the desired mode (default=Scene)
     */
    public BloomPass(BloomFilter.GlowMode glowMode) {
        this.glowMode = glowMode;
    }

    @Override
    protected void init(FrameGraph frameGraph) {
        
        geometry = addInput("Geometry");
        objectGlow = addInput("ObjectGlow");
        
        //configuring extractPass
        extractMat = new Material(frameGraph.getAssetManager(), "Common/MatDefs/Post/BloomExtract.j3md");
        Subpass extractPass = add(new Subpass(extractMat, true, false) {
            @Override
            public void beforeAcquire(FGRenderContext context) {
                getDef().setSize(screenWidth, screenHeight);
            }
            @Override
            public void beforeRender(FGRenderContext context) {
                extractMat.setFloat("ExposurePow", exposurePower);
                extractMat.setFloat("ExposureCutoff", exposureCutOff);
                extractMat.setBoolean("Extract", glowMode != BloomFilter.GlowMode.Objects);
                if (glowMode != BloomFilter.GlowMode.Scene) {
                    extractMat.setTexture("GlowMap", glowMap);
                }
            }
        });

        //configuring horizontal blur pass
        hBlurMat = new Material(frameGraph.getAssetManager(), "Common/MatDefs/Blur/HGaussianBlur.j3md");
        Subpass hBlurPass = add(new Subpass(hBlurMat, false, false) {
            @Override
            public void beforeAcquire(FGRenderContext context) {
                getDef().setSize(screenWidth, screenHeight);
            }
            @Override
            public void beforeRender(FGRenderContext context) {
                hBlurMat.setTexture("Texture", extractPass.getRenderedTexture());
                hBlurMat.setFloat("Size", screenWidth);
                hBlurMat.setFloat("Scale", blurScale);
            }
        });

        //configuring vertical blur pass
        vBlurMat = new Material(frameGraph.getAssetManager(), "Common/MatDefs/Blur/VGaussianBlur.j3md");
        Subpass vBlurPass = add(new Subpass(vBlurMat, false, false) {
            @Override
            public void beforeAcquire(FGRenderContext context) {
                getDef().setSize(screenWidth, screenHeight);
            }
            @Override
            public void beforeRender(FGRenderContext context) {
                vBlurMat.setTexture("Texture", hBlurPass.getRenderedTexture());
                vBlurMat.setFloat("Size", screenHeight);
                vBlurMat.setFloat("Scale", blurScale);
            }
        });
        
        //final material
        finalMat = new Material(frameGraph.getAssetManager(), "Common/MatDefs/Post/BloomFinal.j3md");
        add(new Subpass(finalMat, true, false) {
            @Override
            public void beforeRender(FGRenderContext context) {
                finalMat.setTexture("BloomTex", vBlurPass.getRenderedTexture());
                finalMat.setFloat("BloomIntensity", bloomIntensity);
            }
        });
        
    }
    @Override
    protected void prepare(FGRenderContext context) {
        super.prepare(context);
        boolean scene = glowMode == BloomFilter.GlowMode.Scene;
        if (!scene) {
            referenceOptional(geometry, objectGlow);
            if (ResourceTicket.validate(geometry)) {
                declareTemporary(colorDef, geometryResult);
                declareTemporary(depthDef, geometryDepth);
            }
        }
        
    }
    @Override
    protected void execute(FGRenderContext context) {
        // multiple acquire calls with the same ticket is perfectly fine
        Texture2D inColor = resources.acquire(sceneColor);
        screenWidth = (int)Math.max(1, (inColor.getImage().getWidth() / downSamplingFactor));
        screenHeight = (int)Math.max(1, (inColor.getImage().getHeight() / downSamplingFactor));
        if (glowMode != BloomFilter.GlowMode.Scene) {
            GeometryQueue queue = resources.acquireOrElse(geometry, null);
            if (queue != null) {
                // render glow geometry
                colorDef.setSize(screenWidth, screenHeight);
                depthDef.setSize(colorDef);
                context.registerMode(RenderMode.cameraSize(screenWidth, screenHeight));
                FrameBuffer fb = getFrameBuffer("objectGlowRender", screenWidth, screenHeight, 1);
                glowMap = resources.acquireColorTarget(fb, geometryResult);
                resources.acquireDepthTarget(fb, geometryDepth);
                context.registerMode(RenderMode.frameBuffer(fb));
                context.clearBuffers();
                context.registerMode(RenderMode.background(ColorRGBA.BlackNoAlpha));
                context.registerMode(RenderMode.forcedTechnique("Glow"));
                queue.render(context, GeometryRenderHandler.DEFAULT);
                context.popActiveModes();
                resources.release(geometryResult, geometryDepth);
            } else {
                // use existing glow map
                glowMap = resources.acquire(objectGlow);
            }
        }
        super.execute(context);
    }

    /**
     * returns the bloom intensity
     * @return the intensity value
     */
    public float getBloomIntensity() {
        return bloomIntensity;
    }

    /**
     * intensity of the bloom effect default is 2.0
     *
     * @param bloomIntensity the desired intensity (default=2)
     */
    public void setBloomIntensity(float bloomIntensity) {
        this.bloomIntensity = bloomIntensity;
    }

    /**
     * returns the blur scale
     * @return the blur scale
     */
    public float getBlurScale() {
        return blurScale;
    }

    /**
     * sets The spread of the bloom default is 1.5f
     *
     * @param blurScale the desired scale (default=1.5)
     */
    public void setBlurScale(float blurScale) {
        this.blurScale = blurScale;
    }

    /**
     * returns the exposure cutoff<br>
     * for more details see {@link #setExposureCutOff(float exposureCutOff)}
     * @return the exposure cutoff
     */    
    public float getExposureCutOff() {
        return exposureCutOff;
    }

    /**
     * Define the color threshold on which the bloom will be applied (0.0 to 1.0)
     *
     * @param exposureCutOff the desired threshold (&ge;0, &le;1, default=0)
     */
    public void setExposureCutOff(float exposureCutOff) {
        this.exposureCutOff = exposureCutOff;
    }

    /**
     * returns the exposure power<br>
     * for more details see {@link #setExposurePower(float exposurePower)}
     * @return the exposure power
     */
    public float getExposurePower() {
        return exposurePower;
    }

    /**
     * defines how many times the bloom extracted color will be multiplied by itself. default is 5.0<br>
     * a high value will reduce rough edges in the bloom and somehow the range of the bloom area
     *
     * @param exposurePower the desired exponent (default=5)
     */
    public void setExposurePower(float exposurePower) {
        this.exposurePower = exposurePower;
    }

    /**
     * returns the downSampling factor<br>
     * for more details see {@link #setDownSamplingFactor(float downSamplingFactor)}
     * @return the downsampling factor
     */
    public float getDownSamplingFactor() {
        return downSamplingFactor;
    }

    /**
     * Sets the downSampling factor : the size of the computed texture will be divided by this factor. default is 1 for no downsampling
     * A 2 value is a good way of widening the blur
     *
     * @param downSamplingFactor the desired factor (default=1)
     */
    public void setDownSamplingFactor(float downSamplingFactor) {
        this.downSamplingFactor = downSamplingFactor;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(glowMode, "glowMode", BloomFilter.GlowMode.Scene);
        oc.write(blurScale, "blurScale", 1.5f);
        oc.write(exposurePower, "exposurePower", 5.0f);
        oc.write(exposureCutOff, "exposureCutOff", 0.0f);
        oc.write(bloomIntensity, "bloomIntensity", 2.0f);
        oc.write(downSamplingFactor, "downSamplingFactor", 1);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        glowMode = ic.readEnum("glowMode", BloomFilter.GlowMode.class, BloomFilter.GlowMode.Scene);
        blurScale = ic.readFloat("blurScale", 1.5f);
        exposurePower = ic.readFloat("exposurePower", 5.0f);
        exposureCutOff = ic.readFloat("exposureCutOff", 0.0f);
        bloomIntensity = ic.readFloat("bloomIntensity", 2.0f);
        downSamplingFactor = ic.readFloat("downSamplingFactor", 1);
    }
    
}
