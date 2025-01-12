
@dynamic_local_size

layout(RGBA8) uniform image3D VoxelMap;
uniform vec4 Value;

void main() {
    
    imageStore(VoxelMap, ivec3(gl_GlobalInvocationID), Value);
    
}

