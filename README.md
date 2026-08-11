# regionRTP

Paper/Purpur plugin for randomized teleportation to WorldGuard regions.

## Requirements

- Paper/Purpur **1.18 – 26.2**
- Java 17+
- WorldGuard 7.0.7+

| MC | WorldGuard | Java |
|----|-----------|------|
| 1.18 | 7.0.7 | 17 |
| 1.19 | 7.0.8 | 17 |
| 1.20 | 7.0.9–10 | 17–21 |
| 1.21 | 7.0.11 | 21+ |
| 26.2 | 7.0.17 | 25 |

No NMS. Only stable Bukkit/WorldGuard APIs.

## Configuration

```yaml
regions:
  castle:
    world: world
    region: castle_region
    require-sky-exposure: true
    allow-water: false
    teleport-delay-seconds: 0
    cooldown-seconds: 0

spawn:
  attempts: 50
  edge-distance: 5
  minimum-player-distance: 15
```

### Per-region options

| Setting | Default | Description |
|---------|---------|-------------|
| `require-sky-exposure` | `true` | No solid blocks above the player's head. |
| `allow-water` | `false` | Allow spawning in water. |
| `teleport-delay-seconds` | `0` | Stand-still delay before teleport. 0–60. |
| `cooldown-seconds` | `0` | Cooldown after teleport. 0–3600. |

### Global spawn settings

| Setting | Default | Range | Description |
|---------|---------|-------|-------------|
| `attempts` | `50` | 1–500 | Candidates checked per teleport. |
| `edge-distance` | `5` | 0–256 | Edge band width in blocks. |
| `minimum-player-distance` | `15` | 0–1024 | Preferred min distance from other players. |

Invalid values are clamped at load time with a log warning.

## Commands

```
/regionrtp <region>     Teleport to the region.
/regionrtp reload       Reload config.
```

### Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `regionrtp.use` | op | Use `/regionrtp`. |
| `regionrtp.<region>` | — | Teleport to a specific region. |
| `regionrtp.*` | op | Teleport to any region. |
| `regionrtp.admin` | op | Manage regions via `/rrtp`. |
| `regionrtp.admin.reload` | op | Reload config. |

## Admin commands (`/rrtp`)

```
/rrtp add <id> <world> <region>     Add a region.
/rrtp remove <id>                   Remove a region.
/rrtp set <id> <key> <value>        Set a per-region option.
/rrtp list                          List all regions.
/rrtp info <id>                     Show region details.
/rrtp config <key> <value>          Set a global spawn setting.
/rrtp reload                        Reload config.
```

Changes are saved to `config.yml` immediately.

## Edge bias

Candidates are sampled uniformly from the region's bounding box and accepted
only if their distance to the nearest horizontal boundary is `<= edge-distance`.
WorldGuard containment is always the final check — non-cuboid regions work
correctly.

## Spawn selection

1. **Y search**: top-down when sky exposure is required; random start
   bidirectional otherwise. Scans the full vertical range of the region.
2. **Safety**: ground must be solid; feet and head must be non-solid and
   non-hazardous.
3. **Sky exposure** (if enabled): no solid blocks above the head up to world
   max height.
4. **Facing**: player faces the region center after teleport.

### Blocked blocks

Only blocks that hurt, slow, suffocate, or trap are denied. Crops, flowers,
tall grass, and farmland are allowed.

| Block | Checked at | Reason |
|-------|-----------|--------|
| Lava | feet, head, ground | Destroys player |
| Fire / Soul Fire | feet, head | Burns |
| Cactus | feet, head, ground | Contact damage |
| Magma Block | feet, head, ground | Contact damage |
| Wither Rose | feet, head | Wither effect |
| Pointed Dripstone | feet, head, ground | Fall/piercing |
| Powder Snow | feet, head, ground | Freezes |
| Bubble Column | feet, head | Sinks |
| Campfire / Soul Campfire | feet, head | Burns |
| Sweet Berry Bush | feet, head | Slows, damages |
| Cobweb | feet, head | Traps |
| Dragon Egg | feet, head, ground | Unreliable ground |
| Water | feet, head | Sinks (unless `allow-water: true`) |

## Teleport delay

If `teleport-delay-seconds > 0`, the player must stand still for the configured
duration. Moving or taking damage cancels the teleport.

## Player distance

`minimum-player-distance` is a preference, not a hard requirement. After
`attempts` candidates fail the distance check, a fallback of `attempts / 2`
ignores the distance preference. Safety and containment are never weakened.

## Cooldown

If `cooldown-seconds > 0`, the player must wait after a successful teleport
before using that region again.

## Build

```bash
./gradlew build
```

Output: `build/libs/regionRTP-1.0.0.jar`

## Tests

```bash
./gradlew test
```

## Architecture

```
region/         RtpRegion, RtpRegionSource, ConfigRtpRegionSource
config/         SpawnConfig
worldguard/     WorldGuardResolver, ResolvedRegion
spawn/          CandidateSelector, SafeLocationValidator, TeleportService,
                TeleportTask, RegionBounds
command/        RegionRtpCommand, AdminCommand
```

`RtpRegionSource` abstracts the region source so `ConfigRtpRegionSource` can be
replaced without touching the teleport logic.
