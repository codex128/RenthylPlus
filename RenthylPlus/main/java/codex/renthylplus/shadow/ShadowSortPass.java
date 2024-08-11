/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.GeometryQueue;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.ResourceTicket;
import codex.renthyl.util.SpatialWorldParam;
import com.jme3.renderer.queue.OpaqueComparator;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;

/**
 *
 * @author codex
 */
public class ShadowSortPass extends RenderPass {

    private ResourceTicket<GeometryQueue> geometry, occluders, receivers;
    private GeometryQueue occluderQueue, receiverQueue;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        geometry = addInput("Geometry");
        occluders = addOutput("Occluders");
        receivers = addOutput("Receivers");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(null, occluders);
        declare(null, receivers);
        reference(geometry);
    }
    @Override
    protected void execute(FGRenderContext context) {
        GeometryQueue source = resources.acquire(geometry);
        int numGeoms = source.getNumGeometries();
        if (occluderQueue == null || occluderQueue.getAllocatedSpace() < numGeoms) {
            occluderQueue = new GeometryQueue(new OpaqueComparator(), numGeoms);
        }
        if (receiverQueue == null || receiverQueue.getAllocatedSpace() < numGeoms) {
            receiverQueue = new GeometryQueue(new OpaqueComparator(), numGeoms);
        }
        for (Geometry g : source) {
            RenderQueue.ShadowMode mode = g.getUserData(SpatialWorldParam.Shadow.RESULT);
            if (mode != null) {
                boolean all = mode == RenderQueue.ShadowMode.CastAndReceive;
                if (all || mode == RenderQueue.ShadowMode.Cast) {
                    occluderQueue.add(g);
                }
                if (all || mode == RenderQueue.ShadowMode.Receive) {
                    receiverQueue.add(g);
                }
            }
        }
        resources.setPrimitive(occluders, occluderQueue);
        resources.setPrimitive(receivers, receiverQueue);
    }
    @Override
    protected void reset(FGRenderContext context) {
        occluderQueue.clear();
        receiverQueue.clear();
    }
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
}
