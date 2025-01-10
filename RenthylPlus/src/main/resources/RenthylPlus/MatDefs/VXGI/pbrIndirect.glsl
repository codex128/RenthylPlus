
@dynamic_local_size

#import "RenthylPlus/ShaderLib/Projection.glsllib"
#import "RenthylPlus/MatDefs/VXGI/gbufferCompression.glsllib"

uniform sampler3D VoxelMap;
uniform sampler2D ColorMap;
uniform sampler2D DepthMap;
uniform sampler2D DiffuseMap;
uniform sampler2D PositionMap;
uniform sampler2D NormalMap;
uniform sampler2D MaterialMap;
uniform mat4 CameraMatrixInverse;
uniform vec3 CameraPosition;
uniform vec3 GridMin;
uniform vec3 GridMax;
uniform int GridSize;
uniform float TraceTangent;
uniform vec2 SpecularAngleRange;
uniform float TraceQuality;
uniform vec3 TraceDirections[NUM_TRACES];
uniform float IndirectFactor;

layout(RGBA8) uniform image2D Target;

#define TRACING_VOXEL_MAP VoxelMap
#define GRID_MIN GridMin
#define GRID_MAX GridMax
#define GRID_SIZE GridSize
#import "RenthylPlus/MatDefs/VXGI/voxelConeTracing.glsllib"

shared int atomicR = 0;
shared int atomicG = 0;
shared int atomicB = 0;
const vec2 expansion = vec2(100000.0, 0.00001);

void main() {
    
    ivec2 texel = ivec2(gl_WorkGroupID.xy);
    
    vec3 diffuse = texelFetch(DiffuseMap, texel, 0).rgb;
    vec3 wPosition = texelFetch(PositionMap, texel, 0).xyz;
    vec3 normal = texelFetch(NormalMap, texel, 0).xyz;
    
    // unpack material
    vec3 specular;
    vec2 metalRough;
    vec4 gbuffer = texelFetch(MaterialMap, texel, 0);
    expandGBuffer(gbuffer, specular, metalRough);
    wPosition += normal * maxVoxelSize() * 2.2;
    
    // get trace direction and angle
    vec3 direction;
    float apertureTan2;
    if (gl_LocalInvocationIndex == 0) {
        // first invocation does specular
        direction = reflect(normalize(wPosition - CameraPosition), normal);
        apertureTan2 = 2.0 * tan(mix(SpecularAngleRange.x, SpecularAngleRange.y, clamp(/*metalRough.y*/1.0, 0.0, 1.0)));
    } else {
        // all other invocations do diffuse
        vec3 xtan = orthogonal(normal);
        vec3 ytan = cross(normal, xtan);
        mat3 tanSpace = mat3(xtan, ytan, normal);
        direction = normalize(tanSpace * TraceDirections[gl_LocalInvocationIndex - 1]);
        apertureTan2 = TraceTangent * 2.0;
    }
    
    // approximate indirect lighting
    vec3 color = traceVoxelCone(wPosition, direction, apertureTan2, TraceQuality);
    if (gl_LocalInvocationIndex == 0) {
        //color *= specular;
    } else {
        //color *= metalRough.y;
    }
    
    // combine trace results with results from other invocations
    ivec3 iClr = ivec3(color * expansion.x);
    atomicAdd(atomicR, iClr.r);
    atomicAdd(atomicG, iClr.g);
    atomicAdd(atomicB, iClr.b);
    
    // add indirect color to result
    //barrier();
    if (gl_LocalInvocationIndex == 0) {
        memoryBarrier();
        vec4 baseColor = texelFetch(ColorMap, texel, 0);
        vec4 indirect = vec4(atomicR, atomicG, atomicB, 0.0) * vec4(diffuse, 0.0) * IndirectFactor * expansion.y;
        imageStore(Target, texel, indirect + baseColor);
        //imageStore(Target, texel, indirect);
        //imageStore(Target, texel, color.rgbb);
        //imageStore(Target, texel, vec4(wPosition * 0.01, 1.0));
        //imageStore(Target, texel, vec4(diffuse, 1.0));
        //imageStore(Target, texel, indirect);
        //imageStore(Target, texel, vec4(1.0 - depth, 0.0, 0.0, 1.0));
        //imageStore(Target, texel, vec4(direction, 1.0));
        //imageStore(Target, texel, vec4(1.0, 0.0, 0.0, 1.0));
    }
    
}

