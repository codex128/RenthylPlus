
#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"
#import "Common/ShaderLib/Skinning.glsllib"
#import "Common/ShaderLib/MorphAnim.glsllib"

in vec3 inPosition;
in vec3 inNormal;
in vec2 inTexCoord;
#ifdef VERTEX_COLOR
    in vec4 inColor;
#endif

uniform vec4 m_BaseColor;
uniform vec3 m_GridMin;
uniform vec3 m_GridMax;

out vec3 wNorm;
out vec2 uv;
out vec4 color;

void main() {

    vec4 modelSpacePos = vec4(inPosition, 1.0);
    vec3 modelSpaceNorm = inNormal;

    #ifdef NUM_MORPH_TARGETS
        Morph_Compute(modelSpacePos, modelSpaceNorm);
    #endif
    #ifdef NUM_BONES
        Skinning_Compute(modelSpacePos, modelSpaceNorm);
    #endif
    
    gl_Position = TransformWorld(modelSpacePos);
    wNorm = TransformWorldNormal(modelSpaceNorm);
    uv = inTexCoord;
    
    color = m_BaseColor;
    #ifdef VERTEX_COLOR
        color *= inColor;
    #endif
    
}






