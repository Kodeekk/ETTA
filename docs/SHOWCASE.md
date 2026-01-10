# 🌟 ETTA Community Showcase

Amazing animations created by the community using ETTA!

> **Want to be featured?** Tag `#ETTAAnimations` on social media or submit a PR!

---

## Featured Creations

### 🏆 Top Picks

Coming soon! Be the first to submit your creation.

---

## By Category

### ⚔️ Combat & Weapons

**Example: Dynamic Sword**
```mcmetax
# Sword that changes in combat
[animation]
frametime: 2

[segment: critical_strike]
type: transition
frames: 30-40
priority: 500
when: event_start(player_attacking) && health <= 5

[segment: combat_mode]
type: sequence
frames: 20-29
priority: 300
when: event(player_attacking)

[segment: idle_glow]
type: sequence
frames: 0-10
priority: 10
```

*Submit yours →*

---

### 💚 Health & Status

**Example: Dynamic Heart**
```mcmetax
# Item that shows health status
[animation]
frametime: 3

[segment: critical]
type: sequence
frames: 15-20
frametime: 1
when: health <= 2

[segment: low]
type: sequence
frames: 10-14
when: health <= 5

[segment: normal]
type: sequence
frames: 0-9
priority: 10
```

*Submit yours →*

---

### 🌤️ Environmental

**Example: Weather Clock**
```mcmetax
# Shows time and weather
[animation]
frametime: 1

[segment: thunder]
type: sequence
frames: 40-50
priority: 300
when: event(thundering)

[segment: rain_day]
type: sequence
frames: 30-39
priority: 200
when: event(raining) && event(daytime)

[segment: rain_night]
type: sequence
frames: 20-29
priority: 200
when: event(raining) && event(nighttime)

[segment: clear_day]
type: sequence
frames: 0-9
when: event(daytime)

[segment: clear_night]
type: sequence
frames: 10-19
when: event(nighttime)
```

*Submit yours →*

---

### 🍖 Food & Consumables

**Example: Hunger-Aware Food**
```mcmetax
# Glows when you're hungry
[animation]
frametime: 2

[segment: starving]
type: sequence
frames: 20-30
frametime: 1
priority: 300
when: hunger <= 6

[segment: hungry]
type: sequence
frames: 10-19
priority: 200
when: hunger < 10

[segment: normal]
type: sequence
frames: 0-9
priority: 10
```

*Submit yours →*

---

### 🛡️ Armor & Protection

**Example: Reactive Shield**
```mcmetax
# Shield that reacts to blocking
[animation]
frametime: 1

[segment: block_flash]
type: transition
frames: 30-35
priority: 500
when: event_start(player_sneaking)

[segment: blocking]
type: sequence
frames: 20-29
priority: 300
when: event(player_sneaking)

[segment: damaged]
type: sequence
frames: 10-19
priority: 100
when: armor_value() < 10

[segment: idle]
type: sequence
frames: 0-9
priority: 10
```

*Submit yours →*

---

### 🧭 Tools & Utilities

**Example: Smart Compass**
```mcmetax
# Spins based on movement
[animation]
frametime: 1

[segment: sprinting]
type: sequence
frames: 20-35
frametime: 1
priority: 200
when: event(player_sprinting)

[segment: moving]
type: sequence
frames: 10-19
priority: 100
when: event(player_moving)

[segment: idle]
type: sequence
frames: 0-9
priority: 10
```

*Submit yours →*

---

### ✨ Special Effects

**Example: Sparkle Effect**
```mcmetax
# Random sparkles when idle
[animation]
frametime: 2

[segment: rare_sparkle]
type: sequence
frames: 30-40
priority: 200
when: random() < 0.05 && event(player_idle)

[segment: normal_glow]
type: sequence
frames: 0-20
priority: 10
```

*Submit yours →*

---

### 🎨 Creative/Artistic

**Example: Mood Ring**
```mcmetax
# Changes color based on stats
[animation]
frametime: 2

[segment: danger]
type: sequence
frames: 30-40
priority: 300
when: min(health, hunger) < 5

[segment: caution]
type: sequence
frames: 20-29
priority: 200
when: min(health, hunger) < 10

[segment: good]
type: sequence
frames: 10-19
priority: 100
when: min(health, hunger) < 15

[segment: perfect]
type: sequence
frames: 0-9
priority: 10
```

*Submit yours →*

---

## Resource Packs

### Complete Packs

*No submissions yet - be the first!*

**Template Pack Structure:**
```
my_etta_pack/
├── pack.mcmeta
├── pack.png (optional)
└── assets/
    └── minecraft/
        └── textures/
            └── item/
                ├── item1.etta/
                ├── item2.etta/
                └── ...
```

---

## Tutorials & Tips

### Community Tips

