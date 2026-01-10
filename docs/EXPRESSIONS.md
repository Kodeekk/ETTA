# 📋 ETTA Expression Reference

Complete guide to writing conditions and expressions in MCMETAX animations.

## Table of Contents

- [Basics](#basics)
- [Variables](#variables)
- [Operators](#operators)
- [Functions](#functions)
- [Examples](#examples)
- [Best Practices](#best-practices)

---

## Basics

Expressions are used in the `when:` property to control when animation segments are active.

### Simple Expression
```mcmetax
when: health < 5
```

### Complex Expression
```mcmetax
when: health <= max_health * 0.25 && event(player_moving)
```

### Multiline Expression
```mcmetax
when:
    health < 5 &&
    (event(player_on_fire) || event(player_in_lava)) &&
    !event(player_swimming)
```

---

## Variables

Built-in variables you can use in expressions.

### Player Stats

| Variable | Type | Description | Range |
|----------|------|-------------|-------|
| `health` | Number | Current health | 0-20 (can be higher) |
| `max_health` | Number | Maximum health | Usually 20 |
| `hunger` | Number | Hunger level | 0-20 |

**Examples:**
```mcmetax
when: health < 5
when: health <= max_health * 0.5
when: hunger < 10
```

### Animation State

| Variable | Type | Description |
|----------|------|-------------|
| `first_frame` | Number | First frame index of current segment |
| `last_frame` | Number | Last frame index of current segment |

**Examples:**
```mcmetax
when: frame_index() == first_frame
when: frame_index() >= last_frame - 2
```

### Custom Variables

Define your own variables in the `[variables]` section:

```mcmetax
[variables]
low_hp: 5
critical_hp: 2
pulse_speed: 1

[segment: test]
frametime: $pulse_speed
when: health < $low_hp
```

**Variable References:**
- Use `$variable_name` to reference
- Can be numbers, frame specs, or expressions
- Expanded at parse time

---

## Operators

### Comparison Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `<` | Less than | `health < 5` |
| `>` | Greater than | `health > 15` |
| `<=` | Less than or equal | `health <= 10` |
| `>=` | Greater than or equal | `health >= 20` |
| `==` | Equal to | `hunger == 20` |
| `!=` | Not equal to | `hunger != 0` |

**Examples:**
```mcmetax
when: health < 5
when: hunger >= 15
when: health == max_health
```

### Logical Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `&&` | AND (both must be true) | `health < 5 && hunger < 10` |
| `\|\|` | OR (at least one true) | `event(player_on_fire) \|\| event(player_in_lava)` |
| `!` | NOT (negates condition) | `!event(player_moving)` |

**Examples:**
```mcmetax
# Both conditions must be true
when: health < 5 && event(player_moving)

# At least one must be true
when: event(daytime) || event(nighttime)

# Negate a condition
when: !event(player_sneaking)
```

### Arithmetic Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `+` | Addition | `health + 5` |
| `-` | Subtraction | `max_health - 5` |
| `*` | Multiplication | `max_health * 0.5` |
| `/` | Division | `health / max_health` |

**Examples:**
```mcmetax
# 50% of max health
when: health <= max_health * 0.5

# 25% of max health
when: health < max_health * 0.25

# Health percentage
when: (health / max_health) < 0.3
```

### Operator Precedence

From highest to lowest:

1. **Parentheses** `()`
2. **Negation** `!`
3. **Multiplication/Division** `*` `/`
4. **Addition/Subtraction** `+` `-`
5. **Comparison** `<` `>` `<=` `>=` `==` `!=`
6. **Logical AND** `&&`
7. **Logical OR** `||`

**Examples:**
```mcmetax
# Without parentheses
when: health < 5 || hunger < 10 && event(player_moving)
# Evaluates as: health < 5 || (hunger < 10 && event(player_moving))

# With parentheses for clarity
when: (health < 5 || hunger < 10) && event(player_moving)
```

---

## Functions

### Event Functions

#### `event(name)`
Checks if an event is currently active.

**Syntax:** `event(event_name)`

**Returns:** `true` or `false`

**Examples:**
```mcmetax
when: event(player_sneaking)
when: event(player_on_fire)
when: event(daytime)
```

#### `event_start(name)`
Triggers only when an event **just activated** (false → true transition).

**Syntax:** `event_start(event_name)`

**Returns:** `true` for one tick, then `false`

**Use Cases:**
- Flash effects
- Sound triggers
- One-shot animations

**Examples:**
```mcmetax
[segment: damage_flash]
type: transition
frames: 50-55
when: event_start(hurt_recently)

[segment: attack_burst]
type: transition
frames: 60-65
when: event_start(player_attacking)
```

#### `event_end(name)`
Triggers only when an event **just deactivated** (true → false transition).

**Syntax:** `event_end(event_name)`

**Returns:** `true` for one tick, then `false`

**Examples:**
```mcmetax
[segment: heal_effect]
type: transition
frames: 70-75
when: event_end(hurt_recently)

[segment: landing]
type: transition
frames: 80-85
when: event_end(player_airborne)
```

---

### Math Functions

#### `random()`
Returns a random number between 0.0 and 1.0.

**Syntax:** `random()`

**Returns:** `0.0` to `1.0`

**Examples:**
```mcmetax
# 10% chance
when: random() < 0.1

# 50% chance
when: random() < 0.5

# 25% chance with other condition
when: random() < 0.25 && event(player_idle)
```

#### `abs(value)`
Returns absolute value (always positive).

**Syntax:** `abs(expression)`

**Returns:** Positive number

**Examples:**
```mcmetax
when: abs(health - 10) < 2
when: abs(hunger - max_health) > 5
```

#### `min(a, b)`
Returns the smaller of two values.

**Syntax:** `min(value1, value2)`

**Returns:** Smaller value

**Examples:**
```mcmetax
# Either stat is low
when: min(health, hunger) < 5

# Lowest of three (nested)
when: min(health, min(hunger, 10)) < 3
```

#### `max(a, b)`
Returns the larger of two values.

**Syntax:** `max(value1, value2)`

**Returns:** Larger value

**Examples:**
```mcmetax
# Both stats are high
when: max(health, hunger) > 15

# At least one stat is critical
when: max(health, hunger) < 5
```

#### `between(value, min, max)`
Checks if value is between min and max (inclusive).

**Syntax:** `between(value, min, max)`

**Returns:** `true` or `false`

**Examples:**
```mcmetax
# Health between 5 and 10
when: between(health, 5, 10)

# Hunger in middle range
when: between(hunger, 8, 12)

# Using percentages
when: between(health, max_health * 0.25, max_health * 0.75)
```

---

### State Functions

#### `time_in_state()`
Returns how many ticks the current segment has been active.

**Syntax:** `time_in_state()`

**Returns:** Number of ticks (20 ticks = 1 second)

**Use Cases:**
- Delayed effects
- Time-based transitions
- Idle animations

**Examples:**
```mcmetax
# After 5 seconds (100 ticks)
when: time_in_state() > 100

# Idle for 10 seconds
when: event(player_idle) && time_in_state() > 200

# Quick pulse only if sustained
when: health < 5 && time_in_state() > 40
```

#### `frame_index()`
Returns the current frame index being displayed.

**Syntax:** `frame_index()`

**Returns:** Frame number

**Examples:**
```mcmetax
when: frame_index() > 10
when: frame_index() == first_frame
when: frame_index() >= last_frame - 2
```

#### `cycle_count()`
Returns how many times the animation has looped.

**Syntax:** `cycle_count()`

**Returns:** Number of loops

**Examples:**
```mcmetax
# After 3 loops
when: cycle_count() > 3

# First loop only
when: cycle_count() == 0

# Every other loop
when: cycle_count() % 2 == 0
```

---

### Game State Functions

#### `holding_item(name)`
Checks if player is holding an item containing the name.

**Syntax:** `holding_item(partial_name)`

**Returns:** `true` or `false`

**Note:** Case-insensitive partial match

**Examples:**
```mcmetax
when: holding_item("sword")
when: holding_item("pickaxe")
when: holding_item("diamond")
when: holding_item("totem")
```

#### `in_biome(name)`
Checks if player is in a biome containing the name.

**Syntax:** `in_biome(partial_name)`

**Returns:** `true` or `false`

**Note:** Case-insensitive partial match

**Examples:**
```mcmetax
when: in_biome("desert")
when: in_biome("ocean")
when: in_biome("nether")
when: in_biome("forest")
```

#### `has_effect(name)`
Checks if player has a potion effect containing the name.

**Syntax:** `has_effect(partial_name)`

**Returns:** `true` or `false`

**Note:** Case-insensitive partial match

**Examples:**
```mcmetax
when: has_effect("regeneration")
when: has_effect("poison")
when: has_effect("strength")
when: has_effect("speed")
```

#### `armor_value()`
Returns total armor points (0-20).

**Syntax:** `armor_value()`

**Returns:** Number (0-20)

**Examples:**
```mcmetax
# Low armor
when: armor_value() < 10

# Full armor
when: armor_value() >= 20

# No armor
when: armor_value() == 0
```

#### `light_level()`
Returns light level at player's position (0-15).

**Syntax:** `light_level()`

**Returns:** Number (0-15)

**Examples:**
```mcmetax
# Dark area
when: light_level() < 5

# Bright area
when: light_level() > 12

# Perfect darkness
when: light_level() == 0
```

---

## Examples

### Basic Conditions

```mcmetax
# Simple health check
when: health < 5

# Event check
when: event(player_sneaking)

# Combined
when: health < 10 && event(player_moving)
```

### Health Percentages

```mcmetax
# Below 25% health
when: health <= max_health * 0.25

# Below 50% health
when: health < max_health * 0.5

# Above 75% health
when: health > max_health * 0.75
```

### Complex Logic

```mcmetax
# In danger
when:
    health <= 5 &&
    (event(player_on_fire) || event(player_in_lava))

# Safe conditions
when:
    health >= max_health &&
    !event(hurt_recently) &&
    event(player_idle)

# Combat ready
when:
    health > 10 &&
    armor_value() > 15 &&
    holding_item("sword")
```

### Random Effects

```mcmetax
# 5% chance per tick
when: random() < 0.05

# Random sparkle when idle
when: random() < 0.1 && event(player_idle)

# Random only after delay
when: random() < 0.2 && time_in_state() > 100
```

### Transitions

```mcmetax
# Damage flash
when: event_start(hurt_recently)

# Attack burst
when: event_start(player_attacking)

# Heal effect
when: event_end(hurt_recently)

# Landing animation
when: event_end(player_airborne)
```

### Environmental

```mcmetax
# Desert at night
when: in_biome("desert") && event(nighttime)

# Ocean during storm
when: in_biome("ocean") && event(thundering)

# Dark cave
when: light_level() < 3 && !event(player_on_fire)
```

### State-Based

```mcmetax
# Idle for 5 seconds
when: event(player_idle) && time_in_state() > 100

# Long-running effect
when: time_in_state() > 200 && health < max_health

# Sustained damage
when: event(hurt_recently) && time_in_state() > 20
```

### Stats Comparison

```mcmetax
# Either stat low
when: min(health, hunger) < 5

# Both stats low
when: health < 5 && hunger < 5

# At least one stat critical
when: min(health, hunger) <= 2

# Both stats high
when: min(health, hunger) > 15
```

---

## Best Practices

### 1. Use Variables for Clarity

**Bad:**
```mcmetax
when: health <= 5
# Later...
when: health <= 5 && hunger < 10
```

**Good:**
```mcmetax
[variables]
low_hp: 5
low_hunger: 10

[segment: test1]
when: health <= $low_hp

[segment: test2]
when: health <= $low_hp && hunger < $low_hunger
```

### 2. Use Conditions for Reusability

**Bad:**
```mcmetax
when: health <= 5 || event(player_on_fire) || event(player_in_lava)
# Repeated multiple times...
```

**Good:**
```mcmetax
[conditions]
in_danger: health <= 5 || event(player_on_fire) || event(player_in_lava)

[segment: test]
when: $in_danger
```

### 3. Multiline for Complex Logic

**Bad:**
```mcmetax
when: health < 5 && (event(player_on_fire) || event(player_in_lava)) && !event(player_swimming) && armor_value() < 10
```

**Good:**
```mcmetax
when:
    health < 5 &&
    (event(player_on_fire) || event(player_in_lava)) &&
    !event(player_swimming) &&
    armor_value() < 10
```

### 4. Use Parentheses for Clarity

**Unclear:**
```mcmetax
when: health < 5 || hunger < 10 && event(player_moving)
```

**Clear:**
```mcmetax
when: (health < 5 || hunger < 10) && event(player_moving)
```

### 5. Optimize Event Checks

**Inefficient:**
```mcmetax
# Checks random every tick
when: random() < 0.01

# Better: limit with other condition
when: random() < 0.1 && event(player_idle) && time_in_state() > 100
```

### 6. Use Appropriate Functions

**Verbose:**
```mcmetax
when: health < 5 || hunger < 5
```

**Concise:**
```mcmetax
when: min(health, hunger) < 5
```

### 7. Comment Complex Expressions

```mcmetax
# Player is in extreme danger and trying to escape
when:
    (health <= 2 || event(near_death)) &&
    (event(player_sprinting) || event(player_swimming)) &&
    (event(player_on_fire) || event(player_in_lava))
```

---

## Common Patterns

### Health Tier System

```mcmetax
[variables]
critical: 2
low: 5
medium: 10

[segment: critical_state]
priority: 300
when: health <= $critical

[segment: low_state]
priority: 200
when: health <= $low && health > $critical

[segment: medium_state]
priority: 100
when: health <= $medium && health > $low

[segment: normal_state]
priority: 10
when: health > $medium
```

### Combat Detection

```mcmetax
[conditions]
in_combat: event(player_attacking) || event(hurt_recently)

[segment: active_combat]
when: $in_combat && health > 5

[segment: desperate_combat]
when: $in_combat && health <= 5
```

### Environmental Response

```mcmetax
[conditions]
hostile_environment:
    event(player_on_fire) ||
    event(player_in_lava) ||
    event(player_underwater)

[segment: danger]
when: $hostile_environment && armor_value() < 15
```

---

## Performance Tips

1. **Simple expressions are faster** than complex ones
2. **Event checks are cached** per tick
3. **Random is calculated fresh** each call
4. **Use variables** to avoid re-parsing
5. **Order matters**: Put likely-false conditions first with `&&`

**Example:**
```mcmetax
# Fast: player_moving is often false
when: event(player_moving) && health < 5

# Slower: checks health first every time
when: health < 5 && event(player_moving)
```

---

## Debugging Expressions

### Use F8 Debug Overlay

Shows:
- Active events in real-time
- Current segment being displayed
- Frame numbers

### Test Incrementally

```mcmetax
# Start simple
when: health < 5

# Add conditions one at a time
when: health < 5 && event(player_moving)

# Add more
when: health < 5 && event(player_moving) && !event(player_swimming)
```

### Use Fallback Segment

```mcmetax
[fallback]
frame: 0  # Shows when no other segments match
```

If you see the fallback frame, your conditions aren't matching.

---

<div align="center">

**[← Events Reference](EVENTS.md)** | **[Complete Guide →](GUIDE.md)**

</div>