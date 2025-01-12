
#import "Common/ShaderLib/GLSLCompat.glsllib"

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

vec4 worldToProjection(vec3 world) {
    vec3 voxelPos = (world - m_GridMin) / (m_GridMax - m_GridMin);
    return vec4(voxelPos.xy * 2.0 - 1.0, 0.0, 1.0);
}
void transformVertex(vec3 n, uint i) {
    vec4 p = gl_in[i].gl_Position;
    vec3 wSwizzle;
    if (n.x > n.y && n.x > n.z) {
        // x major
        wSwizzle = vec3(p.yz, 0.0);
    } else if (n.y > n.z) {
        // y major
        wSwizzle = vec3(p.xz, 0.0);
    } else {
        // z major
        wSwizzle = vec3(p.xy, 0.0);
    }
    gl_Position = worldToProjection(wSwizzle);
    wPosition = p.xyz;
    vPosition = (wPosition - m_GridMin) / (m_GridMax - m_GridMin);
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






