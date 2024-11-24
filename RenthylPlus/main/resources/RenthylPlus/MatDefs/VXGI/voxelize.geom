
#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"

layout (triangles) in;
layout (triangle_strip, max_vertices = 3) out;

uniform vec3 m_GridMin;
uniform vec3 m_GridMax;

in vec3 wNorm[];
in vec2 uv[];
in vec4 color[];

out vec3 wPosition;
out vec3 vPosition;
out vec3 wNormal;
out vec2 texCoord;
out vec4 Color;

vec4 transformToViewSpace(vec3 voxelPos) {
    return vec4(voxelPos.xy * 2.0 - 1.0, voxelPos.z, 1.0);
}
void transformVertex(vec3 n, uint i) {
    vec4 p = gl_in[i].gl_Position;
    vec4 mPos;
    if (n.x > n.y && n.x > n.z) {
        // x major
        mPos = vec4(p.y, p.z, 0.0, 1.0);
    } else if (n.y > n.z) {
        // y major
        mPos = vec4(p.x, p.z, 0.0, 1.0);
    } else {
        // z major
        mPos = vec4(p.x, p.y, 0.0, 1.0);
    }
    wPosition = TransformWorld(gl_in[i].gl_Position).xyz;
    vPosition = (wPosition - m_GridMin) / (m_GridMax - m_GridMin);
    gl_Position = transformToViewSpace(vPosition);
    wNormal = wNorm[i];
    texCoord = uv[i];
    Color = color[i];
    EmitVertex();
}

void main() {
    
    // calculate normal from vertices
    vec3 a = gl_in[0].gl_Position.xyz;
    vec3 b = gl_in[1].gl_Position.xyz;
    vec3 c = gl_in[2].gl_Position.xyz;
    vec3 normal = abs(cross(b-a, c-a));
    
    // Reproject towards the largest axis of the normal vector.
    // Otherwise triangles whose normals are facing more away from
    // the camera will not raster properly. The z axis is unnecessary
    // so it is left at zero.
    transformVertex(normal, 0);
    transformVertex(normal, 1);
    transformVertex(normal, 2);
    
    EndPrimitive();
    
}






