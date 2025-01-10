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
package codex.renthylplus.deferred;

import codex.boost.material.MaterialAdapter;
import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.GeometryQueue;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.draw.RenderMode;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.tickets.DefinedTicketArray;
import codex.renthyl.resources.tickets.ResourceTicket;
import codex.renthyl.resources.tickets.TicketSelector;
import codex.renthyl.util.GeometryRenderHandler;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.renderer.queue.NullComparator;
import java.util.LinkedList;

/**
 * Renders information about a queue of geometries to a set of textures.
 * <p>
 * Inputs:
 * <ul>
 *   <li>Geometry ({@link GeometryQueue}: queue of geometries to extract information from.</li>
 * </ul>
 * Outputs:
 * <ul>
 *   <li>GBufferData[5] ({@link Texture2D}): textures containing geometry information.</li>
 *   <li>NumRenders (int): number of geometries rendered, since not all geometries are guaranteed to be rendered.</li>
 * </ul>
 * Geometries that do not have a material with a "GBuffer" technique are not rendered.
 * 
 * @author codex
 */
public class DeferredGBufferPass extends RenderPass implements GeometryRenderHandler {
    
    private static final String GBUFFER_PASS = "GBufferPass";
    private static final MaterialAdapter adapter = new MaterialAdapter();
    
    static {
        String gbuffer = "RenthylPlus/MatDefs/GBuffer/";
        adapter.add("Common/MatDefs/Light/PBRLighting.j3md",          gbuffer + "PBRLighting.j3md");
        adapter.add("Common/MatDefs/Light/Lighting.j3md",             gbuffer + "Lighting.j3md");
        adapter.add("Common/MatDefs/Misc/Unshaded.j3md",              gbuffer + "Unshaded.j3md");
        adapter.add("Common/MatDefs/Terrain/Terrain.j3md",            gbuffer + "Terrain.j3md");
        adapter.add("Common/MatDefs/Terrain/PBRTerrain.j3md",         gbuffer + "PBRTerrain.j3md");
        adapter.add("Common/MatDefs/Terrain/AdvancedPBRTerrain.j3md", gbuffer + "AdvancedPBRTerrain.j3md");
        adapter.add("Common/MatDefs/Terrain/TerrainLighting.j3md",    gbuffer + "TerrainLighting.j3md");
    }
    
    private AssetManager assetManager;
    private ResourceTicket<GeometryQueue> geometry;
    private DefinedTicketArray<Texture2D, TextureDef<Texture2D>> gbuffers;
    private ResourceTicket<GeometryQueue> skipped;
    private final GeometryQueue skipQueue = new GeometryQueue(new NullComparator());
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        geometry = addInput("Geometry");
        gbuffers = addOutputGroup(new DefinedTicketArray<>("GBufferData",
                TextureDef.texture2D(Image.Format.RGBA16F),
                TextureDef.texture2D(Image.Format.RGBA16F),
                TextureDef.texture2D(Image.Format.RGBA16F),
                TextureDef.texture2D(Image.Format.RGBA32F),
                TextureDef.texture2D(Image.Format.Depth)));
        skipped = addOutput("SkippedGeometry");
        this.assetManager = frameGraph.getAssetManager();
    }
    @Override
    protected void prepare(FGRenderContext context) {
        int w = context.getWidth();
        int h = context.getHeight();
        // TODO: make group implementation to handle definitions as well
        for (int i = 0; i < gbuffers.size(); i++) {
            gbuffers.getDef(i).setSize(w, h);
            declare(gbuffers.getDef(i), gbuffers.get(i));
        }
        declare(null, skipped);
        reserve(gbuffers);
        reference(geometry);
    }
    @Override
    protected void execute(FGRenderContext context) {
        FrameBuffer fb = getFrameBuffer(context, 1);
        fb.setMultiTarget(true);
        resources.acquireColorTargets(fb, gbuffers.select(TicketSelector.before(4), new LinkedList()));
        resources.acquireDepthTarget(fb, gbuffers.get(4));
        context.registerMode(RenderMode.frameBuffer(fb));
        context.clearBuffers();
        context.registerMode(RenderMode.background(ColorRGBA.BlackNoAlpha));
        GeometryQueue queue = resources.acquire(geometry);
        queue.render(context, this);
        resources.setPrimitive(skipped, skipQueue);
    }
    @Override
    protected void reset(FGRenderContext context) {
        skipQueue.clear();
    }
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public void renderGeometry(FGRenderContext context, Geometry geom) {
        Material material = geom.getMaterial();
        if (!adapter.adaptMaterial(assetManager, material, GBUFFER_PASS)) {
            skipQueue.add(geom);
            return;
        }
        material.selectTechnique(GBUFFER_PASS, context.getRenderManager());
        context.getRenderManager().renderGeometry(geom);
    }
    
    public static void addMaterialAdaption(String matdef, String technique) {
        adapter.add(matdef, technique);
    }
    public static void adaptAllMaterials(AssetManager assetManager) {
        adapter.adaptAll(assetManager);
    }
    
}
