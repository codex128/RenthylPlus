/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.vxgi;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.tickets.ResourceTicket;
import com.jme3.light.Light;
import com.jme3.scene.SceneGraphIterator;
import com.jme3.scene.Spatial;
import java.util.Collection;
import java.util.LinkedList;

/**
 *
 * @author codex
 */
public class LightGatherPass extends RenderPass {

    private ResourceTicket<Collection<Light>> lights;
    private final LinkedList<Light> lightList = new LinkedList<>();
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        lights = addOutput("Lights");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declarePrimitive(lights);
    }
    @Override
    protected void execute(FGRenderContext context) {
        for (Spatial scene : context.getViewPort().getScenes()) {
            for (Spatial spatial : new SceneGraphIterator(scene)) {
                for (Light l : spatial.getLocalLightList()) {
                    lightList.add(l);
                }
            }
        }
        resources.setPrimitive(lights, lightList);
    }
    @Override
    protected void reset(FGRenderContext context) {
        lightList.clear();
    }
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
}
