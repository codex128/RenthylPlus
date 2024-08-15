/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import codex.renthyl.FrameGraph;
import codex.renthyl.client.GraphTarget;
import com.jme3.material.Material;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.SceneGraphIterator;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.shader.VarType;

/**
 *
 * @author gary
 */
public class ShadowMapViewer extends AbstractControl implements GraphTarget<ShadowMap>{

    private final String name;
    private final VarType type;
    private ViewPort[] viewPorts;
    private Material material;
    private ShadowMap value;
    
    public ShadowMapViewer(String name, VarType type) {
        this.name = name;
        this.type = type;
    }
    
    @Override
    protected void controlUpdate(float tpf) {}
    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}
    @Override
    public void setSpatial(Spatial spat) {
        if (spatial == spat) {
            return;
        }
        super.setSpatial(spat);
        if (spatial != null) {
            for (Spatial s : new SceneGraphIterator(spatial)) {
                if (s instanceof Geometry) {
                    material = ((Geometry)s).getMaterial();
                    break;
                }
            }
        } else {
            material = null;
        }
    }
    @Override
    public boolean setGraphValue(FrameGraph frameGraph, ViewPort viewPort, ShadowMap value) {
        if (containsViewPort(viewPort)) {
            this.value = value;
            if (this.value != null) {
                material.setParam(name, type, this.value.getMap());
            } else {
                material.clearParam(name);
            }
            return true;
        }
        return false;
    }
    
    private boolean containsViewPort(ViewPort vp) {
        if (viewPorts == null) return true;
        for (ViewPort p : viewPorts) {
            if (p == vp) return true;
        }
        return false;
    }
    
    /**
     * Registers the ViewPorts that are able to affect the internal value.
     * <p>
     * ViewPorts not included in the array cannot affect the internal value.
     * 
     * @param viewPorts 
     */
    public void setViewPorts(ViewPort... viewPorts) {
        this.viewPorts = viewPorts;
    }
    /**
     * Sets the ViewPort filter to allow ViewPorts to affect the internal value.
     */
    public void includeAllViewPorts() {
        viewPorts = null;
    }
    
    /**
     * Gets the internal value.
     * 
     * @return 
     */
    public ShadowMap getValue() {
        return value;
    }
    /**
     * Gets the array of ViewPorts that are able to affect the internal value.
     * 
     * @return array of ViewPorts, or null if all ViewPorts are accepted
     */
    public ViewPort[] getViewPorts() {
        return viewPorts;
    }
    
}
