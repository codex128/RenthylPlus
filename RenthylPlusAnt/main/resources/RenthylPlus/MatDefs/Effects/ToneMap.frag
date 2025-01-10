
#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform sampler2D m_ColorMap;
uniform sampler2D m_ToneMap;
uniform vec2 m_ToneTexelSize;

varying vec2 texCoord;

const float valuesPerChannel = 64.0;

void main() {
    
    vec4 color = min(texture2D(m_ColorMap, texCoord), vec4(1.0));
    
    vec4 index = floor(color * (valuesPerChannel - 1.0));
    
    index.a = floor(index.b / 8.0);
    index.b = floor(index.b - index.a * 8.0);
    //index.b = 0.0;
    //index.a = 0.0;
    
    vec2 uv = vec2(0.0);
    uv.x = (valuesPerChannel * index.b + index.r) * m_ToneTexelSize.x;
    uv.y = (valuesPerChannel * index.a + index.g) * m_ToneTexelSize.y;
    
    gl_FragColor.rgb = texture2D(m_ToneMap, uv).rgb * 2.5;
    gl_FragColor.a = color.a;
    
    //gl_FragColor.rg = color.rg;
    
}
