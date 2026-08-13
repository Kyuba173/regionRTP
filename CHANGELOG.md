# Changelog

## 1.0.1

- **Security fix**: `regionrtp.*` wildcard no longer grants admin access.
  The wildcard is now `regionrtp.regions.*`, scoped to region teleportation
  only. Previously, Bukkit's `.*` wildcard resolution caused `regionrtp.*`
  to implicitly match `regionrtp.admin` and `regionrtp.admin.reload`, giving
  any holder of the region wildcard full admin privileges.
- **Breaking**: `regionrtp.*` renamed to `regionrtp.regions.*`.
- **Breaking**: `regionrtp.<region>` renamed to `regionrtp.regions.<region>`.
- **Breaking**: `regionrtp.admin.reload` removed. Reload is now gated by
  `regionrtp.admin` (both `/regionrtp reload` and `/rrtp reload`).
- `regionrtp.admin` now implicitly grants `regionrtp.use` and
  `regionrtp.regions.*` via Bukkit's `children` hierarchy.
- `regionrtp.regions.*` now implicitly grants `regionrtp.use`.

### Migration

Update your permissions plugin (LuckPerms, etc.):

| Old | New |
|-----|-----|
| `regionrtp.*` | `regionrtp.regions.*` |
| `regionrtp.<region>` | `regionrtp.regions.<region>` |
| `regionrtp.admin.reload` | `regionrtp.admin` (now a child of admin) |

`regionrtp.admin` holders no longer need separate `regionrtp.use` or
`regionrtp.regions.*` nodes — they are granted automatically.

## 1.0.0

- Initial release
- Randomized teleport to WorldGuard regions
- Edge-biased candidate selection
- Safe location validation (feet, head, ground)
- Optional sky exposure check
- Per-region water, delay, and cooldown settings
- Player distance preference with fallback
- Face region center after teleport
- Admin commands (`/rrtp`)
- Per-region permissions
- Paper 1.18 – 26.2, WorldGuard 7.0.7+