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
import codex.renthylplus.effects.JmeFilterPass;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.post.filters.SoftBloomFilter;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author codex
 */
public class SoftBloomPass extends JmeFilterPass {
    
    private static final Logger logger = Logger.getLogger(SoftBloomFilter.class.getName());
    
    private Subpass[] downsamplingPasses, upsamplingPasses;
    private final Image.Format format = Image.Format.RGBA16F;
    private int numSamplingPasses = 5;
    private float glowFactor = 0.05f;
    private boolean bilinearFiltering = true;
    private boolean updateFlag = true;
    
    /**
     * Creates filter with default settings.
     */
    public SoftBloomPass() {}
    
    @Override
    protected void init(FrameGraph frameGraph) {}
    @Override
    protected void prepare(FGRenderContext context) {
        if (updateFlag | capPassesToSize(context.getWidth(), context.getHeight())) {
            updateFilterChain(context.getWidth(), context.getHeight());
        }
        super.prepare(context);
    }
    
    private void updateFilterChain(int w, int h) {
        
        clearSubpasses();
        downsamplingPasses = new Subpass[numSamplingPasses];
        upsamplingPasses = new Subpass[numSamplingPasses];
        
        // downsampling passes
        Material downsampleMat = new Material(frameGraph.getAssetManager(), "Common/MatDefs/Post/Downsample.j3md");
        for (int i = 0; i < downsamplingPasses.length; i++) {
            Vector2f texelSize = new Vector2f(1f/w, 1f/h);
            w = w >> 1;
            h = h >> 1;
            Subpass prev = (i > 0 ? downsamplingPasses[i-1] : null);
            downsamplingPasses[i] = add(new SamplingPass(downsampleMat, i == 0, w, h) {
                @Override
                public void beforeAcquire(FGRenderContext context) {
                    getDef().setFormat(format);
                    getDef().setSize(width, height);
                    if (bilinearFiltering) {
                        getDef().setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
                    } else {
                        getDef().setMinFilter(Texture.MinFilter.NearestNoMipMaps);
                    }
                }
                @Override
                public void beforeRender(FGRenderContext context) {
                    if (prev != null) {
                        downsampleMat.setTexture("Texture", prev.getRenderedTexture());
                    }
                    downsampleMat.setVector2("TexelSize", texelSize);
                }
            });
        }
        
        // upsampling passes
        Material upsampleMat = new Material(frameGraph.getAssetManager(), "Common/MatDefs/Post/Upsample.j3md");
        for (int i = 0; i < upsamplingPasses.length; i++) {
            Vector2f texelSize = new Vector2f(1f/w, 1f/h);
            w = w << 1; h = h << 1;
            Subpass prev;
            if (i == 0) {
                prev = downsamplingPasses[downsamplingPasses.length-1];
            } else {
                prev = upsamplingPasses[i-1];
            }
            upsamplingPasses[i] = add(new SamplingPass(upsampleMat, false, w, h) {
                @Override
                public void beforeAcquire(FGRenderContext context) {
                    getDef().setFormat(format);
                    getDef().setSize(width, height);
                    if (bilinearFiltering) {
                        getDef().setMagFilter(Texture.MagFilter.Bilinear);
                    } else {
                        getDef().setMagFilter(Texture.MagFilter.Nearest);
                    }
                }
                @Override
                public void beforeRender(FGRenderContext context) {
                    upsampleMat.setTexture("Texture", prev.getRenderedTexture());
                    upsampleMat.setVector2("TexelSize", texelSize);
                }
            });
        }
        
        // final pass
        Material finalMat = new Material(frameGraph.getAssetManager(), "Common/MatDefs/Post/SoftBloomFinal.j3md");
        add(new Subpass(finalMat, true, false) {
            @Override
            public void beforeRender(FGRenderContext context) {
                finalMat.setTexture("GlowMap", upsamplingPasses[upsamplingPasses.length-1].getRenderedTexture());
                finalMat.setFloat("GlowFactor", glowFactor);
            }
        });
        
        updateFlag = false;
        
    }
    private boolean capPassesToSize(int w, int h) {
        int limit = Math.min(w, h);
        for (int i = 0; i < numSamplingPasses; i++) {
            limit = limit >> 1;
            if (limit <= 2) {
                if (numSamplingPasses != i) {
                    logger.log(Level.INFO, "Number of sampling passes capped at {0} due to texture size.", i);
                    numSamplingPasses = i;
                    return true;
                } else break;
            }
        }
        return false;
    }
    
