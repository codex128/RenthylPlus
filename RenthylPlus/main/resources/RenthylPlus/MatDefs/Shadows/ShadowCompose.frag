
#import "Common/ShaderLib/GLSLCompat.glsllib"

#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "RenthylPlus/ShaderLib/Projection.glsllib"

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
    vec4 lightViewPos = m_LightViewProjectionMatrix * vec4(fragPos, 1.0);
    vec2 lightUv = (lightViewPos.xy / lightViewPos.w + 1.0) / 2.0;
    float depth = lightViewPos.z;
    
    gl_FragColor = vec4(0.0);
    bool inside = lightUv.x >= 0.0 && lightUv.x <= 1.0 && lightUv.y >= 0.0 && lightUv.y <= 1.0;
    if (depth >= 0.0 && (m_LightType == 0 || inside)) {
        float shadow = texture2D(m_ShadowMap, lightUv).r;
        depth = (1.0 / depth - m_LightRangeInverse.x) / (m_LightRangeInverse.y - m_LightRangeInverse.x);
        if (depth <= shadow) {
            gl_FragColor.r = 1 << m_LightIndex;
        }
    }
    
}











