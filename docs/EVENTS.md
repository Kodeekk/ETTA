# 📋 ETTA Events Reference

Complete list of all built-in events you can use in your animations.

## Table of Contents

- [Player Movement](#player-movement)
- [Player State](#player-state)
- [Environmental](#environmental)
- [Time & Weather](#time--weather)
- [Combat](#combat)
- [Health & Hunger](#health--hunger)
- [Equipment](#equipment)
- [Dimensions](#dimensions)
- [UI State](#ui-state)

---

## Player Movement

Events related to player movement and position.

| Event | Description | Example Use |
|-------|-------------|-------------|
| `player_moving` | Player is moving horizontally | Compass spinning |
| `player_sneaking` | Player is crouching | Stealth indicator |
| `player_sprinting` | Player is sprinting | Speed lines effect |
| `player_swimming` | Player is swimming | Water droplets |
| `player_jumping` | Player is jumping (in air with upward momentum) | Jump boost visual |
| `player_falling` | Player is falling (in air with downward momentum) | Feather falling effect |
| `player_flying` | Player is flying (creative/spectator mode) | Wing animation |
| `player_airborne` | Player is not on ground | Any aerial state |
| `player_idle` | Player is not moving and on ground | Idle breathing animation |
| `player_crouch_walking` | Player is sneaking while moving | Sneaky footsteps |
| `on_ground_stationary` | Player on ground and not moving | Standing still |

**Example:**
```mcmetax
[segment: running]
type: sequence
frames: 0-10
when: event(player_sprinting)

[segment: sneaking]
type: sequence
frames: 11-15
when: event(player_sneaking)
```

---

## Player State

Events about the player's current condition.

| Event | Description | Example Use |
|-------|-------------|-------------|
| `player_in_water` | Player is in water | Wet texture |
| `player_underwater` | Player's head is underwater | Bubbles effect |
| `player_in_lava` | Player is in lava | Melting effect |
| `player_on_fire` | Player is burning | Fire overlay |
| `invisible` | Player has invisibility effect | Ghostly appearance |
| `has_effects` | Player has any potion effects | Magical glow |
| `invulnerable` | Player is invulnerable | Shield effect |

**Example:**
```mcmetax
[segment: burning]
type: sequence
frames: 20-25
priority: 300
frametime: 1
when: event(player_on_fire)

[segment: underwater]
type: sequence
frames: 30-35
priority: 200
when: event(player_underwater)
```

---

## Environmental

Events based on the environment.

| Event | Description | Example Use |
|-------|-------------|-------------|
| `daytime` | It's daytime (bright) | Sun icon |
| `nighttime` | It's nighttime (dark) | Moon icon |
| `sunrise` | Dawn (23000-1000 ticks) | Orange glow |
| `sunset` | Dusk (12000-13000 ticks) | Red glow |
| `midnight` | Middle of night (18000-19000 ticks) | Star twinkle |
| `raining` | It's raining | Water drops |
| `thundering` | Thunderstorm active | Lightning flash |

**Example:**
```mcmetax
[segment: day_mode]
type: sequence
frames: 0-11
when: event(daytime)

[segment: night_mode]
type: sequence
frames: 12-23
when: event(nighttime)

[segment: rain_drops]
type: sequence
frames: 24-30
when: event(raining)
```

---

## Time & Weather

Time-specific events for clocks and weather-responsive items.

| Event | Description | Game Time |
|-------|-------------|-----------|
| `daytime` | Daylight hours | 0-12000 |
| `nighttime` | Night hours | 12000-24000 |
| `sunrise` | Dawn transition | 23000-1000 |
| `sunset` | Dusk transition | 12000-13000 |
| `midnight` | Middle of night | 18000-19000 |
| `raining` | Rain/snow falling | Weather state |
| `thundering` | Thunder and lightning | Weather state |

**Example Clock:**
```mcmetax
[animation]
frametime: 1

[segment: sunrise_anim]
type: sequence
frames: 0-5
priority: 200
when: event(sunrise)

[segment: day]
type: sequence
frames: 6-17
when: event(daytime)

[segment: sunset_anim]
type: sequence
frames: 18-23
priority: 200
when: event(sunset)

[segment: night]
type: sequence
frames: 24-35
when: event(nighttime)
```

---

## Combat

Events related to combat and damage.

| Event | Description | Example Use |
|-------|-------------|-------------|
| `player_attacking` | Player is swinging weapon (first 3 ticks) | Attack flash |
| `hurt_recently` | Player took damage recently (hurt animation) | Damage indicator |
| `near_death` | Player health ≤ 2 hearts | Critical warning |

**Example:**
```mcmetax
[segment: attack_flash]
type: transition
frames: 50-55
priority: 500
when: event_start(player_attacking)

[segment: hurt_indicator]
type: sequence
frames: 40-45
priority: 400
frametime: 1
when: event(hurt_recently)

[segment: death_warning]
type: sequence
frames: 30-35
priority: 600
frametime: 1
when: event(near_death)
```

---

## Health & Hunger

Events based on player stats.

| Event | Description | Condition |
|-------|-------------|-----------|
| `low_health` | Health is low | health/max_health ≤ 0.25 |
| `critical_health` | Health is critical | health/max_health ≤ 0.1 |
| `full_health` | At maximum health | health ≥ max_health |
| `hungry` | Hunger is low | hunger < 10 |
| `starving` | Hunger is critical | hunger ≤ 6 |
| `full_hunger` | Hunger is full | hunger = 20 |

**Example:**
```mcmetax
[variables]
critical_hp: 2
low_hp: 5

[segment: critical]
type: sequence
frames: 20-30
priority: 300
frametime: 1
when: event(critical_health)

[segment: low]
type: sequence
frames: 10-19
priority: 200
when: event(low_health)

[segment: starving]
type: sequence
frames: 40-50
priority: 250
when: event(starving)
```

---

## Equipment

Events about what the player is wearing/holding.

| Event | Description | Example Use |
|-------|-------------|-------------|
| `wearing_helmet` | Has helmet equipped | Head protection indicator |
| `wearing_elytra` | Has elytra equipped | Flight ready |
| `armor_full` | All armor slots filled | Full protection |
| `armor_empty` | No armor equipped | Vulnerable state |
| `holding_item` | Holding item in main hand | Active item glow |
| `holding_block` | Holding a block | Placement ready |

**Example:**
```mcmetax
[segment: protected]
type: sequence
frames: 0-5
priority: 100
when: event(armor_full)

[segment: vulnerable]
type: sequence
frames: 10-15
priority: 100
when: event(armor_empty)

[segment: active]
type: sequence
frames: 20-25
when: event(holding_item)
```

---

## Dimensions

Events for different Minecraft dimensions.

| Event | Description | Dimension |
|-------|-------------|-----------|
| `overworld` | In the Overworld | Overworld |
| `in_nether` | In the Nether | Nether |
| `in_end` | In the End | The End |

**Example:**
```mcmetax
[segment: nether_glow]
type: sequence
frames: 30-40
priority: 200
when: event(in_nether)

[segment: end_particles]
type: sequence
frames: 41-50
priority: 200
when: event(in_end)

[segment: overworld_normal]
type: sequence
frames: 0-10
priority: 10
when: event(overworld)
```

---

## UI State

Events related to UI and camera.

| Event | Description | Example Use |
|-------|-------------|-------------|
| `first_person` | First-person camera | FP-specific effects |
| `third_person` | Third-person camera | TP-specific effects |
| `gui_open` | Any GUI is open | Interface mode |
| `chat_open` | Chat is open | Typing indicator |

**Example:**
```mcmetax
[segment: fp_view]
type: sequence
frames: 0-5
when: event(first_person)

[segment: tp_view]
type: sequence
frames: 6-10
when: event(third_person)
```

---

## Event Combinations

You can combine events using logical operators:

### AND (`&&`)
Both conditions must be true:
```mcmetax
when: event(player_sneaking) && event(player_moving)
```

### OR (`||`)
At least one condition must be true:
```mcmetax
when: event(player_on_fire) || event(player_in_lava)
```

### NOT (`!`)
Negates a condition:
```mcmetax
when: !event(player_moving)  # Player is NOT moving
```

### Complex Examples

```mcmetax
# Sneaking in danger
when: 
    event(player_sneaking) && 
    (event(player_on_fire) || event(player_in_lava))

# Moving fast in overworld during day
when:
    event(player_sprinting) &&
    event(overworld) &&
    event(daytime)

# Safe and idle
when:
    event(player_idle) &&
    event(full_health) &&
    !event(has_effects)
```

---

## Event Transitions

Special functions that trigger only on state changes:

### `event_start(name)`
Triggers when event activates (false → true):
```mcmetax
[segment: damage_flash]
type: transition
frames: 50-55
priority: 500
when: event_start(hurt_recently)
```

### `event_end(name)`
Triggers when event deactivates (true → false):
```mcmetax
[segment: heal_effect]
type: transition
frames: 60-65
priority: 400
when: event_end(hurt_recently)
```

**Use Cases:**
- Flash effects on damage
- Sound/particle triggers
- State transition animations
- One-shot visual effects

---

## Custom Event Usage

### Health-Based Food Item
```mcmetax
[animation]
frametime: 2

[segment: critical_need]
type: sequence
frames: 20-30
priority: 300
frametime: 1
when: event(starving) && event(critical_health)

[segment: hungry]
type: sequence
frames: 10-19
priority: 200
when: event(hungry)

[segment: normal]
type: sequence
frames: 0-9
priority: 10
```

### Combat Sword
```mcmetax
[segment: strike_flash]
type: transition
frames: 40-45
priority: 500
when: event_start(player_attacking)

[segment: battle_glow]
type: sequence
frames: 30-39
priority: 300
when: event(hurt_recently) || event(player_attacking)

[segment: idle]
type: sequence
frames: 0-10
priority: 10
```

### Weather Compass
```mcmetax
[segment: storm]
type: sequence
frames: 30-40
priority: 200
frametime: 1
when: event(thundering)

[segment: rain]
type: sequence
frames: 20-29
priority: 150
when: event(raining)

[segment: clear]
type: sequence
frames: 0-19
priority: 10
when: !event(raining) && !event(thundering)
```

---

## Tips & Best Practices

### Priority Ordering
List events by importance (highest priority first):
```mcmetax
# Emergency (1000)
when: event(near_death)

# Critical (500)  
when: event(critical_health)

# Important (200)
when: event(low_health)

# Normal (100)
when: event(hurt_recently)

# Default (10)
when: event(player_moving)
```

### Event Grouping
Combine related events:
```mcmetax
[conditions]
in_danger: 
    event(player_on_fire) ||
    event(player_in_lava) ||
    event(near_death)

moving_fast:
    event(player_sprinting) ||
    event(player_swimming) ||
    event(player_flying)

[segment: danger_fast]
when: $in_danger && $moving_fast
```

### Debug Testing
Use F8 overlay to see:
- Which events are active
- Current frame being displayed
- Priority conflicts

---

## Event Performance

All events are checked **20 times per second** (once per tick).

**Performance Tips:**
- Events are highly optimized (< 0.01ms per check)
- Use priority to avoid unnecessary checks
- Complex expressions are cached
- No performance impact from number of events

---

## Future Events

Planned for future versions:

- `biome_specific` events (desert, ocean, etc.)
- `moon_phase` events
- `difficulty_level` events
- `game_mode` events
- `achievement_unlocked` events
- Custom event registration API

[Request new events →](../../issues)

---

<div align="center">

**[← Back to README](../README.md)** | **[Expression Reference →](EXPRESSIONS.md)**

</div>