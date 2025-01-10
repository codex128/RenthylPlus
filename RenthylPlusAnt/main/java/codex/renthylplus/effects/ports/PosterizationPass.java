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

import codex.renthyl.FrameGraph;
import codex.renthylplus.effects.JmeFilterPass;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import java.io.IOException;

/**
 *
 * @author codex
 */
public class PosterizationPass extends JmeFilterPass {
    
    private Material material;
    private int numColors = 8;
    private float gamma = 0.6f;
    private float strength = 1.0f;

    /**
     * Creates a posterization Filter
     */
    public PosterizationPass() {}

    /**
     * Creates a posterization Filter with the given number of colors
     *
     * @param numColors the desired number of colors (&gt;0, default=8)
     */
    public PosterizationPass(int numColors) {
        this.numColors = numColors;
    }

    /**
     * Creates a posterization Filter with the given number of colors and gamma
     *
     * @param numColors the desired number of colors (&gt;0, default=8)
     * @param gamma the desired exponent (default=0.6)
     */
    public PosterizationPass(int numColors, float gamma) {
        this(numColors);
        this.gamma = gamma;
    }
    
    @Override
    protected void init(FrameGraph frameGraph) {
        material = new Material(frameGraph.getAssetManager(), "Common/MatDefs/Post/Posterization.j3md");
        material.setInt("NumColors", numColors);
        material.setFloat("Gamma", gamma);
        material.setFloat("Strength", strength);
        add(new Subpass(material));
    }

    /**
     * Sets number of color levels used to draw the screen
     * 
     * @param numColors the desired number of colors (&gt;0, default=8)
     */
    public void setNumColors(int numColors) {
        this.numColors = numColors;
        if (material != null) {
            material.setInt("NumColors", numColors);
        }
    }

    /**
     * Sets gamma level used to enhance visual quality
     * 
     * @param gamma the desired exponent (default=0.6)
     */
    public void setGamma(float gamma) {
        this.gamma = gamma;
        if (material != null) {
            material.setFloat("Gamma", gamma);
        }
    }

    /**
     * Sets current strength value, i.e. influence on final image
     *
     * @param strength the desired influence factor (default=1)
     */
    public void setStrength(float strength) {
        this.strength = strength;
        if (material != null) {
            material.setFloat("Strength", strength);
        }
    }

    /**
     * Returns number of color levels used
     *
     * @return the count (&gt;0)
     */
    public int getNumColors() {
        return numColors;
    }

    /**
     * Returns current gamma value
     *
     * @return the exponent
     */
    public float getGamma() {
        return gamma;
    }

    /**
     * Returns current strength value, i.e. influence on final image
     *
     * @return the influence factor
     */
    public float getStrength() {
        return strength;
    }

    /**
     * Load properties when the filter is de-serialized, for example when
     * loading from a J3O file.
     *
     * @param importer the importer to use (not null)
     * @throws IOException from the importer
     */
    @Override
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);
        InputCapsule capsule = importer.getCapsule(this);

        this.gamma = capsule.readFloat("gamma", 0.6f);
        this.numColors = capsule.readInt("numColors", 8);
        this.strength = capsule.readFloat("strength", 1f);
    }

    /**
     * Save properties when the filter is serialized, for example when saving to
     * a J3O file.
     *
     * @param exporter the exporter to use (not null)
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter exporter) throws IOException {
        super.write(exporter);
        OutputCapsule capsule = exporter.getCapsule(this);

        capsule.write(gamma, "gamma", 0.6f);
        capsule.write(numColors, "numColors", 8);
        capsule.write(strength, "strength", 1f);
    }
    
}
