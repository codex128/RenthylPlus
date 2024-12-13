
//dynamic_local_size
layout (local_size_x = 2, local_size_y = 2, local_size_z = 2) in;

#import "Common/ShaderLib/GLSLCompat.glsllib"

layout(RGBA32F) uniform image3D UpperLevel;
layout(RGBA32F) uniform image3D LowerLevel;

shared int r = 0;
shared int g = 0;
shared int b = 0;
shared int a = 0;
const vec2 expansion = vec2(100000.0, 0.00001);

void main() {
    
    ivec3 lowerTexel = ivec3(gl_WorkGroupID);
    ivec3 upperTexel = lowerTexel * 2;
    vec4 result = imageLoad(UpperLevel, ivec3(gl_GlobalInvocationID));
    
    if (gl_LocalInvocationIndex > 0) {
        ivec4 vals = ivec4(result * expansion.x);
        atomicAdd(r, vals.r);
        atomicAdd(g, vals.g);
        atomicAdd(b, vals.b);
        atomicAdd(a, vals.a);
    }
    
    if (gl_LocalInvocationIndex == 0) {
        memoryBarrierShared();
        result += vec4(r, g, b, a) * expansion.y;
        imageStore(LowerLevel, ivec3(gl_WorkGroupID), result * 0.125);
    }
    
}

