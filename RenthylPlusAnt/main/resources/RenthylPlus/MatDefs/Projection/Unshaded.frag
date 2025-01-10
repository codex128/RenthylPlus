
#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Projection.glsllib"

uniform sampler2D m_Texture;
uniform sampler2D m_ColorMap;
uniform sampler2D m_DepthMap;
uniform mat4 m_SceneViewProjectionMatrix;
uniform mat4 m_ProjectorViewMatrix;

varying vec2 texCoord;

void main() {
    
    float depth = texture2D(m_DepthMap, texCoord).r;
    vec3 pos = getPosition(texCoord, depth, m_SceneViewProjectionMatrix);
    vec2 uv = getProjectedTexCoord(pos, m_ProjectorViewMatrix);
    
    gl_FragColor = texture2D(m_ColorMap, texCoord);
    if (uv.x >= 0.0 && uv.x <= 1.0 && uv.y >= 0.0 && uv.y <= 1.0) {
        vec4 projColor = texture2D(m_Texture, uv);
        gl_FragColor.rgb = mix(gl_FragColor.rgb, projColor.rgb, projColor.a);
        gl_FragColor.a = min(1.0, gl_FragColor.a + projColor.a);
    }
    
    
}

