package com.example.fps_raytrace

import org.intellij.lang.annotations.Language

@Language("AGSL")
val emptyShader = """
   uniform shader composable; // The base image shader
   uniform float time; // Time uniform for animated noise
   uniform float noiseIntensity; // Intensity of the noise effect
   uniform float displacement; // Chromatic aberration displacement
   uniform float brightness; // Brightness of the final image
   uniform float2 resolution; // Screen resolution
   
   half4 main(float2 fragCoord) {
       float3 color = composable.eval(fragCoord).rgb;
       return half4(color, 1.0);
   }
""".trimIndent()

@Language("AGSL")
val analogShader = """
   uniform shader composable; // The base image shader
   uniform float time; // Time uniform for animated noise
   uniform float noiseIntensity; // Intensity of the noise effect
   uniform float displacement; // Chromatic aberration displacement
   uniform float brightness; // Brightness of the final image
   uniform float2 resolution; // Screen resolution

   half rand(float2 coord) {
       // A pseudo-random function based on the coordinate
       return fract(sin(dot(coord.xy, float2(12.9898, 78.233))) * 43758.5453);
   }

   half4 main(float2 fragCoord) {
       // Normalize coordinates
       float2 uv = fragCoord / resolution;

       // Sample the base color
       half3 baseColor = composable.eval(fragCoord).rgb;

       // Apply brightness adjustment
       baseColor *= brightness;

       // Apply chromatic aberration with proper displacement
       half r = composable.eval(fragCoord + float2(displacement, 0.0)).r;
       half g = composable.eval(fragCoord).g;
       half b = composable.eval(fragCoord - float2(displacement, 0.0)).b;      
       half3 aberratedColor = half3(r, g, b);

       // Generate noise based on fragCoord and time
       half noise = rand(fragCoord + time);

       // Scale the noise intensity and apply it to the RGB channels
       noise = (noise - 0.5) * noiseIntensity;
       half3 noisyColor = aberratedColor + noise;

       // Apply vignette effect
       float radius = 0.8;
       float softness = 0.5;
       float dist = distance(uv, float2(0.5, 0.5)); // Distance from the center
       float vignette = smoothstep(radius, radius - softness, dist);
       noisyColor *= vignette;

       // Clamp the final color to ensure it stays within valid range
       noisyColor = clamp(noisyColor, 0.0, 1.0);

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









