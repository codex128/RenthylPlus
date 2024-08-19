
#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform sampler2D m_DepthMap;
varying vec2 texCoord;

void main() {
    
    gl_FragColor.a = 1.0;
    gl_FragColor.rgb = vec3(1.0 - texture2D(m_DepthMap, texCoord).r);
    
}

