##  Examples

### Health-Based Texture Animation

```mcmetax
[animation]
frametime: 2

[fallback]
frame: 0

[segment: critical]
type: sequence
frames: 10-15
frametime: 1
when: health <= 2

[segment: low]
type: sequence
frames: 5-9
when: health <= 5

[segment: normal]
type: sequence
frames: 0-4
when: health > 5
```

### Sword In Combat

```mcmetax
[animation]
frametime: 2

[fallback]
frame: 0

# Flash on attack
[segment: attack_flash]
type: transition
frames: 20-25
priority: 500
when: event_start(player_attacking)

# Glow in combat
[segment: combat]
type: sequence
frames: 10-15
priority: 200
when: event(player_attacking)

# Idle shimmer
[segment: idle]
type: sequence
frames: 0-4
priority: 10
```

### Dynamic Clock

```mcmetax
[animation]
frametime: 1

[fallback]
frame: 0

[segment: day]
type: sequence
frames: 0-11
when: event(daytime)

[segment: night]
type: sequence
frames: 12-23
when: event(nighttime)

[segment: sunrise]
type: sequence
frames: 24-27
when: event(sunrise)
```

### Environmental Food

```mcmetax
[animation]
frametime: 3

[variables]
starving: 6

[fallback]
frame: 0

[segment: desperate]
type: sequence
frames: 10-15
frametime: 1
when: hunger <= $starving && event(player_in_lava)

[segment: hungry]
type: sequence
frames: 5-9
when: hunger < 10

[segment: normal]
type: sequence
frames: 0-4
priority: 10
```
<div align="center">

**[← Back to README](../README.md)** | **[Events →](EVENTS.md)**

</div>