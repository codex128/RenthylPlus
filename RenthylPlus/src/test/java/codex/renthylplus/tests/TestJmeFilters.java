/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.tests;

import codex.boost.material.ImmediateMatDef;
import codex.boost.material.ImmediateShader;
import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.GeometryQueue;
import codex.renthyl.Renthyl;
import codex.renthyl.client.GraphSetting;
import codex.renthyl.client.GraphSource;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.draw.RenderMode;
import codex.renthyl.modules.ControlRenderPass;
import codex.renthyl.modules.Junction;
import codex.renthyl.modules.OutputPass;
import codex.renthyl.modules.RenderContainer;
import codex.renthyl.modules.AbstractRenderModule;
import codex.renthyl.modules.RenderModule;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.modules.geometry.GeometryPass;
import codex.renthyl.modules.geometry.QueueMergePass;
import codex.renthyl.modules.geometry.SceneEnqueuePass;
import codex.renthyl.modules.protocol.FilterProtocol;
import codex.renthyl.resources.tickets.ResourceTicket;
import codex.renthyl.resources.tickets.TicketSelector;
import codex.renthyl.util.GeometryRenderHandler;
import codex.renthylplus.effects.FilterChain;
import codex.renthylplus.effects.ports.*;
import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.filters.BloomFilter;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.shader.Shader;
import com.jme3.shader.VarType;
import com.jme3.system.AppSettings;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

/**
 *
 * @author codex
 */
public class TestJmeFilters extends SimpleApplication {
    
    private GeometryPass geometry;
    private RenderContainer<AbstractRenderModule> fpp;
    private JunctionCycleSource activeFilterSource, outputColor;
    private BitmapText filterLabel, formatLabel;
    private int colorFormat = 0;
    
    public static void main(String[] args) {
        TestJmeFilters app = new TestJmeFilters();
        AppSettings settings = new AppSettings(true);
        settings.setWidth(700);
        settings.setHeight(700);
        settings.setRenderer(AppSettings.LWJGL_OPENGL45);
        app.setSettings(settings);
        app.start();
    }
    
    @Override
    public void simpleInitApp() {
        
        Renthyl.initialize(this);
        assetManager.registerLocator("", ImmediateShader.class);
        flyCam.setMoveSpeed(10);
        
        setupFrameGraph();
        setupScene();
        setupGui();
        setupInput();
        
    }
    @Override
    public void simpleUpdate(float tpf) {
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    }
    
