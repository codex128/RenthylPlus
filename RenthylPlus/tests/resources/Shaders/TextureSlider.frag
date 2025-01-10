
#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform sampler2D m_Texture1;
uniform sampler2D m_Texture2;
uniform float m_Divide;

varying vec2 texCoord;

void main() {
    
    if (texCoord.x <= m_Divide) {
        gl_FragColor = texture2D(m_Texture1, texCoord);
    } else {
        gl_FragColor = texture2D(m_Texture2, texCoord);
    }
    
}

