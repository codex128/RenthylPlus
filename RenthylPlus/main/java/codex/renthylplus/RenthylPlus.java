/*
 * Copyright (c) 2024, codex
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package codex.renthylplus;

import codex.renthyl.FrameGraph;
import codex.renthyl.modules.Attribute;
import codex.renthyl.modules.OutputPass;
import codex.renthyl.modules.Junction;
import codex.renthyl.modules.geometry.SceneEnqueuePass;
import codex.renthyl.modules.geometry.QueueMergePass;
import codex.renthyl.modules.geometry.OutputGeometryPass;
import codex.renthylplus.deferred.DeferredGBufferPass;
import codex.renthylplus.deferred.DeferredPass;
import codex.renthylplus.light.LightImagePass;
import codex.renthyl.client.GraphSetting;
import codex.renthyl.light.TiledRenderGrid;
import codex.renthyl.modules.ControlRenderPass;
import codex.renthyl.modules.GroupJunction;
import codex.renthyl.modules.RenderThread;
import codex.renthyl.resources.tickets.TicketSelector;
import codex.renthylplus.shadow.PointShadowPass;
import codex.renthylplus.shadow.ShadowComposerPass;
import codex.renthylplus.shadow.ShadowQueuePass;
import codex.renthylplus.shadow.SpotShadowPass;
import com.jme3.asset.AssetManager;

/**
 * Utility class for constructing common {@link FrameGraph}s in code.
 * 
 * @author codex
 */
public class RenthylPlus {
    
    private RenthylPlus() {}
    