    private void setupFrameGraph() {
        
        FrameGraph fg = new FrameGraph(assetManager);
        viewPort.setPipeline(fg);
        
        fg.add(new ControlRenderPass());
        SceneEnqueuePass enqueue = fg.add(SceneEnqueuePass.withLegacyQueues());
        QueueMergePass merge = fg.add(new QueueMergePass());
        geometry = fg.add(new GeometryPass());
        NormalPass normals = fg.add(new NormalPass());
        fpp = fg.add(new RenderContainer<>());
        TextureSliderPass slider = fg.add(new TextureSliderPass());
        Junction colorOut = fg.add(new Junction());
        OutputPass out = fg.add(new OutputPass());
        
        fpp.addInput("Color");
        fpp.addInput("Depth");
        fpp.addInput("Normals");
        fpp.addInput("Geometry");
        fpp.addOutput("Result");
        
        CartoonEdgePass cartoon = fpp.add(new CartoonEdgePass());
        cartoon.setEdgeColor(ColorRGBA.Black);
        fpp.makeInternalInput("Normals", "Normals", cartoon);
        SSAOPass ssao = fpp.add(new SSAOPass(5, 10, 0.2f, 0.1f));
        fpp.makeInternalInput("Normals", "Normals", ssao);
        fpp.add(new CrossHatchPass());
        fpp.add(new FXAAPass());
        fpp.add(new PosterizationPass());
        fpp.add(new ContrastAdjustmentPass(2f));
        BloomPass bloom = fpp.add(new BloomPass(BloomFilter.GlowMode.Objects));
        fpp.makeInternalInput("Geometry", "Geometry", bloom);
        fpp.add(new DepthOfFieldPass());
        fpp.add(new FogPass(ColorRGBA.Black, 5, 100));
        fpp.add(new FilmicToneMapPass(new Vector3f(.5f, .5f, .5f)));
        SoftBloomPass softBloom = fpp.add(new SoftBloomPass());
        softBloom.setGlowFactor(0.5f);
        SSRPass ssr = fpp.add(new SSRPass());
        fpp.makeInternalInput("Normals", "Normals", ssr);
        
        FilterChain<FilterProtocol> chain = fpp.add(new FilterChain());
        chain.addInput("Normals");
        fpp.makeInternalInput("Normals", "Normals", chain);
        CartoonEdgePass cartoon2 = chain.add(new CartoonEdgePass());
        cartoon2.setEdgeColor(ColorRGBA.Green);
        chain.makeInternalInput("Normals", "Normals", cartoon2);
        chain.add(new FXAAPass());
        
        Junction activeFilter = fpp.add(new Junction());
        activeFilterSource = new JunctionCycleSource(activeFilter);
        int i = 0;
        for (RenderModule m : fpp) {
            if (m == activeFilter) {
                continue;
            }
            fpp.makeInternalInput("Color", "Color", m);
            fpp.makeInternalInput("Depth", "Depth", m);
            activeFilter.makeInput(m.getMainOutputGroup(), TicketSelector.name("Result"), TicketSelector.All);
        }
        
        merge.makeInput(enqueue, "Opaque", "Queues[0]");
        merge.makeInput(enqueue, "Sky", "Queues[1]");
        merge.makeInput(enqueue, "Transparent", "Queues[2]");
        merge.makeInput(enqueue, "Gui", "Queues[3]");
        merge.makeInput(enqueue, "Translucent", "Queues[4]");
        merge.makeInput(enqueue.getMainOutputGroup(), TicketSelector.All, TicketSelector.All);
        
        geometry.makeInput(merge, "Result", "Geometry");
        geometry.getColorDef().setFormatFlexible(false);
        normals.makeInput(merge, "Result", "Geometry");
        
        fpp.makeInput(geometry, "Color", "Color");
        fpp.makeInput(geometry, "Depth", "Depth");
        fpp.makeInput(normals, "Result", "Normals");
        fpp.makeInput(merge, "Result", "Geometry");
        fpp.getMainOutputGroup().makeInput(activeFilter.getMainOutputGroup(),
                TicketSelector.name(Junction.OUTPUT), TicketSelector.name("Result"));
        
        slider.setDivide(new GraphSetting<>("SliderDivide", 0.5f));
        slider.makeInput(geometry, "Color", "Texture1");
        slider.makeInput(fpp, "Result", "Texture2");
        
        outputColor = new JunctionCycleSource(colorOut);
        colorOut.makeInput(slider.getMainOutputGroup(), TicketSelector.name("Result"), TicketSelector.First);
        colorOut.makeInput(geometry.getMainOutputGroup(), TicketSelector.name("Color"), TicketSelector.First);
        colorOut.makeInput(normals.getMainOutputGroup(), TicketSelector.name("Result"), TicketSelector.First);
        
        out.makeInput(colorOut, Junction.OUTPUT, "Color");
        out.makeInput(geometry, "Depth", "Depth");
        
    }
    private void setupScene() {
        
        createCube(0, 0, 0, 1, 1, 1, ColorRGBA.Blue);
        createCube(0, 0, 0, 2, 0.3f, 0.3f, ColorRGBA.Red);
        
        PointLight pl = new PointLight();
        pl.setColor(ColorRGBA.White.mult(2f));
        pl.setPosition(new Vector3f(2, 4, 3));
        pl.setRadius(20);
        rootNode.addLight(pl);
        
        rootNode.addLight(new AmbientLight(ColorRGBA.White.mult(0.1f)));
        
    }
    private void setupGui() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        filterLabel = new BitmapText(guiFont);
        filterLabel.setSize(guiFont.getCharSet().getRenderedSize());
        filterLabel.setText("");
        filterLabel.setLocalTranslation(context.getFramebufferWidth()/2 + 5, context.getFramebufferHeight() - 5, 0);
        guiNode.attachChild(filterLabel);
        formatLabel = new BitmapText(guiFont);
        formatLabel.setSize(guiFont.getCharSet().getRenderedSize());
        formatLabel.setText("Format: RGBA8");
        formatLabel.setLocalTranslation(5, context.getFramebufferHeight() - 5, 0);
        guiNode.attachChild(formatLabel);
        updateGui(fpp.get(0).getClass().getSimpleName());
    }
    private void setupInput() {
        inputManager.addMapping("nextFilter", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("nextColorOut", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("nextFormat", new KeyTrigger(KeyInput.KEY_3));
        inputManager.addListener((ActionListener) (String name, boolean isPressed, float tpf) -> {
            if (isPressed) {
                switch (name) {
                    case "nextFilter":
                        activeFilterSource.increment();
                        updateGui(fpp.get(activeFilterSource.getValue()).getClass().getSimpleName());
                        break;
                    case "nextColorOut":
                        outputColor.increment();
                        switch (outputColor.getValue()) {
                            case 0: updateGui(fpp.get(activeFilterSource.getValue()).getClass().getSimpleName()); break;
                            case 1: updateGui("Scene"); break;
                            case 2: updateGui("Normals"); break;
                        }
                        break;
                    case "nextFormat":
                        switch (++colorFormat) {
                            case 0:
                                geometry.getColorDef().setFormat(Image.Format.RGBA8);
                                formatLabel.setText("Format: RGBA8");
                                break;
                            case 1:
                                geometry.getColorDef().setFormat(Image.Format.RGBA16F);
                                formatLabel.setText("Format: RGBA16F");
                                break;
                            case 2:
                                geometry.getColorDef().setFormat(Image.Format.RGBA32F);
                                formatLabel.setText("Format: RGBA32F");
                                colorFormat = -1;
                                break;
                        }
                        break;
                }
            }
        }, "nextFilter", "nextColorOut", "nextFormat");
    }
    
    private void updateGui(String text) {
        filterLabel.setText(text);
    }
    private Geometry createCube(float x, float y, float z, float w, float h, float d, ColorRGBA color) {
        Geometry g = new Geometry("box", new Box(w, h, d));
        g.setLocalTranslation(x, y, z);
        Material m = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", color);
        m.setColor("GlowColor", color);
        g.setMaterial(m);
        rootNode.attachChild(g);
        return g;
    }
    
    private static class NormalPass extends RenderPass {

        private static ImmediateMatDef matdef;
        
        private ResourceTicket<GeometryQueue> geometry;
        private ResourceTicket<Texture2D> result;
        private final ResourceTicket<Texture2D> depth = new ResourceTicket<>("_depth");
        private final TextureDef<Texture2D> resultDef = TextureDef.texture2D(Image.Format.RGBA8);
        private final TextureDef<Texture2D> depthDef = TextureDef.texture2D(Image.Format.Depth);
        private Material material;
        
        @Override
        protected void initialize(FrameGraph frameGraph) {
            geometry = addInput("Geometry");
            result = addOutput("Result");
            if (matdef == null) {
                ImmediateShader vert = new ImmediateShader(Shader.ShaderType.Vertex, true)
                    .includeGlslCompat().includeInstancing().includeSkinning().includeMorphing()
                    .attribute("vec3", "inPosition").attribute("vec3", "inNormal")
                    .varying("vec3", "wNormal")
                    .main()
                        .assign("vec4", "modelSpacePos", "vec4(inPosition, 1.0)")
                        .assign("vec3", "modelSpaceNorm", "inNormal")
                        .ifdef("NUM_MORPH_TARGETS")
                            .call("Morph_Compute", "modelSpacePos", "modelSpaceNorm")
                        .endif()
                        .ifdef("NUM_BONES")
                            .call("Skinning_Compute", "modelSpacePos", "modelSpaceNorm")
                        .endif()
                        .assign("gl_Position", "TransformWorldViewProjection(modelSpacePos)")
                        .assign("wNormal", "TransformWorldNormal(modelSpaceNorm)")
                    .end();
                ImmediateShader frag = new ImmediateShader(Shader.ShaderType.Fragment, true)
                    .includeGlslCompat()
                    .varying("vec3", "wNormal")
                    .main()
                        .assign("gl_FragColor.rgb", "wNormal")
                    .end();
                matdef = new ImmediateMatDef(frameGraph.getAssetManager(), "Normals");
                matdef.createTechnique("PreNormalPass")
                    .setVersions(450, 310, 150)
                    .setShader(vert).setShader(frag)
                    .addWorldParameters("WorldViewProjectionMatrix", "WorldNormalMatrix")
                    .add();
            }
            material = matdef.createMaterial();
        }
        @Override
        protected void prepare(FGRenderContext context) {
            declareTemporary(depthDef, depth);
            declare(resultDef, result);
            reserve(result);
            reference(geometry);
        }
        @Override
        protected void execute(FGRenderContext context) {
            FrameBuffer fb = getFrameBuffer(context, 1);
            resultDef.setSize(fb.getWidth(), fb.getHeight());
            depthDef.setSize(fb.getWidth(), fb.getHeight());
            resources.acquireColorTarget(fb, result);
            resources.acquireDepthTarget(fb, depth);
            context.registerMode(RenderMode.frameBuffer(fb));
            context.clearBuffers();
            context.registerMode(RenderMode.forcedTechnique("PreNormalPass"));
            context.registerMode(RenderMode.forcedMaterial(material));
            resources.acquire(geometry).render(context, GeometryRenderHandler.DEFAULT);
        }
        @Override
        protected void reset(FGRenderContext context) {}
        @Override
        protected void cleanup(FrameGraph frameGraph) {}
        
    }
    private static class TextureSliderPass extends RenderPass {
        
        private static ImmediateMatDef matdef;
        
        private ResourceTicket<Texture2D> tex1, tex2;
        private ResourceTicket<Texture2D> result;
        private final TextureDef<Texture2D> resultDef = TextureDef.texture2D();
        private Material material;
        private GraphSource<Float> divideSource;

        @Override
        protected void initialize(FrameGraph frameGraph) {
            tex1 = addInput("Texture1");
            tex2 = addInput("Texture2");
            result = addOutput("Result");
            if (matdef == null) {
                ImmediateShader frag = new ImmediateShader(Shader.ShaderType.Fragment, true)
                    .includeGlslCompat()
                    .uniform("sampler2D", "Texture1", false)
                    .uniform("sampler2D", "Texture2", false)
                    .uniform("float", "Divide", false)
                    .uniform("float", "DividerThickness", false)
                    .varying("vec2", "texCoord")
                    .main()
                        .assign("float", "dist", "abs(m_Divide - texCoord.x)")
                        ._if("dist < m_DividerThickness")
                            .assign("gl_FragColor", "vec4(1.0)")
                        ._elseif("texCoord.x < m_Divide")
                            .assign("gl_FragColor", "texture2D(m_Texture1, texCoord)")
                        ._else()
                            .assign("gl_FragColor", "texture2D(m_Texture2, texCoord)")
                        .end()
                    .end();
                matdef = new ImmediateMatDef(frameGraph.getAssetManager(), "TextureSlider")
                    .addParam(VarType.Texture2D, "Texture1")
                    .addParam(VarType.Texture2D, "Texture2")
                    .addParam(VarType.Float, "Divide", 0.5f)
                    .addParam(VarType.Float, "DividerThickness", 0.001f);
                matdef.createTechnique()
                    .setVersions(450, 310, 150)
                    .setVertexShader("RenthylCore/MatDefs/Fullscreen/Screen.vert")
                    .setShader(frag)
                    .add();
            }
            material = matdef.createMaterial();
        }
        @Override
        protected void prepare(FGRenderContext context) {
            declare(resultDef, result);
            reserve(result);
            reference(tex1, tex2);
        }
        @Override
        protected void execute(FGRenderContext context) {
            Texture2D inTex1 = resources.acquire(tex1);
            Texture2D inTex2 = resources.acquire(tex2);
            int w = inTex1.getImage().getWidth();
            int h = inTex1.getImage().getHeight();
            resultDef.setSize(w, h);
            resultDef.setFormat(inTex1.getImage().getFormat());
            FrameBuffer fb = getFrameBuffer(w, h, 1);
            resources.acquireColorTarget(fb, result);
            context.registerMode(RenderMode.cameraSize(w, h));
            context.registerMode(RenderMode.frameBuffer(fb));
            context.clearBuffers();
            material.setTexture("Texture1", inTex1);
            material.setTexture("Texture2", inTex2);
            if (divideSource != null) {
                material.setFloat("Divide", divideSource.getGraphValue(frameGraph, context.getViewPort()));
            }
            context.renderFullscreen(material);
        }
        @Override
        protected void reset(FGRenderContext context) {}
        @Override
        protected void cleanup(FrameGraph frameGraph) {}
        
        public void setDivide(GraphSource<Float> divide) {
            this.divideSource = divide;
        }
        
    }
    private static class JunctionCycleSource implements GraphSource<Integer> {

        private final Junction junction;
        private int value = 0;

        public JunctionCycleSource(Junction junction) {
            this.junction = junction;
            this.junction.setIndexSource(this);
        }

        @Override
        public Integer getGraphValue(FrameGraph frameGraph, ViewPort viewPort) {
            return value;
        }

        public void increment() {
            if (++value >= junction.getLength()) {
                value = 0;
            }
        }
        
        public int getValue() {
            return value;
        }

    }
    
}
