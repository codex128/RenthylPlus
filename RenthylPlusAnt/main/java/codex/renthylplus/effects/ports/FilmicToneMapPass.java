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
import com.jme3.math.Vector3f;
import java.io.IOException;

/**
 *
 * @author codex
 */
public class FilmicToneMapPass extends JmeFilterPass {
    
    private static final Vector3f DEFAULT_WHITEPOINT = new Vector3f(11.2f, 11.2f, 11.2f);
    
    private Material material;
    private Vector3f whitePoint = DEFAULT_WHITEPOINT.clone();

    /**
     * Creates a tone-mapping filter with the default white-point of 11.2.
     */
    public FilmicToneMapPass() {}

    /**
     * Creates a tone-mapping filter with the specified white-point.
     * 
     * @param whitePoint The intensity of the brightest part of the scene. 
     */
    public FilmicToneMapPass(Vector3f whitePoint) {
        this.whitePoint = whitePoint.clone();
    }

    @Override
    protected void init(FrameGraph frameGraph) {
        material = new Material(frameGraph.getAssetManager(), "Common/MatDefs/Post/ToneMap.j3md");
        material.setVector3("WhitePoint", whitePoint);
        add(new Subpass(material, true, false));
    }

    /**
     * Set the scene white point.
     * 
     * @param whitePoint The intensity of the brightest part of the scene. 
     */
    public void setWhitePoint(Vector3f whitePoint) {
        if (material != null) {
            material.setVector3("WhitePoint", whitePoint);
        }
        this.whitePoint = whitePoint;
    }
    
    /**
     * Get the scene white point.
     * 
     * @return The intensity of the brightest part of the scene. 
     */
    public Vector3f getWhitePoint() {
        return whitePoint;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(whitePoint, "whitePoint", DEFAULT_WHITEPOINT.clone());
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        whitePoint = (Vector3f) ic.readSavable("whitePoint", DEFAULT_WHITEPOINT.clone());
    }
    
}
