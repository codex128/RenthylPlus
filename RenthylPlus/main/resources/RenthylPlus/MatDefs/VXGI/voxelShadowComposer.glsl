
@dynamic_local_size

#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "RenthylPlus/ShaderLib/Shadows.glsllib"

uniform sampler2D ShadowMaps[NUM_SHADOW_MAPS];
uniform mat4 LightMatrices[NUM_SHADOW_MAPS];
uniform vec2 InverseRanges[NUM_SHADOW_MAPS];
uniform int LightIndices[NUM_SHADOW_MAPS];
uniform int LightTypes[NUM_SHADOW_MAPS];

layout(RGBA32F) uniform image3D VoxelLightMap;
uniform vec3 GridMin;
uniform vec3 GridMax;
uniform int CurrentBatch;

shared int table = 0;

void main() {
    
    int i = int(gl_LocalInvocationIndex);
    int batch = i + CurrentBatch;
    ivec3 texel = ivec3(gl_GlobalInvocationID);
    
    if (CurrentBatch > 0) {
        if (i == 0) {
            table = int(imageLoad(VoxelLightMap, texel).r);
        }
        barrier();
    }
    
    int result = 0;
    vec3 texCoord = vec3(gl_WorkGroupID) / vec3(gl_NumWorkGroups);
    vec3 voxSize = (GridMax - GridMin) / vec3(imageSize(VoxelLightMap));
    vec3 wPos = ((GridMax - GridMin) / vec3(gl_NumWorkGroups)) * vec3(gl_WorkGroupID) + GridMin + voxSize*0.5;
    /*mat4 mat = LightMatrices[i + CurrentBatch];
    vec2 range = InverseRanges[i + CurrentBatch].xy;
    vec4 lightViewPos = mat * vec4(wPos, 1.0);
    vec2 lightUv = (lightViewPos.xy / lightViewPos.w + 1.0) * 0.5;
    float depth = lightViewPos.z / lightViewPos.w;
    float shadow = textureLod(ShadowMaps[i + CurrentBatch], lightUv, 0.0).r;
    if (depth >= 0.0 && lightUv.x >= 0.0 && lightUv.x <= 1.0 && lightUv.y >= 0.0 && lightUv.y <= 1.0) {
        depth = linearizeDepth(depth, range);
        if (depth <= shadow) {
            result = 1 << LightIndices[i + CurrentBatch];
        }
    }*/
    
    if (isExposedToLight(wPos, ShadowMaps[batch], LightMatrices[batch], InverseRanges[batch], true)) {
        result = 1 << LightIndices[batch];
    }
    
    atomicOr(table, result);
    
    memoryBarrierShared();
    if (i == 0) {
        imageStore(VoxelLightMap, texel, vec4(table, 0.0, 0.0, 0.0));
        //imageStore(VoxelLightMap, texel, vec4(1.0 - textureLod(ShadowMaps[batch], texCoord.xy, 0.0).r));
        //imageStore(VoxelLightMap, texel, vec4(wPos, 1.0));
        //imageStore(VoxelLightMap, ivec3(gl_GlobalInvocationID), vec4(0.0) + textureLod(ShadowMaps[i], texCoord.xy, 0.0));
        //imageStore(VoxelLightMap, ivec3(gl_GlobalInvocationID), abs(LightMatrices[i] * vec4(wPos, 1.0) * 10000.0));
        //imageStore(VoxelLightMap, ivec3(gl_GlobalInvocationID), vec4((wPos + vec3(20.0)) / 40.0, 1.0));
    }
    
}

