
#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform sampler3D m_VoxelMap;
uniform int m_GridSize;
varying vec3 vPosition;

void main() {
    
    if (vPosition.x < 0.0 || vPosition.x >= 1.0 || vPosition.y < 0.0 || vPosition.y >= 1.0 || vPosition.z < 0.0 || vPosition.z >= 1.0) {
        discard;
    }
    
    //ivec3 gridSize = imageSize(m_VoxelMap);
    //gl_FragColor = imageLoad(m_VoxelMap, ivec3(m_GridSize * vPosition));
    gl_FragColor = texture3D(m_VoxelMap, vPosition);
    
    if (gl_FragColor.a < 0.9) {
        discard;
    }
    
    //gl_FragColor.rgb = vPosition;
    
    //if (any(greaterThan(gl_FragColor, vec4(1.0)))) {
    //    gl_FragColor.r = 1.0;
    //}
    
}






