package com.example.fps_raytrace.engine

import org.intellij.lang.annotations.Language


@Language("AGSL")
val analogShader = """
   uniform shader composable; // The base image shader
   uniform float time; // Time uniform for animated noise
   uniform float noiseIntensity; // Intensity of the noise effect

   half rand(float2 coord) {
       // A pseudo-random function based on the coordinate
       return fract(sin(dot(coord.xy, float2(12.9898, 78.233))) * 43758.5453);
   }

   half4 main(float2 fragCoord) {
       float displacement = 5.0; // Chromatic aberration displacement

       // Apply chromatic aberration
       half3 color = composable.eval(fragCoord).rgb;
       color.r = composable.eval(float2(fragCoord.x - displacement, fragCoord.y)).r;
       color.b = composable.eval(float2(fragCoord.x + displacement, fragCoord.y)).b;

       // Generate noise based on fragCoord and time
       half noise = rand(fragCoord + time);

       // Scale the noise intensity
       noise = (noise - 0.5) * noiseIntensity;

       // Apply noise to the RGB channels
       half3 noisyColor = color + half3(noise, noise, 0.0);

       // Return the final color with alpha preserved
       return half4(noisyColor, 1.0);
   }
""".trimIndent()


