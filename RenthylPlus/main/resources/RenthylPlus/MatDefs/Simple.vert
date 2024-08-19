
#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"

#ifdef NUM_BONES
    #import "Common/ShaderLib/Skinning.glsllib"
#endif
#ifdef NUM_MORPH_TARGETS
    #import "Common/ShaderLib/MorphAnim.glsllib"
#endif

attribute vec3 inPosition;
attribute vec2 inTexCoord;
varying vec2 texCoord;

void main() {

    vec4 modelSpacePos = vec4(inPosition, 1.0);

    #ifdef NUM_MORPH_TARGETS
        Morph_Compute(modelSpacePos);
    #endif

    #ifdef NUM_BONES
        Skinning_Compute(modelSpacePos);
    #endif

    gl_Position = TransformWorldViewProjection(modelSpacePos);
    texCoord = inTexCoord;
    
}






