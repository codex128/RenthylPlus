
#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/PBR.glsllib"
#import "Common/ShaderLib/Parallax.glsllib"
#import "Common/ShaderLib/Lighting.glsllib"

#ifdef TEMPORAL
    #import "RenthylPlus/MatDefs/VXGI/coneTracing.glsllib"
#endif

uniform vec3 g_CameraPosition;

uniform vec4 m_LightData[NUM_LIGHTS];
uniform vec4 m_AmbientLight;

layout(RGBA32F) uniform image3D m_VoxelMap;
uniform int m_GridSize;

varying vec3 wPosition;
varying vec3 vPosition;
varying vec3 wNormal;
varying vec2 texCoord;
varying vec4 Color;

#ifdef BASECOLORMAP
    uniform sampler2D m_BaseColorMap;
#endif
#ifdef EMISSIVE
    uniform vec4 m_Emissive;
#endif
#ifdef EMISSIVEMAP
    uniform sampler2D m_EmissiveMap;
#endif
#if defined(EMISSIVE) || defined(EMISSIVEMAP)
    uniform float m_EmissivePower;
    uniform float m_EmissiveIntensity;
#endif
#ifdef DISCARD_ALPHA
    uniform float m_AlphaDiscardThreshold;
#endif
#ifdef SHADOWS
    uniform sampler2D m_LightContributionMap;
#endif

void main() {
    
    if (vPosition.x < 0.0 || vPosition.x >= 1.0 || vPosition.y < 0.0 || vPosition.y >= 1.0 || vPosition.z < 0.0 || vPosition.z >= 1.0) {
        discard;
    }
    
    vec4 diffuseColor = Color;
    #ifdef BASECOLORMAP
        diffuseColor *= texture2D(m_BaseColorMap, texCoord);
    #endif
    
    float alpha = diffuseColor.a;
    #ifdef DISCARD_ALPHA
        if (alpha < m_AlphaDiscardThreshold) {
            discard;
        }
    #endif
    
    vec3 viewDir = normalize(g_CameraPosition - wPosition);
    vec3 normal = normalize(wNormal);
    vec3 fZero = vec3(0.5);
    vec4 result = vec4(diffuseColor.rgb * m_AmbientLight.rgb * m_AmbientLight.a, diffuseColor.a);
    float ndotv = max(dot(normal, viewDir), 0.0);
    
    for (int i = 0, l = NUM_LIGHTS*3; i < l; i += 3) {
        #ifdef USE_LIGHT_TEXTURES
            #ifdef TILED_LIGHTS
                if (componentIndex == 0 || lightIndex.x < 0) {
                    // get indices from next pixel
                    lightIndex = texture2D(m_LightIndex, vec2(x, y) * m_LightIndexSize.yz);
                }
                // apply index from each component in order
                vec2 pixel = vec2(m_LightTexInv, 0);
                switch (componentIndex) {
                    case 0: pixel.x *= lightIndex.x; break;
                    case 1: pixel.x *= lightIndex.y; break;
                    case 2: pixel.x *= lightIndex.z; break;
                    case 3: pixel.x *= lightIndex.w; break;
                }
                // increment indices
                componentIndex++;
                if (componentIndex > 3) {
                    componentIndex = 0;
                    x++;
                    if (x >= m_LightIndexSize.x) {
                        x = 0;
                        y++;
                    }
                }
            #else
                vec2 pixel = vec2(m_LightTexInv * i, 0);
            #endif
            vec4 lightColor = texture2D(m_LightTex1, pixel);
            vec4 lightData1 = texture2D(m_LightTex2, pixel);
        #else
            vec4 lightColor = m_LightData[i];
            vec4 lightData1 = m_LightData[i+1];
        #endif
        #ifdef SHADOWS
            // shadowIndex packed as all bits past the first two, which represent the light type
            int shadowIndex = int(lightColor.w) >> 2;
            // shadowIndex=0 means light does not cast shadows
            if (shadowIndex > 0) {
                int table = int(texture2D(m_LightContributionMap, texCoord).r);
                // indexing is zero-based, so 1 must be subtracted from shadowIndex
                if (((table >> (shadowIndex - 1)) & 1) == 0) {
                    continue;
                }
            }
        #endif             
        vec4 lightDir;
        vec3 lightVec;
        lightComputeDir(wPosition, lightColor.w, lightData1, lightDir, lightVec);

        float spotFallOff = 1.0;
        #if __VERSION__ >= 110
        if (lightColor.w > 1.0) {
        #endif
            #if USE_LIGHT_TEXTURES
                spotFallOff = computeSpotFalloff(texture2D(m_LightTex3, pixel), lightVec);
            #else
                spotFallOff = computeSpotFalloff(m_LightData[i+2], lightVec);
            #endif
        #if __VERSION__ >= 110
        }
        #endif
        //point light attenuation
        spotFallOff *= lightDir.w;

        lightDir.xyz = normalize(lightDir.xyz);            
        vec3 directDiffuse;
        vec3 directSpecular;
        float roughness = 1.0;
        
        PBR_ComputeDirectLight(normal, lightDir.xyz, viewDir, lightColor.rgb,
                fZero, roughness, ndotv, directDiffuse, directSpecular);

        vec3 directLighting = diffuseColor.rgb * directDiffuse /*+ directSpecular*/;
        result.rgb += directLighting * spotFallOff;
        
    }

    #if defined(EMISSIVE) || defined (EMISSIVEMAP)
        #ifdef EMISSIVEMAP
            vec3 emissive = texture2D(m_EmissiveMap, texCoord).rgb;
            #ifdef EMISSIVE
                emissive *= m_Emissive;
            #endif
        #else
            vec3 emissive = m_Emissive.rgb;
        #endif
        result.rgb += emissive * pow(emissive.a, m_EmissivePower) * m_EmissiveIntensity;
    #endif
    
    // todo: temporal voxel cone tracing to simulate "infinite" bounces
    
    // debug
    gl_FragColor = vec4(vPosition, 1.0);
    result = vec4(255.0);
    
    // this will likely produce flickering
    //ivec3 gridSize = imageSize(m_VoxelMap);
    imageStore(m_VoxelMap, ivec3(m_GridSize * vPosition), result);
    //imageAtomicMax(m_VoxelMap, ivec3(m_GridSize * vPosition), result);
   
}