**Tip #1: Health Tiers**
Use variables for clean health thresholds:
```mcmetax
[variables]
critical: 2
low: 5
medium: 10
high: 15
```

**Tip #2: Priority Layering**
Use 100-point increments for easier management:
```mcmetax
critical: 500
important: 400
active: 300
normal: 200
idle: 100
fallback: 10
```

**Tip #3: Testing**
Start simple, add complexity:
```mcmetax
# Step 1: Basic
when: health < 5

# Step 2: Add event
when: health < 5 && event(player_moving)

# Step 3: Add more
when: health < 5 && event(player_moving) && !event(player_swimming)
```

---

## Video Showcases

*No videos yet - submit yours!*

### How to Submit

1. Upload your video to YouTube
2. Tag with `#ETTAAnimations`
3. Open a PR adding your video here
4. Include:
    - Video link
    - Brief description
    - Creator name

---

## Download Packs

### Featured Downloads

*Coming soon!*

### How to Submit a Pack

1. **Create your pack** following [GUIDE.md](docs/GUIDE.md)
2. **Test thoroughly**
3. **Add README** with:
    - Pack description
    - Installation instructions
    - Credits
4. **Upload to**:
    - GitHub release
    - Modrinth
    - CurseForge
5. **Submit here** via PR

---

## Hall of Fame

### Top Contributors

*Be the first!*

### Most Creative

*Be the first!*

### Most Technical

*Be the first!*

---

## Submission Guidelines

### What We're Looking For

✅ **Original creations**
✅ **Well-documented** (comments in code)
✅ **Good visual quality**
✅ **Interesting mechanics**
✅ **Reusable/educational**

### How to Submit

**Option 1: Pull Request**
1. Fork this repository
2. Add your showcase to this file
3. Include:
    - Screenshots/GIF
    - Code snippet
    - Description
    - Your name/link
4. Submit PR

**Option 2: Issue**
1. Create new issue with `showcase` label
2. Include all materials
3. We'll add it for you

**Option 3: Social Media**
1. Post with `#ETTAAnimations`
2. We'll find and feature it!

### Required Information

```markdown
### [Your Animation Name]

**Creator:** [Your Name](link)
**Category:** [Combat/Health/Environmental/etc.]

**Description:**
Brief description of what it does

**Code:**
```mcmetax
[paste your mcmetax code]
```

**Screenshots:**
![Screenshot](url)

**Download:** [Link if available]
```

---

## Community Gallery

### Recent Submissions

*No submissions yet - yours could be first!*

---

## Challenges

### Monthly Challenges

**Current Challenge:** None yet

**Past Challenges:**
- None yet

### How Challenges Work

1. **Theme announced** (first of month)
2. **Create animation** based on theme
3. **Submit by end of month**
4. **Community votes** on favorite
5. **Winner featured** in README

---

## Resources

### Templates

**Basic Item Template:**
```mcmetax
[animation]
frametime: 2

[variables]
# Add your variables

[conditions]
# Add your conditions

[fallback]
frame: 0

[segment: your_segment]
type: sequence
frames: 0-10
when: event(holding_item)
```

**Health-Based Template:**
```mcmetax
[animation]
frametime: 2

[variables]
critical_hp: 2
low_hp: 5
medium_hp: 10

[fallback]
frame: 0

[segment: critical]
frames: 20-30
priority: 300
when: health <= $critical_hp

[segment: low]
frames: 10-19
priority: 200
when: health <= $low_hp && health > $critical_hp

[segment: medium]
frames: 5-9
priority: 100
when: health <= $medium_hp && health > $low_hp

[segment: normal]
frames: 0-4
priority: 10
```

**Combat Template:**
```mcmetax
[animation]
frametime: 2

[conditions]
in_combat: event(player_attacking) || event(hurt_recently)

[fallback]
frame: 0

[segment: attack_flash]
type: transition
frames: 30-35
priority: 500
when: event_start(player_attacking)

[segment: combat]
frames: 20-29
priority: 300
when: $in_combat

[segment: idle]
frames: 0-10
priority: 10
```

### Texture Resources

**Where to get textures:**
- [Minecraft Textures](https://minecraft.wiki/w/Resource_pack) - Official wiki
- Create your own in:
    - [Paint.NET](https://www.getpaint.net/)
    - [GIMP](https://www.gimp.org/)
    - [Aseprite](https://www.aseprite.org/)
    - [Piskel](https://www.piskelapp.com/) (online)

---

## Stats

- **Total Submissions:** 0
- **Total Downloads:** 0
- **Most Popular Category:** TBD
- **Average Rating:** TBD

---

## Thank You!

Thank you to everyone who creates and shares ETTA animations! Your creativity makes this community amazing. 💙

---

<div align="center>

**[← Back to README](README.md)** | **[Submit Creation →](../../issues/new)**

</div>