    /**
     * Constructs a deferred FrameGraph.
     * 
     * @param assetManager
     * @return deferred framegraph
     */
    public static FrameGraph deferred(AssetManager assetManager) {
        
        FrameGraph fg = new FrameGraph(assetManager);
        fg.setName("Deferred");
        
        GraphSetting<Integer> async = new GraphSetting<>("Async", 0);
        //fg.add(new TimeGuard(2, 3000));
        
        fg.add(new ControlRenderPass());
        SceneEnqueuePass enqueue = fg.add(SceneEnqueuePass.withLegacyQueues());
        QueueMergePass mergeForShadows = fg.add(new QueueMergePass());
        ShadowQueuePass shadowQueue = fg.add(new ShadowQueuePass());
        SpotShadowPass lightShadows = fg.add(new SpotShadowPass(1024));
        PointShadowPass lightShadows2 = fg.add(new PointShadowPass(1024));
        ShadowComposerPass shadowCompose = fg.add(new ShadowComposerPass());
        Attribute shadowDepthDebug = fg.add(new Attribute());
        Attribute lightContrDebug = fg.add(new Attribute());
        Attribute shadowOut = fg.add(new Attribute());
        Attribute tileInfoAttr = fg.add(new Attribute());
        Junction tileJunct1 = fg.add(new Junction());
        DeferredGBufferPass gbuf = fg.add(new DeferredGBufferPass());
        Junction gbufDebugTarget = fg.add(new Junction());
        Attribute gbufDebug = fg.add(new Attribute());
        LightImagePass lightImg = fg.add(new RenderThread(async)).add(new LightImagePass());
        GroupJunction lightJunct = fg.add(new GroupJunction(6));
        GroupJunction tileJunct2 = fg.add(new GroupJunction(2));
        DeferredPass deferred = fg.add(new DeferredPass());
        OutputPass defOut = fg.add(new OutputPass(0f));
        QueueMergePass merge = fg.add(new RenderThread(async)).add(new QueueMergePass());
        OutputGeometryPass geometry = fg.add(new OutputGeometryPass());
        
//        mergeForShadows.makeInput(enqueue, "Opaque", "Queues[0]");
//        mergeForShadows.makeInput(enqueue, "Sky", "Queues[1]");
//        mergeForShadows.makeInput(enqueue, "Transparent", "Queues[2]");
//        mergeForShadows.makeInput(enqueue, "Gui", "Queues[3]");
//        mergeForShadows.makeInput(enqueue, "Translucent", "Queues[4]");
        mergeForShadows.makeInput(enqueue, TicketSelector.All, TicketSelector.All);
        
        shadowQueue.makeInput(mergeForShadows, "Result", "Geometry");
        lightShadows.makeInput(shadowQueue, "Occluders", "Occluders");
        lightShadows2.makeInput(shadowQueue, "Occluders", "Occluders");
        shadowCompose.makeInput(shadowQueue, "Receivers", "Receivers");
        //shadowCompose.makeGroupInputToList(lightShadows, "ShadowMaps", "ShadowMaps");
        //shadowCompose.makeGroupInputToList(lightShadows2, "ShadowMaps", "ShadowMaps");
        shadowCompose.getInputGroup("ShadowMaps").makeInput(
                lightShadows.getOutputGroup("ShadowMaps"), TicketSelector.All, TicketSelector.All);
        shadowCompose.getInputGroup("ShadowMaps").makeInput(
                lightShadows2.getOutputGroup("ShadowMaps"), TicketSelector.All, TicketSelector.All);
        lightImg.makeInput(shadowCompose, "LightShadowIndices", "LightShadowIndices");
        deferred.makeInput(shadowCompose, "LightContribution", "LightContribution");
        shadowDepthDebug.getMainInputGroup().makeInput(
                lightShadows2.getOutputGroup("ShadowMaps"), TicketSelector.at(0), TicketSelector.All);
        lightContrDebug.makeInput(shadowCompose, "LightContribution", Attribute.INPUT);
        
        shadowDepthDebug.setName("ShadowDepthDebug");
        lightContrDebug.setName("LightContributionDebug");
        lightShadows.setLightSource(new GraphSetting<>("PointLightShadowCaster", null));
        lightShadows2.setLightSource(new GraphSetting<>("PointLightShadowCaster2", null));
        
        //shadowOut.makeInput(shadowCompose, "LightContribution", Attribute.INPUT);

        gbuf.makeInput(enqueue, "Opaque", "Geometry");
        
        gbufDebugTarget.setIndexSource(new GraphSetting("GBufferDebug", -1));
        //gbufDebugTarget.makeGroupInput(gbuf, "GBufferData", Junction.getInput());
        gbufDebugTarget.makeInput(gbuf.getOutputGroup("GBufferData"), TicketSelector.All, TicketSelector.All);
        
        gbufDebug.setName("GBufferDebug");
        gbufDebug.makeInput(gbufDebugTarget, Junction.OUTPUT, Attribute.INPUT);
        
        GraphSetting<TiledRenderGrid> tileInfo = new GraphSetting<>("TileInfo", new TiledRenderGrid());
        tileInfoAttr.setSource(tileInfo);
        
        GraphSetting<Integer> tileToggle = new GraphSetting("UseLightTiling", -1);
        //tileJunct1.makeInput(tileInfoAttr, Attribute.OUTPUT, Junction.getInput(0));
        tileJunct1.makeInput(tileInfoAttr, TicketSelector.name(Attribute.OUTPUT), TicketSelector.All);
        tileJunct1.setIndexSource(tileToggle);
        
        lightImg.makeInput(enqueue, "OpaqueLights", "Lights");
        lightImg.makeInput(tileJunct1, Junction.OUTPUT, "TileInfo");
        
        GraphSetting<Integer> lightPackMethod = new GraphSetting("UseLightTextures", -1);
        //lightJunct.makeGroupInput(lightImg, "Textures", Junction.getInput(0), 0, 0, 3);
        lightJunct.makeInput(lightImg.getOutputGroup("Textures"),
                TicketSelector.All, TicketSelector.before(3), 0);
        lightJunct.makeInput(lightImg.getMainOutputGroup(),
                TicketSelector.anyName("NumLights", "Ambient", "Probes"), TicketSelector.atOrAfter(3), 0);
        //lightJunct.makeInput(lightImg, "Ambient", Junction.getInput(0, 4));
        //lightJunct.makeInput(lightImg, "Probes", Junction.getInput(0, 5));
        lightJunct.setIndexSource(lightPackMethod);
        
        //tileJunct2.makeGroupInput(lightImg, "TileTextures", Junction.getInput(0));
        tileJunct2.makeInput(lightImg.getOutputGroup("TileTextures"),
                TicketSelector.All, TicketSelector.All, 0);
        tileJunct2.setIndexSource(tileToggle);
        
        //deferred.makeGroupInput(gbuf, "GBufferData", "GBufferData");
        deferred.getInputGroup("GBufferData").makeInput(gbuf.getOutputGroup("GBufferData"),
                TicketSelector.All, TicketSelector.All);
        deferred.makeInput(enqueue, "OpaqueLights", "Lights");
        //deferred.makeGroupInput(lightJunct, Junction.getOutput(), "LightTextures", 0, 0, 3);
        deferred.getInputGroup("LightTextures").makeInput(lightJunct.getOutputGroup(),
                TicketSelector.before(3), TicketSelector.All);
        deferred.makeInput(lightJunct.getOutputGroup(),
                TicketSelector.atOrAfter(3), TicketSelector.anyName("NumLights", "Ambient", "Probes"));
        //deferred.makeInput(lightJunct, Junction.getOutput(3), "NumLights");
        //deferred.makeInput(lightJunct, Junction.getOutput(4), "Ambient");
        //deferred.makeInput(lightJunct, Junction.getOutput(5), "Probes");
        deferred.getInputGroup("TileTextures").makeInput(tileJunct2.getOutputGroup(),
                TicketSelector.All, TicketSelector.All);
        //deferred.makeGroupInput(tileJunct2, Junction.getOutput(), "TileTextures");
        
        defOut.makeInput(deferred, "Color", "Color");
        defOut.makeInput(gbuf, "GBufferData[4]", "Depth");
        
//        merge.makeInput(enqueue, "Sky", "Queues[0]");
//        merge.makeInput(enqueue, "Transparent", "Queues[1]");
//        merge.makeInput(enqueue, "Gui", "Queues[2]");
//        merge.makeInput(enqueue, "Translucent", "Queues[3]");
        merge.makeInput(enqueue, TicketSelector.anyName("Sky", "Transparent", "Gui", "Translucent"), TicketSelector.All);
        
        geometry.makeInput(merge, "Result", "Geometry");
        
        return fg;
        
    }
    
    /**
     * Utility class for creating test FrameGraphs.
     */
    public static class Test {
        
        private Test() {}
        
        public static FrameGraph testGBuffer(AssetManager assetManager) {
            
            FrameGraph fg = new FrameGraph(assetManager);
            
            fg.add(new ControlRenderPass());
            SceneEnqueuePass enqueue = fg.add(SceneEnqueuePass.withLegacyQueues());
            QueueMergePass merge = fg.add(new QueueMergePass());
            DeferredGBufferPass gbuf = fg.add(new DeferredGBufferPass());
            Junction junct = fg.add(new Junction());
            OutputPass out = fg.add(new OutputPass());
            
            merge.makeInput(enqueue, TicketSelector.All, TicketSelector.All);
            
            gbuf.makeInput(merge, "Result", "Geometry");
            
            junct.setIndexSource(new GraphSetting("GBuffer", 0));
            //junct.makeGroupInput(gbuf, "GBufferData", Junction.getInput());
            junct.makeInput(gbuf.getOutputGroup("GBufferData"), TicketSelector.All, TicketSelector.All);
            
            out.makeInput(junct, Junction.OUTPUT, "Color");
            
            return fg;
            
        }
        
    }
    
}
