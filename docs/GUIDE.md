# 📘 ETTA Complete Guide

Welcome to ETTA! This guide will take you from beginner to advanced user.

## Table of Contents

1. [Introduction](#introduction)
2. [Installation](#installation)
3. [Your First Animation](#your-first-animation)
4. [Understanding MCMETAX Format](#understanding-mcmetax-format)
5. [Frame Organization](#frame-organization)
6. [Animation Segments](#animation-segments)
7. [Events & Conditions](#events--conditions)
8. [Variables & Reusability](#variables--reusability)
9. [Priority System](#priority-system)
10. [Advanced Techniques](#advanced-techniques)
11. [Hot Reload](#hot-reload)
12. [Troubleshooting](#troubleshooting)
13. [Best Practices](#best-practices)

---

## Introduction

### What is ETTA?

ETTA (Event-Triggered Texture Animation) is a Minecraft mod that makes textures dynamic and responsive to gameplay. Instead of just looping animations, items and blocks can change based on:

- **Player health and hunger**
- **Events** (sneaking, combat, weather)
- **Environment** (time, biome, light level)
- **Custom conditions** you define

### Why Use ETTA?

- 🎨 **Immersive Experience** - Items feel alive and reactive
- 🎮 **Gameplay Feedback** - Visual cues for game state
- 🔧 **Easy to Create** - Simple text-based format
- ⚡ **Performant** - Minimal overhead, optimized GPU uploads
- 🔥 **Hot Reload** - Edit while playing

### What You Can Create

- Health-responsive items (glow when low HP)
- Combat-reactive weapons (flash on attack)
- Environmental items (clock showing time, compass spinning)
- Status indicators (food showing hunger level)
- Dynamic tools (different states based on use)

---

## Installation

### Requirements

- **Minecraft** 1.21.8 or newer
- **Fabric Loader** (latest version)
- **Fabric API** (latest version)
- **Fabric Language Kotlin** (latest version)

### Steps

1. **Download Fabric Loader**
    - Visit [fabricmc.net](https://fabricmc.net/use/)
    - Download the installer
    - Run and install for your Minecraft version

2. **Install Dependencies**
    - Download [Fabric API](https://modrinth.com/mod/fabric-api)
    - Download [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin)
    - Place both in your `mods` folder

3. **Install ETTA**
    - Download ETTA from [Releases](../../releases)
    - Place in your `mods` folder

4. **Launch Minecraft**
    - Start Minecraft with Fabric profile
    - ETTA will load automatically

5. **Verify Installation**
    - Press **F8** in-game
    - You should see "ETTA Debug: ON" message

---

## Your First Animation

Let's create a simple glowing sword that pulses when held.

### Step 1: Create Resource Pack Structure

```
resourcepacks/
└── my_first_pack/
    ├── pack.mcmeta
    └── assets/
        └── minecraft/
            └── textures/
                └── item/
                    └── diamond_sword.etta/
                        ├── diamond_sword.mcmetax
                        └── frames/
                            ├── 0.png
                            ├── 1.png
                            └── 2.png
```

### Step 2: Create pack.mcmeta

```json
{
  "pack": {
    "pack_format": 34,
    "description": "My First ETTA Pack"
  }
}
```

### Step 3: Create Frame Images

Create three 16×16 PNG images in the `frames/` folder:

- **0.png** - Normal sword
- **1.png** - Slight glow
- **2.png** - Full glow

*You can use any image editor (Paint.NET, GIMP, Photoshop, etc.)*

### Step 4: Write the Animation

Create `diamond_sword.mcmetax`:

```mcmetax
# My first ETTA animation!
[animation]
frametime: 2

[fallback]
frame: 0

[segment: glow]
type: sequence
frames: 0-2
loop: true
priority: 10
when: event(holding_item)
```

### Step 5: Enable and Test

1. In Minecraft, go to **Options → Resource Packs**
2. Enable **my_first_pack**
3. Press **Done** (Minecraft will reload resources)
4. Hold a diamond sword
5. Press **F8** to see debug info
6. Watch it glow!

### What Just Happened?

- **[animation]** - Set default frame timing to 2 ticks
- **[fallback]** - Show frame 0 when nothing else is active
- **[segment: glow]** - Defined an animation segment
- **frames: 0-2** - Use frames 0, 1, 2 in sequence
- **when: event(holding_item)** - Only when holding an item

---

## Understanding MCMETAX Format

### File Structure

MCMETAX files are organized into **sections**:

```mcmetax
[animation]          # Global settings
[variables]          # Reusable values
[conditions]         # Reusable logic
[fallback]          # Default frame
[segment: name]     # Animation segments
```

### Sections Explained

#### [animation]
Global animation settings:

```mcmetax
[animation]
frametime: 2        # Default ticks per frame (20 ticks = 1 second)
interpolate: false  # Smooth transitions (optional)
```

#### [variables]
Define reusable values:

```mcmetax
[variables]
low_hp: 5
critical_hp: 2
glow_frames: [0-10, 15-20]
```

#### [conditions]
Define reusable logic:

```mcmetax
[conditions]
in_danger: health <= 5 || event(player_on_fire)
```

#### [fallback]
The default frame when no segments are active:

```mcmetax
[fallback]
frame: 0            # Show frame 0 by default
```

#### [segment: name]
Named animation segments with conditions:

```mcmetax
[segment: low_health]
type: sequence
frames: 10-15
priority: 100
when: health < 5
```

---

## Frame Organization

### Frame Naming

Frames must be named sequentially starting from 0:

```
frames/
├── 0.png    # First frame
├── 1.png    # Second frame
├── 2.png    # Third frame
└── ...
```

### Frame Specifications

#### Single Frame
```mcmetax
frame: 5
```

#### Range
```mcmetax
frames: 0-15    # Frames 0, 1, 2, ..., 15
```

#### Array
```mcmetax
frames: [0, 2, 4, 6, 8]    # Specific frames only
```

#### Step Syntax
```mcmetax
frames: 0-20:2    # Every 2nd frame (0, 2, 4, 6, ...)
```

#### Mixed
```mcmetax
frames: [0-5, 10, 15-20]    # Multiple ranges
```

### Frame Resolution

All frames for an animation must be the **same size** (typically 16×16 for items).

---

## Animation Segments

### Segment Types

#### Sequence
Plays frames in order, can loop:

```mcmetax
[segment: pulse]
type: sequence
frames: 0-10
loop: true          # Repeats
priority: 10
```

#### Single Frame
Shows one static frame:

```mcmetax
[segment: critical]
type: single
frame: 15
priority: 200
when: health <= 2
```

#### Transition
Plays once when triggered:

```mcmetax
[segment: flash]
type: transition
frames: 20-25
priority: 500
when: event_start(hurt_recently)
```

#### Oneshot (Deprecated)
Plays once then stops (use sequence with loop: false instead):

```mcmetax
[segment: effect]
type: oneshot
frames: 30-35
```

### Segment Properties

```mcmetax
[segment: example]
type: sequence           # Type of segment
frames: 0-15            # Frame range/array
loop: true              # Loop animation (default: true)
priority: 100           # Higher = more important
frametime: 1            # Override global frametime
when: health < 5        # Condition to be active
```

---

## Events & Conditions

### Using Events

Events are game states you can check:

```mcmetax
when: event(player_sneaking)
```

**Common Events:**
- `player_sneaking`, `player_sprinting`, `player_moving`
- `player_attacking`, `hurt_recently`
- `player_on_fire`, `player_in_water`
- `daytime`, `nighttime`, `raining`
- `low_health`, `critical_health`

[See all events →](EVENTS.md)

### Combining Conditions

#### AND (`&&`)
Both must be true:
```mcmetax
when: health < 5 && event(player_moving)
```

#### OR (`||`)
At least one must be true:
```mcmetax
when: event(daytime) || event(nighttime)
```

#### NOT (`!`)
Negate a condition:
```mcmetax
when: !event(player_moving)
```

### Expressions

Use math and comparisons:

```mcmetax
# Health below 25%
when: health <= max_health * 0.25

# Either stat low
when: min(health, hunger) < 5

# In range
when: between(health, 5, 10)
```

[See all functions →](EXPRESSIONS.md)

---

## Variables & Reusability

### Defining Variables

```mcmetax
[variables]
low_hp: 5
critical_hp: 2
pulse_frames: [0, 1, 2, 1]
fast_speed: 1
```

### Using Variables

Reference with `$`:

```mcmetax
[segment: low]
frames: $pulse_frames
frametime: $fast_speed
when: health < $low_hp
```

### Defining Conditions

```mcmetax
[conditions]
in_danger: health <= 5 || event(player_on_fire)
in_combat: event(player_attacking) || event(hurt_recently)
```

### Using Conditions

```mcmetax
[segment: danger]
when: $in_danger && $in_combat
```

### Benefits

✅ **DRY Principle** - Don't repeat yourself
✅ **Easy Updates** - Change once, affects all
✅ **Readability** - Descriptive names
✅ **Maintainability** - Cleaner code

---

## Priority System

### How Priority Works

When multiple segments could be active, the **highest priority** wins.

```mcmetax
[segment: critical]
priority: 300        # Highest - always shows when active
when: health <= 2

[segment: low]
priority: 200        # Medium
when: health <= 5

[segment: normal]
priority: 10         # Lowest - default
```

### Priority Guidelines

| Priority | Use Case | Example |
|----------|----------|---------|
| 1000+ | Emergency/Critical | Near death, extreme danger |
| 500-999 | Transitions/Flashes | Attack flash, damage burst |
| 200-499 | Important States | Low health, combat mode |
| 100-199 | Normal States | Moving, holding item |
| 10-99 | Default/Idle | Normal operation |
| -1000 | Fallback | Default when nothing matches |

### Example Priority Chain

```mcmetax
[fallback]
frame: 0
# Priority: -1000 (implicit)

[segment: idle]
frames: 0-4
priority: 10
# Shows by default

[segment: moving]
frames: 5-10
priority: 50
when: event(player_moving)
# Overrides idle when moving

[segment: combat]
frames: 15-20
priority: 200
when: event(player_attacking)
# Overrides moving in combat

[segment: critical]
frames: 25-30
priority: 500
when: health <= 2
# Always shows at critical health
```

---

## Advanced Techniques

### Multiline Conditions

For complex logic:

```mcmetax
when:
    health < 5 &&
    (event(player_on_fire) || event(player_in_lava)) &&
    !event(player_swimming) &&
    armor_value() < 10
```

### Event Transitions

Trigger effects on state changes:

```mcmetax
# Flash when damaged
[segment: damage_flash]
type: transition
frames: 50-55
priority: 500
when: event_start(hurt_recently)

# Heal effect when damage ends
[segment: heal_glow]
type: transition
frames: 60-65
priority: 400
when: event_end(hurt_recently)
```

### Random Effects

Add variety:

```mcmetax
# 10% chance per tick
when: random() < 0.1

# Rare sparkle when idle
[segment: sparkle]
frames: 40-45
priority: 150
when: random() < 0.05 && event(player_idle)
```

### Time-Based Triggers

Use `time_in_state()`:

```mcmetax
# After idle for 5 seconds (100 ticks)
[segment: long_idle]
frames: 50-60
when: event(player_idle) && time_in_state() > 100
```

### Conditional Frame Selection

Different frames for different conditions:

```mcmetax
[variables]
critical: 2
low: 5
medium: 10

[segment: critical_state]
frames: 20-25
priority: 300
when: health <= $critical

[segment: low_state]
frames: 15-19
priority: 200
when: health <= $low && health > $critical

[segment: medium_state]
frames: 10-14
priority: 100
when: health <= $medium && health > $low
```

---

## Hot Reload

### What is Hot Reload?

Edit your animations **while Minecraft is running** - changes apply instantly!

### What Can Be Hot Reloaded?

✅ `.mcmetax` files (animation logic)
✅ Frame images (`.png` files)
✅ Variables and conditions
✅ Segment properties

### How to Use

1. **Start Minecraft** with your resource pack
2. **Edit files** in your text editor
3. **Save changes**
4. **Wait 500ms** (debounce delay)
5. **Changes apply automatically!**

### Example Workflow

1. Create initial animation
2. Load resource pack
3. Open `.mcmetax` in text editor
4. Tweak frame timings
5. Save file
6. See changes in-game immediately!

### Limitations

⚠️ **Zipped resource packs** cannot be hot reloaded
⚠️ **First load** requires resource pack reload (F3+T)
⚠️ **Structural changes** (adding new segments) work best

### Debug Tips

- Press **F8** to see active segments
- Check debug overlay for current frame
- Watch console for reload messages

---

## Troubleshooting

### Animation Not Showing

**Problem:** Texture doesn't animate

**Solutions:**
1. Check file structure:
   ```
   textures/item/diamond_sword.etta/
   ├── diamond_sword.mcmetax  ✓ Correct name
   └── frames/
       ├── 0.png               ✓ Sequential
       ├── 1.png
       └── 2.png
   ```

2. Verify resource pack is enabled
3. Check console for errors
4. Press F8 - is your texture listed?

### Segment Not Activating

**Problem:** Condition never triggers

**Solutions:**
1. Test condition in debug:
   ```mcmetax
   # Simplify to test
   when: event(player_moving)  # Does this work?
   ```

2. Check priority conflicts:
   ```mcmetax
   # Higher priority blocking?
   [segment: test]
   priority: 1000  # Make very high to test
   ```

3. Verify event is active (F8 debug overlay)

### Wrong Frame Showing

**Problem:** Shows unexpected frame

**Solutions:**
1. Check priority order
2. Verify fallback frame exists
3. Test with simple condition
4. Check frame range is correct

### Hot Reload Not Working

**Problem:** Changes don't apply

**Solutions:**
1. Ensure resource pack is **unzipped**
2. Wait for debounce (500ms)
3. Check file saved correctly
4. Look for syntax errors in console

### Performance Issues

**Problem:** Game lags with animations

**Solutions:**
1. Reduce number of animated textures
2. Use simpler conditions
3. Increase frame timing
4. Check for `random()` spam

---

## Best Practices

### 1. Organize Your Code

```mcmetax
# ================================
# Health-Based Totem Animation
# Shows different states by health
# ================================

[animation]
frametime: 2

# === Variables ===
[variables]
critical_hp: 2
low_hp: 5

# === Conditions ===
[conditions]
in_danger: health <= $low_hp

# === Fallback ===
[fallback]
frame: 0

# === Segments ===
[segment: critical]
# ...
```

### 2. Use Descriptive Names

**Bad:**
```mcmetax
[segment: seg1]
[segment: seg2]
```

**Good:**
```mcmetax
[segment: critical_health_pulse]
[segment: low_health_glow]
```

### 3. Comment Your Logic

```mcmetax
# Flash red when player takes damage
[segment: damage_flash]
type: transition
frames: 50-55
priority: 500
when: event_start(hurt_recently)
```

### 4. Test Incrementally

```mcmetax
# Start simple
when: health < 5

# Add complexity
when: health < 5 && event(player_moving)

# Keep building
when:
    health < 5 &&
    event(player_moving) &&
    !event(player_swimming)
```

### 5. Use Priority Wisely

```mcmetax
# Emergency states (1000)
[segment: near_death]
priority: 1000

# Important states (500)
[segment: critical]
priority: 500

# Normal states (100)
[segment: active]
priority: 100

# Default (10)
[segment: idle]
priority: 10
```

### 6. Optimize Conditions

```mcmetax
# Bad: checks random every tick
when: random() < 0.01

# Good: limit checks
when:
    event(player_idle) &&
    time_in_state() > 100 &&
    random() < 0.1
```

### 7. Version Your Resource Packs

```
my_pack_v1.0/
my_pack_v1.1/
my_pack_v2.0/
```

Keep backups before major changes!

---

## Example Projects

### 1. Health-Responsive Totem

```mcmetax
[animation]
frametime: 2

[variables]
critical: 2
low: 5
medium: 10

[fallback]
frame: 0

[segment: critical]
type: sequence
frames: 20-30
priority: 300
frametime: 1
when: health <= $critical

[segment: low]
type: sequence
frames: 10-19
priority: 200
when: health <= $low

[segment: medium]
type: sequence
frames: 5-9
priority: 100
when: health <= $medium

[segment: idle]
type: sequence
frames: 0-4
priority: 10
```

### 2. Combat Sword

```mcmetax
[animation]
frametime: 2

[conditions]
in_combat: event(player_attacking) || event(hurt_recently)

[fallback]
frame: 0

[segment: strike_flash]
type: transition
frames: 30-35
priority: 500
when: event_start(player_attacking)

[segment: combat_glow]
type: sequence
frames: 20-29
priority: 300
frametime: 1
when: $in_combat

[segment: held_shimmer]
type: sequence
frames: 10-15
priority: 100
when: event(holding_item)

[segment: idle]
type: sequence
frames: 0-5
priority: 10
```

### 3. Environmental Compass

```mcmetax
[animation]
frametime: 1

[fallback]
frame: 0

[segment: storm_spin]
type: sequence
frames: 30-45
priority: 300
frametime: 1
when: event(thundering)

[segment: rain_wobble]
type: sequence
frames: 20-29
priority: 200
when: event(raining)

[segment: night_glow]
type: sequence
frames: 10-19
priority: 100
when: event(nighttime)

[segment: day_normal]
type: sequence
frames: 0-9
priority: 10
```

---

## Next Steps

🎓 **Learn More:**
- [Events Reference](EVENTS.md) - All 40+ events
- [Expression Reference](EXPRESSIONS.md) - Functions and operators

🎨 **Get Inspired:**
- [Example Pack](../examples/) - Ready-to-use animations
- [Showcase](../SHOWCASE.md) - Community creations

💬 **Get Help:**
- [Issues](../../issues) - Report bugs
- [Discussions](../../discussions) - Ask questions

---

<div align="center">

**Happy Animating! 🎨**

[← Back to README](../README.md)

</div>