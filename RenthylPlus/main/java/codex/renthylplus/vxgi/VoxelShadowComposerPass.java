/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.vxgi;

import codex.jmecompute.ArgType;
import codex.jmecompute.UniversalShaderLoader;
import codex.jmecompute.WorkSize;
import codex.jmecompute.opengl.GLComputeShader;
import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.tickets.ArbitraryTicketList;
import codex.renthyl.resources.tickets.ResourceTicket;
import codex.renthylplus.shadow.ShadowMap;
import com.jme3.bounding.BoundingBox;
import com.jme3.light.Light;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderContext;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture3D;
import com.jme3.texture.TextureImage;
import java.util.Iterator;
import java.util.LinkedList;

/**
 *
 * @author codex
 */
public class VoxelShadowComposerPass extends RenderPass {
    
    public static final int MAX_SHADOW_LIGHTS = 32;
    
    private ResourceTicket<Integer> gridSize;
    private ResourceTicket<BoundingBox> voxelBounds;
    private ResourceTicket<Texture3D> voxelLight;
    private ResourceTicket<Light[]> lightShadowIndices;
    private ArbitraryTicketList<ShadowMap> shadowMaps;
    private final TextureDef<Texture3D> lightDef = TextureDef.texture3D(Image.Format.RGBA32F);
    private final LinkedList<ShadowMap> shadowMapList = new LinkedList<>();
    private final Vector3f gridMin = new Vector3f();
    private final Vector3f gridMax = new Vector3f();
    private final WorkSize work = new WorkSize();
    private TextureImage lightImg;
    private GLComputeShader shader;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        gridSize = addInput("GridSize");
        voxelBounds = addInput("Bounds");
        shadowMaps = addInputGroup(new ArbitraryTicketList<>("ShadowMaps"));
        voxelLight = addOutput("LightContribution");
        lightShadowIndices = addOutput("LightShadowIndices");
        lightDef.setMagFilter(Texture.MagFilter.Nearest);
        lightDef.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        //lightDef.setAccess(Image.Access.ReadWrite);
        shader = UniversalShaderLoader.loadOpenGLCompute(frameGraph.getAssetManager(),
                "RenthylPlus/MatDefs/VXGI/voxelShadowComposer.glsl");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(lightDef, voxelLight);
        declarePrimitive(lightShadowIndices);
        reference(gridSize, voxelBounds);
        reference(shadowMaps);
    }
    @Override
    protected void execute(FGRenderContext context) {
        
        int n = resources.acquire(gridSize);
        lightDef.setCube(n);
        
        acquireList(shadowMaps, shadowMapList);
        if (shadowMapList.isEmpty()) {
            throw new NullPointerException("No shadow maps provided.");
        }
        Light[] lightMap = new Light[Math.min(shadowMaps.size(), MAX_SHADOW_LIGHTS)];
        int nextLightIndex = 0;
        for (Iterator<ShadowMap> it = shadowMapList.iterator(); it.hasNext();) {
            ShadowMap m = it.next();
            if (indexOf(lightMap, m.getLight()) < 0) {
                if (nextLightIndex < MAX_SHADOW_LIGHTS) {
                    lightMap[nextLightIndex++] = m.getLight();
                } else {
                    it.remove();
                }
            }
        }
        
        Texture[] images = new Texture[shadowMaps.size()];
        Matrix4f[] matrices = new Matrix4f[shadowMaps.size()];
        Vector2f[] ranges = new Vector2f[shadowMaps.size()];
        int[] indices = new int[shadowMaps.size()];
        int[] types = new int[shadowMaps.size()];
        int i = 0;
        for (ShadowMap m : shadowMapList) {
            images[i] = m.getMap();
            matrices[i] = m.getProjection();
            ranges[i] = m.getInverseRange(null);
            indices[i] = indexOf(lightMap, m.getLight());
            types[i] = m.getLight().getType().getId();
            i++;
        }
        
        BoundingBox bound = resources.acquire(voxelBounds);
        bound.getMin(gridMin);
        bound.getMax(gridMax);
        
        if (lightImg == null) {
            lightImg = new TextureImage(resources.acquire(voxelLight), TextureImage.Access.ReadWrite);
        } else {
            lightImg.setTexture(resources.acquire(voxelLight));
        }
        
        shader.setDefine("NUM_SHADOW_MAPS", images.length);
        shader.set("ShadowMaps", ArgType.TextureArray, images);
        shader.set("LightMatrices", ArgType.Matrix4Array, matrices);
        shader.set("InverseRanges", ArgType.Vector2Array, ranges);
        shader.set("LightIndices", ArgType.IntArray, indices);
        shader.set("LightTypes", ArgType.IntArray, types);
        shader.set("VoxelLightMap", ArgType.Image, lightImg);
        shader.set("GridMin", ArgType.Vector3, gridMin);
        shader.set("GridMax", ArgType.Vector3, gridMax);
        shader.set("ShadowMap", ArgType.Texture, images[0]);
        
        int step = RenderContext.maxTextureUnits - 1;
        if (step <= 0) {
            throw new IllegalStateException("Hardware does not support binding more than one texture.");
        }
        for (int j = 0; j < images.length; j += step) {
            //shader.getUniform("ShadowMaps").setTextureArrayLimits(j, step);
            shader.set("CurrentBatch", ArgType.Int, j);
            shader.execute(work.setGlobal(n).setLocal(Math.min(step, images.length - j), 1, 1));
        }
        
        resources.setPrimitive(lightShadowIndices, lightMap);
        
    }
    @Override
    protected void reset(FGRenderContext context) {
        shadowMapList.clear();
    }
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
    private static int indexOf(Object[] array, Object obj) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == obj) {
                return i;
            }
            if (array[i] == null) {
                return -1;
            }
        }
        return -1;
    }
    
}
