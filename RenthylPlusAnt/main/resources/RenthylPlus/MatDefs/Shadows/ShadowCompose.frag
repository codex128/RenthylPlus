
#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "RenthylPlus/ShaderLib/Projection.glsllib"
#import "RenthylPlus/ShaderLib/Shadows.glsllib"

uniform sampler2D m_SceneDepthMap;
uniform sampler2D m_ShadowMap;
uniform mat4 m_CamViewProjectionInverse;
uniform mat4 m_LightViewProjectionMatrix;
uniform int m_LightIndex;
uniform int m_LightType;
uniform vec2 m_LightRangeInverse;

varying vec2 texCoord;

void main() {
    
    float sceneDepth = texture2D(m_SceneDepthMap, texCoord).r;
    vec3 fragPos = getPosition(texCoord, sceneDepth, m_CamViewProjectionInverse);
    if (isExposedToLight(fragPos, m_ShadowMap, m_LightViewProjectionMatrix, m_LightRangeInverse, false)) {
        gl_FragColor.r = 1 << m_LightIndex;
    }
    
}











