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
   uniform float intensity; // Intensity of the noise effect

   half4 main(float2 fragCoord) {
       // Normalize coordinates
       float2 uv = fragCoord / resolution;

       // Random noise for glitch effect
       float noise = fract(sin(dot(uv * time, float2(12.9898, 78.233))) * 43758.5453);
       float glitchStrength =intensity * (sin(time * 2.0) * 0.5 + 0.5)/2.0; // Oscillating glitch intensity
       
       // Horizontal jitter
       float jitter = (intensity * sin(5.0 + fragCoord.y * 0.1) * 0.005) * resolution.x * glitchStrength;

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
       glitchColor.r = composable.eval(float2(displacedCoord.x - 20.0 * noise, displacedCoord.y)).r;
       glitchColor.g = desaturatedColor.g;
       glitchColor.b = composable.eval(float2(displacedCoord.x + 20.0 * noise, displacedCoord.y)).b;

       return half4(glitchColor, 1.0);
   }
""".trimIndent()


@Language("AGSL")
val blackAndWhiteDitheringWithOutlineAndNoise = """
   uniform shader composable; // The base image shader
   uniform float time; // Time uniform for animated noise
   uniform float noiseIntensity; // Intensity of the noise effect
   uniform float displacement; // Chromatic aberration displacement
   uniform float brightness; // Brightness of the final image
   uniform float2 resolution; // Screen resolution

    // Simple random noise function based on fragCoord
    float random(float2 st) {
        return fract(sin(dot(st.xy, float2(12.9898, 78.233))) * 43758.5453123);
    }

    half4 main(float2 fragCoord) {
        // Normalize coordinates
        float2 uv = fragCoord / resolution;

        // Sample the base color
        half3 baseColor = composable.eval(fragCoord).rgb;

        // Convert to grayscale
        half gray = dot(baseColor, half3(0.299, 0.587, 0.114));

        // Edge detection using Sobel filter
        float2 texel = 1.0 / resolution;
        half3 north = composable.eval(fragCoord + float2(0.0, -texel.y)).rgb;
        half3 south = composable.eval(fragCoord + float2(0.0, texel.y)).rgb;
        half3 east = composable.eval(fragCoord + float2(texel.x, 0.0)).rgb;
        half3 west = composable.eval(fragCoord + float2(-texel.x, 0.0)).rgb;

        // Grayscale neighbors
        half northGray = dot(north, half3(0.299, 0.587, 0.114));
        half southGray = dot(south, half3(0.299, 0.587, 0.114));
        half eastGray = dot(east, half3(0.299, 0.587, 0.114));
        half westGray = dot(west, half3(0.299, 0.587, 0.114));

        // Calculate edge strength
        half edgeStrength = abs(northGray - southGray) + abs(eastGray - westGray);
        half outline = edgeStrength > 0.2 ? 1.0 : 0.0; // Edge threshold

        // Scale the coordinates down to create a low-resolution effect
        float2 lowResCoord = floor(fragCoord / 4.0) * 4.0;

        // Dithering pattern (Bayer 2x2 matrix) values
        float ditherThreshold;
        if (mod(lowResCoord.y / 4.0, 2.0) == 0.0) {
            if (mod(lowResCoord.x / 4.0, 2.0) == 0.0) {
                ditherThreshold = 0.1; // Top-left
            } else {
                ditherThreshold = 0.2; // Top-right
            }
        } else {
            if (mod(lowResCoord.x / 4.0, 2.0) == 0.0) {
                ditherThreshold = 0.75; // Bottom-left
            } else {
                ditherThreshold = 0.15; // Bottom-right
            }
        }

        // Add random noise to the dithering threshold
        float noise = random(fragCoord * time) * noiseIntensity; // Scaled noise based on time and intensity
        ditherThreshold += noise;

        // Apply dithering: map grayscale to black or white based on the noisy threshold
        half bwColor = gray > ditherThreshold ? 1.0 : 0.0;

        // Combine outline with the dithered black-and-white color
        bwColor = max(bwColor, outline);

        // Return the final color with alpha preserved
        return half4(bwColor, bwColor, bwColor, 1.0);
    }
""".trimIndent()








