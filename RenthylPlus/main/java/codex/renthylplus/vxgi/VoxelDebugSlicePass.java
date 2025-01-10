/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.vxgi;

import codex.boost.material.ImmediateMatDef;
import codex.boost.material.ImmediateShader;
import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.draw.RenderMode;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.tickets.ResourceTicket;
import com.jme3.material.Material;
import com.jme3.shader.Shader;
import com.jme3.shader.VarType;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture3D;

/**
 *
 * @author codex
 */
public class VoxelDebugSlicePass extends RenderPass {
    
    private static ImmediateMatDef matdef;
    
    private ResourceTicket<Texture3D> voxels;
    private ResourceTicket<Texture2D> result;
    private final TextureDef<Texture2D> resultDef = TextureDef.texture2D();
    private final TextureDef<Texture3D> voxelDef = TextureDef.texture3D(Image.Format.RGBA8);
    private Material material;
    private float slice = 0;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        voxels = addInput("Voxels");
        result = addOutput("Result");
        if (matdef == null) {
            ImmediateShader frag = new ImmediateShader(Shader.ShaderType.Fragment, true)
                    .includeGlslCompat()
                    .uniform("sampler3D", "m_VoxelMap")
                    .uniform("float", "m_Slice")
                    .varying("vec2", "texCoord")
                    .main()
                        //.assign("ivec3", "size", "imageSize(m_VoxelMap)")
                        .assign("gl_FragColor", "texture3D(m_VoxelMap, vec3(texCoord, m_Slice))")
                        //.assign("gl_FragColor", "imageLoad(m_VoxelMap, ivec3(size * vec3(texCoord, m_Slice)) + size)")
                        //.assign("gl_FragColor", "vec4(texCoord, m_Slice, 1.0)")
                    .end();
            matdef = new ImmediateMatDef(frameGraph.getAssetManager(), "VoxelSliceVis")
                    .addParam(VarType.Texture3D, "VoxelMap")
                    .addParam(VarType.Float, "Slice", 0f);
            matdef.createTechnique()
                    .setVersions(450, 430)
                    .setVertexShader("RenthylCore/MatDefs/Fullscreen/Screen.vert")
                    .setShader(frag)
                    .add();
        }
        material = matdef.createMaterial();
        voxelDef.setCube(64);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(resultDef, result);
        reserve(result);
        reference(voxels);
    }
    @Override
    protected void execute(FGRenderContext context) {
        resultDef.setSize(context.getWidth(), context.getHeight());
        FrameBuffer fb = getFrameBuffer(context.getWidth(), context.getHeight(), 1);
        resources.acquireColorTarget(fb, result);
        context.registerMode(RenderMode.frameBuffer(fb));
        context.clearBuffers();
        material.setTexture("VoxelMap", resources.acquire(voxels));
        material.setFloat("Slice", slice);
        context.renderFullscreen(material);
        slice += context.getTpf();
        if (slice >= 1.0) {
            slice = 0;
        }
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
}
