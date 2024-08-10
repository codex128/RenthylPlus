#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"
#import "Common/ShaderLib/Skinning.glsllib"
#import "Common/ShaderLib/Lighting.glsllib"
#import "Common/ShaderLib/MorphAnim.glsllib"

#ifdef VERTEX_LIGHTING
    #import "Common/ShaderLib/BlinnPhongLighting.glsllib"
#endif

// fog - jayfella
#ifdef USE_FOG
varying float fog_distance;
uniform vec3 g_CameraPosition;
#endif

uniform vec4 m_Ambient;
uniform vec4 m_Diffuse;
uniform vec4 m_Specular;
uniform float m_Shininess;

#if defined(VERTEX_LIGHTING)
    uniform vec4 g_LightData[NB_LIGHTS];
#endif
varying vec2 texCoord;

#ifdef SEPARATE_TEXCOORD
  varying vec2 texCoord2;
  attribute vec2 inTexCoord2;
#endif

varying vec3 AmbientSum;
varying vec4 DiffuseSum;
varying vec3 SpecularSum;

attribute vec3 inPosition;
attribute vec2 inTexCoord;
attribute vec3 inNormal;

#ifdef VERTEX_COLOR
  attribute vec4 inColor;
#endif

#ifndef VERTEX_LIGHTING
    varying vec3 vNormal;
    varying vec3 vPos;
    #ifdef NORMALMAP
        attribute vec4 inTangent;
        varying vec4 vTangent;
    #endif
#else
    #ifdef COLORRAMP
      uniform sampler2D m_ColorRamp;
    #endif
#endif

#ifdef USE_REFLECTION
    uniform vec3 g_CameraPosition;
    uniform vec3 m_FresnelParams;
    varying vec4 refVec;

    /**
     * Input:
     * attribute inPosition
     * attribute inNormal
     * uniform g_WorldMatrix
     * uniform g_CameraPosition
     *
     * Output:
     * varying refVec
     */
    void computeRef(in vec4 modelSpacePos){
        // vec3 worldPos = (g_WorldMatrix * modelSpacePos).xyz;
        vec3 worldPos = TransformWorld(modelSpacePos).xyz;

        vec3 I = normalize( g_CameraPosition - worldPos  ).xyz;
        // vec3 N = normalize( (g_WorldMatrix * vec4(inNormal, 0.0)).xyz );
        vec3 N = normalize( TransformWorld(vec4(inNormal, 0.0)).xyz );

        refVec.xyz = reflect(I, N);
        refVec.w   = m_FresnelParams.x + m_FresnelParams.y * pow(1.0 + dot(I, N), m_FresnelParams.z);
    }
#endif

void main(){
   vec4 modelSpacePos = vec4(inPosition, 1.0);
   vec3 modelSpaceNorm = inNormal;
   
   #if  defined(NORMALMAP) && !defined(VERTEX_LIGHTING)
        vec3 modelSpaceTan  = inTangent.xyz;
   #endif

   #ifdef NUM_MORPH_TARGETS
        #if defined(NORMALMAP) && !defined(VERTEX_LIGHTING)
           Morph_Compute(modelSpacePos, modelSpaceNorm, modelSpaceTan);
        #else
           Morph_Compute(modelSpacePos, modelSpaceNorm);
        #endif
   #endif

   #ifdef NUM_BONES
        #if defined(NORMALMAP) && !defined(VERTEX_LIGHTING)
        Skinning_Compute(modelSpacePos, modelSpaceNorm, modelSpaceTan);
        #else
        Skinning_Compute(modelSpacePos, modelSpaceNorm);
        #endif
   #endif

   gl_Position = TransformWorldViewProjection(modelSpacePos);
   texCoord = inTexCoord;
   #ifdef SEPARATE_TEXCOORD
      texCoord2 = inTexCoord2;
   #endif

   vec3 wPosition = TransformWorld(modelSpacePos).xyz;
   vec3 wNormal  = normalize(TransformWorldNormal(modelSpaceNorm));

    #if (defined(NORMALMAP) || defined(PARALLAXMAP)) && !defined(VERTEX_LIGHTING)
      vTangent = vec4(TransformWorldNormal(modelSpaceTan).xyz,inTangent.w);
      vNormal = wNormal;
      vPos = wPosition;
    #elif !defined(VERTEX_LIGHTING)
      vNormal = wNormal;
      vPos = wPosition;
    #endif

    #ifdef MATERIAL_COLORS
        AmbientSum  = m_Ambient.rgb;
        SpecularSum = m_Specular.rgb;
        DiffuseSum = m_Diffuse;                   
    #else
        // Defaults: Ambient and diffuse are white, specular is black.
        AmbientSum  = vec3(1.0);
        SpecularSum = vec3(0.0);
        DiffuseSum = vec4(1.0);
    #endif
    #ifdef VERTEX_COLOR               
        AmbientSum *= inColor.rgb;
        DiffuseSum *= inColor;
    #endif

//    #ifdef USE_REFLECTION
//        computeRef(modelSpacePos);
//    #endif

    #ifdef USE_FOG
    fog_distance = distance(g_CameraPosition, (TransformWorld(modelSpacePos)).xyz);
    #endif
}