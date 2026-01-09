# Quick specs of mcmetax

## File Structure
```mcmetax
[animation]           # Global settings
[variables]          # Reusable values  
[conditions]         # Reusable logic
[fallback]           # Default frame
[segment: name]      # Animation segment
```

## Frame Specifications

| Syntax | Result | Example |
|--------|--------|---------|
| `5` | Single frame | `frame: 5` |
| `0-15` | Range | `frames: 0-15` |
| `[0, 2, 4]` | Array | `frames: [0, 2, 4]` |
| `0-20:2` | Step | `frames: 0-20:2` (every 2nd) |
| `[0-5, 10-15]` | Mixed | Multiple ranges |
| `$var` | Variable | `frames: $my_frames` |

## Variables

```mcmetax
[variables]
low_hp: 5
frames_list: [0-10, 15-20]
speed: 1

[segment: test]
frames: $frames_list
frametime: $speed
when: health < $low_hp
```

## Conditions

```mcmetax
[conditions]
in_danger: health <= 5 || event(player_on_fire)
is_moving: event(player_moving)

[segment: test]
when: $in_danger && $is_moving
```

## Multiline Properties

```mcmetax
when:
    health < 5 &&
    (event(player_sneaking) || event(player_swimming)) &&
    hunger > 5
```

## Built-in Variables

| Variable | Description |
|----------|-------------|
| `health` | Player health |
| `max_health` | Max health |
| `hunger` | Hunger level |
| `first_frame` | First frame index |
| `last_frame` | Last frame index |

## Operators

| Type | Operators |
|------|-----------|
| Comparison | `<`, `>`, `<=`, `>=`, `==`, `!=` |
| Logical | `&&`, `\|\|`, `!` |
| Arithmetic | `+`, `-`, `*`, `/` |
| Grouping | `()` |

## Functions Reference

### Event Functions
```
event(name)              # Check if event active
event_start(name)        # Event just activated
event_end(name)          # Event just deactivated
```

### Math Functions
```
random()                 # 0.0 to 1.0
abs(value)              # Absolute value
min(a, b)               # Minimum
max(a, b)               # Maximum
between(val, min, max)  # Check range
```

### State Functions
```
time_in_state()         # Ticks in current state
frame_index()           # Current frame
cycle_count()           # Loop count
```

### Game State
```
holding_item(name)      # Check held item
in_biome(name)          # Check biome
has_effect(name)        # Check potion
armor_value()           # Total armor
light_level()           # Light at position
```

## Segment Types

```mcmetax
type: single            # Single frame
type: sequence          # Frame sequence
type: oneshot          # Play once
type: weighted         # Random weighted
type: conditional      # Conditional frames
type: transition       # Triggered animation
```

## Common Patterns

### Health-Based
```mcmetax
[segment: critical]
type: sequence
frames: 10-15
when: health <= 2
```

### Event-Based
```mcmetax
[segment: sneaking]
type: sequence
frames: 5-10
when: event(player_sneaking)
```

### Combined
```mcmetax
[segment: complex]
type: sequence
frames: 0-20
when: health < 5 && event(player_moving)
```

### With Variables
```mcmetax
[variables]
low: 5
frames: [0-10, 15-20]

[segment: test]
type: sequence
frames: $frames
when: health < $low
```

### Random Effect
```mcmetax
[segment: sparkle]
type: sequence
frames: 30-35
when: random() < 0.1
```

### Transition
```mcmetax
[segment: damage_flash]
type: transition
frames: 40-45
when: event_start(hurt_recently)
```

### Time-Based
```mcmetax
[segment: idle_long]
type: sequence
frames: 50-60
when: time_in_state() > 200
```

## Priority System

Higher priority = takes precedence

```mcmetax
[fallback]          # -1000 (lowest)
frame: 0

[segment: normal]   # 10 (low)
priority: 10

[segment: active]   # 100 (medium)
priority: 100

[segment: critical] # 500 (high)
priority: 500

[segment: flash]    # 1000 (highest)
priority: 1000
```

## Full Example Template

```mcmetax
# My Animation
[animation]
frametime: 2

[variables]
low_hp: 5
critical_hp: 2

[conditions]
in_danger: health <= $low_hp

[fallback]
frame: 0

[segment: critical]
type: sequence
frames: 10-15
priority: 200
frametime: 1
when: health <= $critical_hp

[segment: low]
type: sequence
frames: 5-9
priority: 100
when: $in_danger

[segment: normal]
type: sequence
frames: 0-4
priority: 10
```

## Common Events

```
player_sneaking
player_sprinting
player_swimming
player_jumping
player_falling
player_attacking
player_moving
player_idle
player_on_fire
player_in_water
player_in_lava
hurt_recently
low_health
critical_health
daytime
nighttime
raining
```

## Debugging

Enable debug overlay: **F8**

Shows:
- Active events
- Current frames
- Animation sources