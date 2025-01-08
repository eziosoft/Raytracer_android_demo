package com.example.fps_raytrace

import org.intellij.lang.annotations.Language


@Language("AGSL")
val analogShader = """
   uniform shader composable; // The base image shader
   uniform float time; // Time uniform for animated noise
   uniform float noiseIntensity; // Intensity of the noise effect
   uniform float displacement; // Chromatic aberration displacement
   uniform float brightness; // Brightness of the final image

   half rand(float2 coord) {
       // A pseudo-random function based on the coordinate
       return fract(sin(dot(coord.xy, float2(12.9898, 78.233))) * 43758.5453);
   }

   half4 main(float2 fragCoord) {
       half3 color = composable.eval(fragCoord).rgb;
       
       // Apply brightness
         color = color + color * brightness;
         
       // Apply chromatic aberration
       color.r = composable.eval(float2(fragCoord.x - displacement, fragCoord.y)).r;
       color.b = composable.eval(float2(fragCoord.x + displacement, fragCoord.y)).b;

       // Generate noise based on fragCoord and time
       half noise = rand(fragCoord + time);

       // Scale the noise intensity
       noise = (noise - 0.5) * noiseIntensity;

       // Apply noise to the RGB channels
       half3 noisyColor = color + noise;

       // Return the final color with alpha preserved
       return half4(noisyColor, 1.0);
   }
""".trimIndent()


@Language("AGSL")
val glitchShader = """
   uniform shader composable; // The base image shader
   uniform float time; // Time uniform for animated effects
   uniform float2 resolution; // Screen resolution

   half4 main(float2 fragCoord) {
       // Normalize coordinates
       float2 uv = fragCoord / resolution;

       // Random noise for glitch effect
       float noise = fract(sin(dot(uv * time, float2(12.9898, 78.233))) * 43758.5453);
       float glitchStrength = (sin(time * 2.0) * 0.5 + 0.5)/2.0; // Oscillating glitch intensity
       
       // Horizontal jitter
       float jitter = (sin(time * 5.0 + fragCoord.y * 0.1) * 0.005) * resolution.x * glitchStrength;

       // Vertical bands
       float verticalBand = step(0.9, fract(sin(fragCoord.x * 0.05 + time) * 43758.5453)) * 0.5;

       // Combine displacement for glitching
       float2 displacedCoord = float2(fragCoord.x + jitter, fragCoord.y + verticalBand * resolution.y * 0.1 * noise);

       // Desaturate for weak signal
       half3 baseColor = composable.eval(displacedCoord).rgb;
       float gray = dot(baseColor, half3(0.299, 0.587, 0.114));
       half3 desaturatedColor = mix(baseColor, half3(gray, gray, gray), 0.6); // Adjust desaturation

       // Add subtle scanline effect
       float scanline = 0.05 * sin(fragCoord.y * 5.0 + time * 10.0);
       desaturatedColor *= (1.0 + scanline);

       // Chromatic aberration (slight color channel offsets)
       half3 glitchColor;
       glitchColor.r = composable.eval(float2(displacedCoord.x - 10.0 * noise, displacedCoord.y)).r;
       glitchColor.g = desaturatedColor.g;
       glitchColor.b = composable.eval(float2(displacedCoord.x + 10.0 * noise, displacedCoord.y)).b;

       return half4(glitchColor, 1.0);
   }
""".trimIndent()