    /**
     * Sets the number of sampling passes in each step.
     * <p>
     * Higher values produce more glow with higher resolution, at the cost
     * of more passes. Lower values produce less glow with lower resolution.
     * <p>
     * The total number of passes is {@code 2n+1}: n passes for downsampling
     * (13 texture reads per pass per fragment), n passes for upsampling and blur
     * (9 texture reads per pass per fragment), and 1 pass for blending (2 texture reads
     * per fragment). Though, it should be noted that for each downsampling pass the
     * number of fragments decreases by 75%, and for each upsampling pass, the number
     * of fragments quadruples (which restores the number of fragments to the original
     * resolution).
     * <p>
     * Setting this forces subpasses to be reconfigured.
     * <p>
     * default=5
     * 
     * @param numSamplingPasses The number of passes per donwsampling/upsampling step. Must be greater than zero.
     * @throws IllegalArgumentException if argument is less than or equal to zero
     */
    public void setNumSamplingPasses(int numSamplingPasses) {
        if (numSamplingPasses <= 0) {
            throw new IllegalArgumentException("Number of sampling passes must be greater "
                    + "than zero (found: " + numSamplingPasses + ").");
        }
        if (this.numSamplingPasses != numSamplingPasses) {
            this.numSamplingPasses = numSamplingPasses;
            updateFlag = true;
        }
    }
    
    /**
     * Sets the factor at which the glow result texture is merged with
     * the scene texture.
     * <p>
     * Low values favor the scene texture more, while high values make
     * glow more noticeable. This value is clamped between 0 and 1.
     * <p>
     * default=0.05f
     * 
     * @param factor 
     */
    public void setGlowFactor(float factor) {
        this.glowFactor = FastMath.clamp(factor, 0, 1);
    }
    
    /**
     * Sets pass textures to use bilinear filtering.
     * <p>
     * If true, downsampling textures are set to {@code min=BilinearNoMipMaps} and
     * upsampling textures are set to {@code mag=Bilinear}, which produces better
     * quality glow. If false, textures use their default filters.
     * <p>
     * default=true
     * 
     * @param bilinearFiltering true to use bilinear filtering
     */
    public void setBilinearFiltering(boolean bilinearFiltering) {
        this.bilinearFiltering = bilinearFiltering;
    }
    
    /**
     * Gets the number of downsampling/upsampling passes per step.
     * 
     * @return number of downsampling/upsampling passes
     * @see #setNumSamplingPasses(int)
     */
    public int getNumSamplingPasses() {
        return numSamplingPasses;
    }
    
    /**
     * Gets the glow factor.
     * 
     * @return glow factor
     * @see #setGlowFactor(float)
     */
    public float getGlowFactor() {
        return glowFactor;
    }
    
    /**
     * Returns true if pass textures use bilinear filtering.
     * 
     * @return 
     * @see #setBilinearFiltering(boolean)
     */
    public boolean isBilinearFiltering() {
        return bilinearFiltering;
    }
    
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(numSamplingPasses, "numSamplingPasses", 5);
        oc.write(glowFactor, "glowFactor", 0.05f);
        oc.write(bilinearFiltering, "bilinearFiltering", true);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        numSamplingPasses = ic.readInt("numSamplingPasses", 5);
        glowFactor = ic.readFloat("glowFactor", 0.05f);
        bilinearFiltering = ic.readBoolean("bilinearFiltering", true);
    }
    
    private class SamplingPass extends JmeFilterPass.Subpass {
        
        public final int width, height;
        
        public SamplingPass(Material material, boolean useColor, int width, int height) {
            super(material, useColor, false);
            this.width = width;
            this.height = height;
        }
        
    }
    
}
