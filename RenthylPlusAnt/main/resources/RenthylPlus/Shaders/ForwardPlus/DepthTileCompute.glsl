
// hint: dynamically set local size at runtime
@dynamic_local_size

uniform image2D depthTexture;
uniform image2D tileImage;
uniform vec2 tileSize;

const uint range = 1000000;
const float invRange = 0.000001;

atomic uint maxTileDepth = 0;
atomic uint minTileDepth = range;

void main() {
	
	uint depth = uint(imageLoad(depthTexture, gl_GlobalInvocationId.xy).r * range);
	atomicMax(maxTileDepth, depth);
	atomicMin(minTileDepth, depth);
	
	memoryBarrier();
	
	if (gl_LocalInvocationIndex == 0) {
	    vec4 result = vec4(float(maxTileDepth), float(maxTileDepth), 0.0, 0.0) * invRange;
        imageStore(tileImage, gl_GlobalInvocationId.xy, result);
    }
    
}

