# 🎨 ETTA - Event-Triggered Texture Animation

> Dynamic texture animations for Minecraft that react to gameplay events

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8-green.svg)](https://minecraft.net)
[![Fabric](https://img.shields.io/badge/Mod%20Loader-Fabric-orange.svg)](https://fabricmc.net)

**ETTA** makes your Minecraft textures come alive! Items, blocks, and UI elements can change based on player health, events, time of day, weather, and much more.

## ✨ Features

- **Event-Based Animations** - Textures respond to player actions
- **Easy to Use** - Simple text-based animation format
- **Performance** - Efficient GPU uploads, minimal overhead
- **Fully Compatible** - Works with vanilla `.mcmeta` files

##  Quick Preview

**Totem of Undying** - Pulses faster when health is low:

```mcmetax
[animation]
frametime: 2

[fallback]
frame: 0

# Critical health - fast red pulse
[segment: critical]
type: sequence
frames: 10-15
priority: 200
frametime: 1
when: health <= 2

# Low health - medium pulse
[segment: low]
type: sequence
frames: 5-9
priority: 100
when: health <= 5

# Normal - slow idle
[segment: idle]
type: sequence
frames: 0-4
priority: 10
```

## Getting Started
### Creating Your First Animation

1. **Create the folder structure:**
   ```
   resourcepacks/my_pack/
   └── assets/
   └── minecraft/
   └── textures/
   └── item/
   ├── totem_of_undying.etta/
   │   ├── totem_of_undying.mcmetax    # Animation definition
   │   └── frames/
   │       ├── 0.png                    # Frame images
   │       ├── 1.png
   │       └── ...
   └── diamond_sword.etta/
   ├── diamond_sword.mcmetax
   └── frames/
   └── ...
   ```

2. **Write the animation file** (`diamond_sword.mcmetax`):
   ```mcmetax
   [animation]
   frametime: 2
   
   [fallback]
   frame: 0
   
   [segment: glowing]
   type: sequence
   frames: 0-2
   when: event(holding_item)
   ```

3. **Add your frame images** (0.png, 1.png, 2.png)

4. **Enable your resource pack** in Minecraft

## 📖 Documentation

### Frame Specifications

```mcmetax
# Single frame
frame: 5

# Range
frames: 0-15

# Array
frames: [0, 2, 4, 6, 8]

# Step (every 2nd frame)
frames: 0-20:2

# Mixed
frames: [0-5, 10-15, 20-25]
```

### Variables & Conditions

```mcmetax
[variables]
low_hp: 5
critical_hp: 2

[conditions]
in_danger: health <= $low_hp || event(player_on_fire)

[segment: danger]
when: $in_danger
```

### Built-in Events

| Event | Triggers When |
|-------|--------------|
| `player_sneaking` | Player is sneaking |
| `player_sprinting` | Player is sprinting |
| `player_attacking` | Player is attacking |
| `player_moving` | Player is moving |
| `hurt_recently` | Player took damage |
| `player_on_fire` | Player is on fire |
| `player_in_water` | Player is in water |
| `daytime` | It's day |
| `nighttime` | It's night |
| `raining` | It's raining |

[See all 40+ events →](docs/EVENTS.md)

### Functions

```mcmetax
# Random effects
when: random() < 0.1

# Event transitions
when: event_start(hurt_recently)

# Math
when: between(health, 5, 10)
when: min(health, hunger) < 5

# Game state
when: holding_item("sword")
when: in_biome("desert")
when: has_effect("regeneration")
```
See [guide about expressions](docs/GUIDE.md)

Also take a look at [examples](docs/EXAMPLES.md)

##  Controls
- **F8** - Toggle debug overlay

### Dependencies
-  Fabric API
-  Fabric Language Kotlin

##  License
This project is licensed under the MIT License - see [LICENSE](LICENSE) file.

## 🙏 Credits
[BasiqueEvangelist](https://github.com/BasiqueEvangelist) and his greatest invention - [ScaldingHot](https://github.com/BasiqueEvangelist/ScaldingHot) mod.
The whole project depends on single function he wrote that makes changing item texture in runtime without resource reloading screen possible! Thank you a lot, BasiqueEvangelist

##  Support
-  **Bug Reports**: [Issues](../../issues)

## 🎓 Learning Resources

- [ Complete Guide](docs/GUIDE.md) - In-depth tutorial
- [ Expression Reference](docs/EXPRESSIONS.md) - All functions & operators
- [ Example Pack](docs/EXAMPLES.md) - Download ready-to-use examples

## 🌟 Showcase

Show us what you've created! Tag `#ETTAAnimations` on social media.

[//]: # (### Community Creations)

[//]: # ()
[//]: # (- **Reactive Health Bars** by @user1)

[//]: # (- **Weather-Aware Compass** by @user2)

[//]: # (- **Combat Effects Pack** by @user3)

[See more →](SHOWCASE.md)

##  Todo List:

- [x] Basic event system
- [x] Expression evaluator
- [x] Hot reload
- [x] Advanced functions
- [ ] Visual editor (GUI)
- [ ] More built-in events
- [ ] Animation templates
- [ ] Resource pack generator

##  Known Issues

- Hot reload may not work with zipped resource packs
- Some texture atlas configurations not supported
- Debug overlay has minor rendering issues

See [Issues](../../issues) for full list.

<div align="center">

**Made with ❤️ for the Minecraft community**

⭐ **Star this repo if you find it useful!** ⭐

[Report Bug](../../issues) · [Request Feature](../../issues) · [Discuss](../../discussions)

</div>