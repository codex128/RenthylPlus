/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import com.jme3.light.Light;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector2f;
import com.jme3.texture.Texture2D;

/**
 *
 * @author gary
 */
public class ShadowMap {
    
    private Texture2D map;
    private final Matrix4f projection = new Matrix4f();
    private Light light;
    private Vector2f range = new Vector2f();

    public ShadowMap(Texture2D map) {
        this.map = map;
    }

    public void setMap(Texture2D map) {
        this.map = map;
    }
    public void setProjection(Matrix4f projection) {
        this.projection.set(projection);
    }
    public void setLight(Light light) {
        this.light = light;
    }
    public void setRange(Vector2f range) {
        this.range = range;
    }
    public void setRange(float min, float max) {
        this.range.set(min, max);
    }
    
    public Texture2D getMap() {
        return map;
    }
    public Matrix4f getProjection() {
        return projection;
    }
    public Light getLight() {
        return light;
    }
    public Vector2f getRange() {
        return range;
    }
    
}
