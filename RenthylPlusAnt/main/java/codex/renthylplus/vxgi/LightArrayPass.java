/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.vxgi;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.definitions.arrays.FloatArrayDef;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.tickets.ResourceTicket;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.LightProbe;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import java.util.Collection;
import java.util.LinkedList;

/**
 *
 * @author codex
 */
public class LightArrayPass extends RenderPass {

    public static final int FLOATS_PER_LIGHT = 12;
    
    private ResourceTicket<Collection<Light>> lights;
    private ResourceTicket<Light[]> lightShadowMap;
    private ResourceTicket<float[]> lightArray;
    private ResourceTicket<ColorRGBA> ambient;
    private ResourceTicket<Collection<LightProbe>> probes;
    private final FloatArrayDef arrayDef = new FloatArrayDef();
    private final ColorRGBA ambientColor = new ColorRGBA(0, 0, 0, 0);
    private final LinkedList<LightProbe> probeList = new LinkedList<>();
    private final ColorRGBA tempColor = new ColorRGBA(0, 0, 0, 0);
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        lights = addInput("Lights");
        lightShadowMap = addInput("Shadows");
        lightArray = addOutput("LightArray");
        ambient = addOutput("Ambient");
        probes = addOutput("Probes");
        arrayDef.setPadding(0);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(arrayDef, lightArray);
        declarePrimitive(ambient, probes);
        reference(lights);
        referenceOptional(lightShadowMap);
    }
    @Override
    protected void execute(FGRenderContext context) {
        Collection<Light> list = resources.acquire(lights);
        arrayDef.setSize(list.size() * FLOATS_PER_LIGHT);
        Light[] shadows = resources.acquireOrElse(lightShadowMap, null);
        float[] array = resources.acquire(lightArray);
        int i = 0;
        for (Light l : list) {
            if (l.getType() == Light.Type.Ambient) {
                ambientColor.addLocal(l.getColor());
                continue;
            }
            if (l.getType() == Light.Type.Probe) {
                probeList.add((LightProbe)l);
                continue;
            }
            int id = l.getType().getId();
            if (shadows != null) {
                if (id > 3) {
                    throw new IllegalStateException("Light type id is larger than two bits: cannot pack shadow indices.");
                }
                int shadowIndex = indexOf(shadows, l);
                if (shadowIndex >= 0) {
                    id += (shadowIndex + 1) << 2;
                }
            }
            // todo: utilize unused elements
            tempColor.set(l.getColor()).setAlpha(id);
            packColor(array, i, tempColor);
            switch (l.getType()) {
                case Directional:
                    DirectionalLight dl = (DirectionalLight)l;
                    packVector(array, i + 4, dl.getDirection());
                    break;
                case Point:
                    PointLight pl = (PointLight)l;
                    packVector(array, i + 4, pl.getPosition());
                    array[i + 7] = pl.getInvRadius();
                    break;
                case Spot:
                    SpotLight sl = (SpotLight)l;
                    packVector(array, i + 4, sl.getPosition());
                    array[i + 7] = sl.getInvSpotRange();
                    packVector(array, i + 8, sl.getDirection());
                    array[i + 11] = sl.getPackedAngleCos();
                    break;
            }
            i += 12;
        }
        resources.setPrimitive(ambient, ambientColor);
        resources.setPrimitive(probes, probeList);
    }
    @Override
    protected void reset(FGRenderContext context) {
        ambientColor.set(0, 0, 0, 0);
        probeList.clear();
    }
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
    private int packColor(float[] array, int i, ColorRGBA color) {
        array[i++] = color.r;
        array[i++] = color.g;
        array[i++] = color.b;
        array[i++] = color.a;
        return i;
    }
    private int packVector(float[] array, int i, Vector3f vec) {
        array[i++] = vec.x;
        array[i++] = vec.y;
        array[i++] = vec.z;
        return i;
    }
    private int indexOf(Object[] array, Object element) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == element) {
                return i;
            }
            if (array[i] == null) {
                return -1;
            }
        }
        return -1;
    }
    
}
