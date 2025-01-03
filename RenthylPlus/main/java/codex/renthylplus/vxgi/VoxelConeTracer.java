/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.vxgi;

import codex.renthyl.FrameGraph;
import codex.renthyl.client.GraphSource;
import codex.renthyl.modules.ModuleLocator;
import codex.renthyl.modules.NewConnectable;
import codex.renthyl.modules.RenderContainer;
import codex.renthyl.modules.RenderModule;
import codex.renthyl.modules.cache.CacheRead;
import codex.renthyl.modules.cache.CacheWrite;
import codex.renthyl.resources.tickets.ArbitraryTicketList;
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
    private static final String VOXEL_ENV_SETUP = "VoxelEnvironmentSetup";
    private static final String VOXEL_SHADOW_COMPOSER = "VoxelShadowComposer";
    
    private ArbitraryTicketList<ShadowMap> shadowMaps;
    private boolean ticketsCreated = false;
    
    public VoxelConeTracer() {
        
    }
    
    /**
     * Creates a VoxelConeTracer module with all necessary internal functions.
     * 
     * @return 
     */
    public static VoxelConeTracer create() {
        
        VoxelConeTracer vct = new VoxelConeTracer();
        vct.createTickets();
        
        CacheRead<Texture3D> voxelRead = vct.add(new CacheRead<>(Texture3D.class, voxelCacheKey));
        VoxelEnvSetupPass voxelEnv = vct.add(new VoxelEnvSetupPass());
        VoxelShadowComposerPass voxShadows = vct.add(new VoxelShadowComposerPass());
        DirectLightingPass direct = vct.add(new DirectLightingPass());
        VoxelizationPass voxels = vct.add(new VoxelizationPass());
        IndirectLightingPass indirect = vct.add(new IndirectLightingPass());
        CacheWrite voxelWrite = vct.add(new CacheWrite(voxelCacheKey));
        
        voxelEnv.setName(VOXEL_ENV_SETUP);
        voxShadows.setName(VOXEL_SHADOW_COMPOSER);
        
        voxShadows.makeInput(voxelEnv, "GridSize", "GridSize");
        voxShadows.makeInput(voxelEnv, "Bounds", "Bounds");
        
        vct.makeInternalInput("Geometry", "Geometry", direct);
        vct.makeInternalInput("Lights", "Lights", direct);
        vct.makeInternalInput("LightContribution", "LightContribution", direct);
        
        vct.makeInternalInput("Geometry", "Geometry", voxels);
        vct.makeInternalInput("Lights", "Lights", voxels);
        voxels.makeInput(voxShadows, "LightContribution", "LightContribution");
        voxels.makeInput(voxelEnv, "GridSize", "GridSize");
        voxels.makeInput(voxelEnv, "Bounds", "Bounds");
        voxels.makeInput(voxelRead, CacheRead.OUTPUT, "TemporalVoxels");
        
        indirect.makeInput(direct, "Color", "SceneColor");
        indirect.makeInput(direct, "Depth", "SceneDepth");
        indirect.getInputGroup("Material").makeInput(direct.getOutputGroup("Material"),
                TicketSelector.All, TicketSelector.All);
        indirect.makeInput(voxels, "Voxels", "Voxels");
        indirect.makeInput(voxelEnv, "Bounds", "Bounds");
        indirect.makeInput(voxelEnv, "GridSize", "GridSize");
        vct.makeInternalOutput(indirect, "Result", "Result");
        
        voxelWrite.makeInput(voxels, "Voxels", CacheWrite.INPUT);
        
        return vct;
        
    }
    
    @Override
    public void initModule(FrameGraph frameGraph) {
        createTickets();
        shadowMaps.registerTarget(get(ModuleLocator.by(VoxelShadowComposerPass.class, VOXEL_SHADOW_COMPOSER))
                .getInputGroup(ArbitraryTicketList.class, "ShadowMaps"));
        super.initModule(frameGraph);
    }
    
    protected void createTickets() {
        if (!ticketsCreated) {
            addInput("Geometry");
            addInput("Depth");
            addInput("Lights");
            addInput("LightContribution");
            shadowMaps = addInputGroup(new ArbitraryTicketList<>("ShadowMaps"));
            addOutput("Result");
        }
        ticketsCreated = false;
    }
    
    public void setVoxelGridSize(GraphSource<Integer> gridSize) {
        get(ModuleLocator.by(VoxelEnvSetupPass.class, VOXEL_ENV_SETUP)).setGridSize(gridSize);
    }
    public void setVoxelBounds(GraphSource<BoundingBox> bounds) {
        get(ModuleLocator.by(VoxelEnvSetupPass.class, VOXEL_ENV_SETUP)).setBounds(bounds);
    }
    
}
