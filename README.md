# FPS Raytrace

A first-person shooter game demo for Android using raycasting rendering with Jetpack Compose. Features include real-time 3D rendering, enemy AI with TensorFlow Lite, touch joystick controls, and visual effects.

![Platform](https://img.shields.io/badge/platform-Android-green.svg)
![Min SDK](https://img.shields.io/badge/minSdk-33-blue.svg)
![Target SDK](https://img.shields.io/badge/targetSdk-36-blue.svg)
![Language](https://img.shields.io/badge/language-Kotlin-purple.svg)

A retro-style FPS game demonstrating raycasting technology on Android with modern Compose UI.

## Requirements

- Android Studio Hedgehog or newer
- Android SDK 33+ (Android 13)
- Kotlin 1.9+
- Java 17

## Installation

1. Clone the repository
   ```bash
   git clone https://github.com/yourusername/FPS_raytrace.git
   ```

2. Open the project in Android Studio

3. Sync Gradle

4. Run on a device or emulator (API 33+)

Alternatively, download the latest APK from [Releases](https://github.com/yourusername/FPS_raytrace/releases) and install it on your device.

## Usage

- **Movement**: Use the on-screen joystick (bottom-left) or connect a keyboard
- **Look around**: Drag on the screen
- **Shoot**: Tap anywhere on the screen or press Spacebar
- **Keyboard controls**: Arrow keys for movement, Spacebar to shoot

### Controls

| Input | Action |
|-------|--------|
| Joystick (left) | Move forward/backward/strafe |
| Touch drag | Look around |
| Tap / Spacebar | Shoot |
| Arrow keys | Move (external keyboard) |

## Project Structure

```
FPS_raytrace/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/fps_raytrace/
│   │   │   ├── AIController.kt        # TensorFlow Lite AI controller
│   │   │   ├── MainActivity.kt       # Main game activity
│   │   │   ├── composable/          # Compose UI components
│   │   │   ├── engine/              # Game engine (raycasting, physics)
│   │   │   │   ├── raycaster/       # Raycasting renderer
│   │   │   │   ├── map/             # Map rendering
│   │   │   │   └── utils/           # Utilities (A*, sound, sprites)
│   │   │   ├── maps/                # Game maps
│   │   │   ├── sprites/             # Game sprites (weapons, enemies)
│   │   │   └── ui/theme/            # Compose theming
│   │   └── res/                     # Resources (sounds, textures)
│   └── build.gradle.kts
├── AI/                               # AI training scripts
├── baselineprofile/                  # Baseline profile configuration
└── build.gradle.kts
```

## Key Features

- **Raycasting Renderer**: Wolfenstein 3D-style rendering engine
- **Jetpack Compose UI**: Modern declarative UI with joystick controls
- **TensorFlow Lite AI**: Enemy AI powered by machine learning
- **Shader Effects**: Custom render effects (glitch, noise)
- **Sprite System**: Enemies and weapons with depth sorting
- **A* Pathfinding**: Intelligent enemy navigation
- **Sound System**: Background music and sound effects
- **Touch Controls**: Virtual joystick for mobile gameplay

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

## Running Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

## Dependencies

- Jetpack Compose (BOM)
- AndroidX Core KTX
- AndroidX Lifecycle
- TensorFlow Lite
- Material 3

## License

MIT © Bartosz Szczygiel
