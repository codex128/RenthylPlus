
#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"

#ifdef NUM_BONES
    #import "Common/ShaderLib/Skinning.glsllib"
#endif
#ifdef NUM_MORPH_TARGETS
    #import "Common/ShaderLib/MorphAnim.glsllib"
#endif

uniform vec3 m_GridMin;
uniform vec3 m_GridMax;

attribute vec3 inPosition;
varying vec3 vPosition;

void main() {

    vec4 modelSpacePos = vec4(inPosition, 1.0);

    #ifdef NUM_MORPH_TARGETS
        Morph_Compute(modelSpacePos);
    #endif

    #ifdef NUM_BONES
        Skinning_Compute(modelSpacePos);
    #endif
    
    vec4 world = TransformWorld(modelSpacePos);
    gl_Position = TransformWorldViewProjection(modelSpacePos);
    
    vPosition = (world.xyz - m_GridMin) / (m_GridMax - m_GridMin);
    
}






