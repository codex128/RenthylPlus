/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.tests;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.Renthyl;
import codex.renthyl.modules.OutputPass;
import codex.renthyl.modules.cache.CacheWrite;
import codex.renthyl.modules.geometry.GeometryPass;
import codex.renthyl.modules.geometry.QueueMergePass;
import codex.renthyl.modules.geometry.SceneEnqueuePass;
import codex.renthyl.util.GeometryRenderHandler;
import com.jme3.app.SimpleApplication;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;

/**
 *
 * @author codex
 */
public class TestOcclusionCulling extends SimpleApplication {
    
    public static void main(String[] args) {
        TestOcclusionCulling app = new TestOcclusionCulling();
        app.start();
    }
    
    @Override
    public void simpleInitApp() {
        
        Renthyl.initialize(this);
        
        FrameGraph fg = new FrameGraph(assetManager);
        SceneEnqueuePass enqueue = fg.add(SceneEnqueuePass.withLegacyQueues());
        QueueMergePass merge = fg.add(new QueueMergePass());
        GeometryPass geometry = fg.add(new GeometryPass());
        OutputPass out = fg.add(new OutputPass());
        CacheWrite write = fg.add(new CacheWrite());
        
        geometry.setGeometryHandler(new VisibleHandler(true));
        
    }
    
    private static class VisibleHandler implements GeometryRenderHandler {
        
        public static final String USERDATA = VisibleHandler.class.getSimpleName() + "$visible";
        
        public static void setVisible(Spatial spatial, boolean visible) {
            spatial.setUserData(USERDATA, visible);
        }
        
        public static boolean isVisible(Spatial spatial) {
            Boolean val = spatial.getUserData(USERDATA);
            return val == null || val;
        }
        
        private final boolean visible;
        
        public VisibleHandler(boolean visible) {
            this.visible = visible;
        }
        
        @Override
        public void renderGeometry(FGRenderContext context, Geometry g) {
            if (visible == isVisible(g)) {
                context.getRenderManager().renderGeometry(g);
            }
        }
        
    }
    
}
