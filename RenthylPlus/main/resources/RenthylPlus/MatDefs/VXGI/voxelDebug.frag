
#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform sampler3D m_VoxelMap;
varying vec3 vPosition;

void main() {
    
    if (vPosition.x < 0.0 || vPosition.x >= 1.0 || vPosition.y < 0.0 || vPosition.y >= 1.0 || vPosition.z < 0.0 || vPosition.z >= 1.0) {
        discard;
    }
    
    gl_FragColor = textureLod(m_VoxelMap, vPosition, 0.0);
    
}






