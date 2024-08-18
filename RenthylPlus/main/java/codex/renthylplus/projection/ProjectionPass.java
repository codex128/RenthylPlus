/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.projection;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.client.GraphSource;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.ResourceTicket;
import com.jme3.material.Material;
import com.jme3.renderer.Camera;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture2D;
import java.util.Objects;

/**
 *
 * @author gary
 */
public class ProjectionPass extends RenderPass {
    
    private ResourceTicket<Texture2D> sceneColor, sceneDepth, projDepth, normal, result;
    private final TextureDef<Texture2D> resultDef = TextureDef.texture2D();
    private GraphSource<Camera> camera;
    private GraphSource<Material> material;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        sceneColor = addInput("SceneColor");
        sceneDepth = addInput("SceneDepth");
        projDepth = addInput("ProjectorDepth");
        normal = addInput("Normal");
        result = addOutput("Result");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(resultDef, result);
        reserve(result);
        reference(sceneColor, sceneDepth);
        referenceOptional(projDepth, normal);
    }
    @Override
    protected void execute(FGRenderContext context) {
        
        Objects.requireNonNull(camera, "Projection camera source cannot be null.");
        Objects.requireNonNull(material, "Projection material source cannot be null.");
        Camera cam = camera.getGraphValue(frameGraph, context.getViewPort());
        Material mat = material.getGraphValue(frameGraph, context.getViewPort());
        Objects.requireNonNull(cam, "Projection camera cannot be null.");
        Objects.requireNonNull(mat, "Projection material cannot be null.");
        
        resultDef.setSize(context.getWidth(), context.getHeight());
        
        mat.setTexture("ColorMap", resources.acquire(sceneColor));
        mat.setTexture("DepthMap", resources.acquire(sceneDepth));
        if (mat.getMaterialDef().getMaterialParam("NormalMap") != null) {
            mat.setTexture("NormalMap", resources.acquireOrElse(normal, null));
        }
        mat.setMatrix4("ProjectorViewMatrix", cam.getViewProjectionMatrix());
        
        if ( /* do rendering */ true) {
            FrameBuffer fb = getFrameBuffer(context, 1);
            resources.acquireColorTarget(fb, result);
            context.getRenderer().setFrameBuffer(fb);
            context.getRenderer().clearBuffers(true, true, true);
            context.renderFullscreen(mat);
        } else {
            resources.merge(sceneColor, result, false);
        }
        
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
}
