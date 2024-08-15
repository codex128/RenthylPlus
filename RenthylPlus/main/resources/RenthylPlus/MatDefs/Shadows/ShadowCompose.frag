
#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform float g_Time;

uniform sampler2D m_SceneDepthMap;
uniform sampler2D m_ShadowMap;
uniform mat4 m_CamViewProjectionInverse;
uniform mat4 m_LightViewProjectionMatrix;
uniform int m_LightIndex;
uniform int m_LightType;
uniform vec2 m_LightRange;

varying vec2 texCoord;

float linearizeDepth(float depth, vec2 range) {
    float z = depth * 2.0 - 1.0; // back to NDC 
    return (2.0 * range.x * range.y) / (range.y + range.x - z * (range.y - range.x));
}

vec3 getPosition(in vec2 texCoord, in float depth, in mat4 matrixInverse){
    vec4 pos = vec4(1.0);
    pos.xy = (texCoord * vec2(2.0)) - vec2(1.0);
    pos.z = depth * 2.0 - 1.0;
    pos = matrixInverse * pos;
    pos.xyz /= pos.w;
    return pos.xyz;
}

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
        //shadow = linearizeDepth(shadow, m_LightRange);
        //shadow = (m_LightRange.y - m_LightRange.x) * shadow + m_LightRange.x;
        depth = (1.0 / depth - 1.0 / m_LightRange.x) / (1.0 / m_LightRange.y - 1.0 / m_LightRange.x);
        //depth = linearizeDepth(depth, m_LightRange);
        //depth = (depth - 1.0) / 2.0;
        //gl_FragColor.rgb = vec3(1.0 - shadow);
        //gl_FragColor.g = 1.0 - shadow;
        //if (shadow >= 0.99) {
        //    gl_FragColor.rgb = vec3((sin(g_Time) + 1.0) / 2.0);
        //}
        if (depth <= shadow) {
            int val = 1 << m_LightIndex;
            gl_FragColor.r = val;
            //gl_FragColor.a = 1.0;
        }
        //shadow = shadow * 2.0 - 1.0;
        //float depth = (lightViewPos.z + 1.0) / 2.0;
        /*if (depth <= shadow) {
            gl_FragColor.r = 1 << m_LightIndex;
        } else {
            gl_FragColor.b = 1.0;
        }*/
    } else {
        gl_FragColor.g = 1.0;
    }
    
}











