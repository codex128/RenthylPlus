/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import codex.renthyl.FrameGraph;
import codex.renthyl.client.GraphSource;
import codex.renthyl.modules.RenderContainer;
import codex.renthyl.resources.tickets.DynamicTicketList;
import codex.renthyl.resources.tickets.TicketSelector;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;

/**
 *
 * @author codex
 */
public class ShadowManager extends RenderContainer<ShadowOcclusionPass> {
    
    private DynamicTicketList<ShadowMap> shadowMaps;
    
    @Override
    public void initializeModule(FrameGraph frameGraph) {
        super.initializeModule(frameGraph);
        addInput("Occluders");
        addInput("Receivers");
        shadowMaps = addOutputGroup(new DynamicTicketList<>("ShadowMaps"));
    }
    
    public DirectionalShadowPass addDirectionalLight(GraphSource<DirectionalLight> light, int size, int splits) {
        DirectionalShadowPass shadow = new DirectionalShadowPass(size, splits);
        shadow.setLightSource(light);
        return addShadows(shadow);
    }
    public PointShadowPass addPointLight(GraphSource<PointLight> light, int size) {
        PointShadowPass shadow = new PointShadowPass(size);
        shadow.setLightSource(light);
        return addShadows(shadow);
    }
    public SpotShadowPass addSpotLight(GraphSource<SpotLight> light, int size) {
        SpotShadowPass shadow = new SpotShadowPass(size);
        shadow.setLightSource(light);
        return addShadows(shadow);
    }
    public <T extends ShadowOcclusionPass> T addShadows(T pass) {
        shadowMaps.makeInput(add(pass).getOutputGroup("ShadowMaps"), TicketSelector.All, TicketSelector.All);
        pass.makeInput(getMainInputGroup(),
                TicketSelector.names("Occluders", "Receivers"),
                TicketSelector.and(TicketSelector.names("Occluders", "Receivers"), TicketSelector.NamesMatch));
        return pass;
    }
    
    public ShadowOcclusionPass removeLight(GraphSource<Light> light) {
        ShadowOcclusionPass remove = null;
        for (ShadowOcclusionPass p : queue) {
            if (p.getLightSource() == light) {
                remove = p;
                break;
            }
        }
        if (remove != null && remove(remove)) {
            return remove;
        }
        return null;
    }
    
}
