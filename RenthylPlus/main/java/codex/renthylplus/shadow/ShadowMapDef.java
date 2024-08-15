/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import codex.renthyl.definitions.AbstractResourceDef;
import codex.renthyl.definitions.TextureDef;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

/**
 *
 * @author gary
 */
public class ShadowMapDef extends AbstractResourceDef<ShadowMap> {

    private TextureDef<Texture2D> mapDef = TextureDef.texture2D();
    
    public ShadowMapDef() {
        mapDef.setFormat(Image.Format.Depth);
        mapDef.setShadowCompare(Texture.ShadowCompareMode.LessOrEqual);
    }
    
    @Override
    public ShadowMap createResource() {
        return new ShadowMap(mapDef.createResource());
    }
    @Override
    public ShadowMap applyDirectResource(Object resource) {
        if (resource instanceof ShadowMap) {
            ShadowMap shadow = (ShadowMap)resource;
            Texture2D map = mapDef.applyDirectResource(shadow.getMap());
            if (map != null) {
                return shadow;
            }
        }
        return null;
    }
    @Override
    public ShadowMap applyIndirectResource(Object resource) {
        Texture2D map = mapDef.applyIndirectResource(resource);
        if (map != null) {
            return new ShadowMap(map);
        }
        return null;
    }
    
    public TextureDef<Texture2D> getMapDef() {
        return mapDef;
    }
    
}
