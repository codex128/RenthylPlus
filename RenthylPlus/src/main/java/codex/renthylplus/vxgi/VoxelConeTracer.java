/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.vxgi;

import codex.renthyl.FrameGraph;
import codex.renthyl.client.GraphSource;
import codex.renthyl.modules.ModuleLocator;
import codex.renthyl.modules.RenderContainer;
import codex.renthyl.modules.RenderModule;
import codex.renthyl.modules.cache.CacheRead;
import codex.renthyl.modules.cache.CacheWrite;
import codex.renthyl.resources.tickets.DynamicTicketList;
import codex.renthyl.resources.tickets.TicketSelector;
import codex.renthylplus.shadow.ShadowMap;
import com.jme3.bounding.BoundingBox;
import com.jme3.texture.Texture3D;

/**
 *
 * @author codex
 */
public class VoxelConeTracer extends RenderContainer<RenderModule> {
    
    private static final GraphSource<String> voxelCacheKey =
            GraphSource.value(VoxelConeTracer.class.getName() + ":TemporalVoxels");
    
    private DynamicTicketList<ShadowMap> shadowMaps;
    
    public VoxelConeTracer() {
        
    }
    
    @Override
    public void initializeModule(FrameGraph frameGraph) {
        super.initializeModule(frameGraph);
        addInput("Geometry");
        addInput("Depth");
        addInput("Lights");
        addInput("LightContribution");
        shadowMaps = addInputGroup(new DynamicTicketList<>("ShadowMaps"));
        addOutput("Result");
    }
    
    /**
     * Setups this container will all necessary modules and connections.
     * 
     * @return 
     */
    public VoxelConeTracer create() {
        
        if (!isAssigned()) {
            throw new IllegalStateException();
        }
        
        CacheRead<Texture3D> voxelRead = add(new CacheRead<>(Texture3D.class, voxelCacheKey));
        VoxelEnvSetupPass voxelEnv = add(new VoxelEnvSetupPass());
        VoxelShadowComposerPass voxShadows = add(new VoxelShadowComposerPass());
        DirectLightingPass direct = add(new DirectLightingPass());
        VoxelizationPass voxels = add(new VoxelizationPass());
        IndirectLightingPass indirect = add(new IndirectLightingPass());
        VoxelVisualizerPass vis = add(new VoxelVisualizerPass());
        CacheWrite voxelWrite = add(new CacheWrite(voxelCacheKey));
        
        voxelEnv.setName("VoxelEnvironment");
        voxShadows.setName("VoxelShadowComposer");
        
        voxShadows.makeInput(voxelEnv, "GridSize", "GridSize");
        voxShadows.makeInput(voxelEnv, "Bounds", "Bounds");
        
        makeInternalInput("Geometry", "Geometry", direct);
        makeInternalInput("Lights", "Lights", direct);
        makeInternalInput("LightContribution", "LightContribution", direct);
        
        makeInternalInput("Geometry", "Geometry", voxels);
        makeInternalInput("Lights", "Lights", voxels);
        voxels.makeInput(voxShadows, "LightContribution", "LightContribution");
        voxels.makeInput(voxelEnv, "GridSize", "GridSize");
        voxels.makeInput(voxelEnv, "Bounds", "Bounds");
        voxels.makeInput(voxelRead, CacheRead.OUTPUT, "TemporalVoxels");
        
        indirect.makeInput(direct, "Color", "SceneColor");
        indirect.makeInput(direct, "Depth", "SceneDepth");
        indirect.getInputGroup("Material").makeInput(direct.getOutputGroup("Material"),
                TicketSelector.NamesMatch, TicketSelector.All);
        indirect.makeInput(voxels, "Voxels", "Voxels");
        indirect.makeInput(voxelEnv, "Bounds", "Bounds");
        indirect.makeInput(voxelEnv, "GridSize", "GridSize");
        
        vis.makeInput(voxels, "Voxels", "Voxels");
        //vis.makeInput(voxShadows, "LightContribution", "Voxels");
        vis.makeInput(getMainInputGroup(), TicketSelector.name("Geometry"), TicketSelector.NamesMatch);
        vis.makeInput(voxelEnv, "Bounds", "Bounds");
        
        voxelWrite.makeInput(voxels, "Voxels", CacheWrite.INPUT);
        shadowMaps.registerTargetList(voxShadows.getInputGroup(DynamicTicketList.class, "ShadowMaps"));
        
        getMainOutputGroup().makeInput(indirect.getMainOutputGroup(), "Result", "Result");
        //getMainOutputGroup().makeInput(vis.getMainOutputGroup(), "Color", "Result");
        
        return this;
        
    }
    
    public void setVoxelGridSize(GraphSource<Integer> gridSize) {
        get(ModuleLocator.by(VoxelEnvSetupPass.class, "VoxelEnvironment")).setGridSize(gridSize);
    }
    public void setVoxelBounds(GraphSource<BoundingBox> bounds) {
        get(ModuleLocator.by(VoxelEnvSetupPass.class, "VoxelEnvironment")).setBounds(bounds);
    }
    
}
