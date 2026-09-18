# BuildCraft Lasers +
A BuildCraft addon for Minecraft 1.12.2 that adds configurable BuildCraft Lasers, for faster processing. The main goal is reducing the strain on the server by compressing 100 lasers into 1.

## Version mapping

| Mod Version | BuildCraft Version |
|-------------|--------------------|
| 1.0.0       | 8.0.0+             |

## Features

Each laser is configurable via the `Laser Factors` config. Each entry in this list creates a new Laser with the associated value as speed and battery capacity factor (from the base BuildCraft laser).

Only 4 default lasers are provided, corresponding to 4x, 16x, 64x, and 256x. If you want more lasers, you will need to add more factors and provide the corresponding configurations :
- Model (`models/block/laser_plus_<index>.json` & `models/item/laser_plus_<index>.json`) for texture mapping + any texture referenced within these models
- Blockstate (`blockstates/laser_plus_<index>.json`) for rotation
- Recipe (`recipes/blocks/laser_plus_<index>.json`)
- Localization (`lang/en_us.json`) for in-game names

A restart is required for any modifications to take effect.

## Configuration

The mod comes with a few mixins that modify the behavior of lasers for improved performance and bug fixes, corresponding to PR 4764 to 4770 in the [BuildCraft repository](https://github.com/BuildCraft/BuildCraft/pull). You can configure which mixins are enabled or disabled through the configuration file or the in-game Forge config GUI. They should be disabled once the PRs have been merged and a new release is available. Without MixinBooter, these mixins will not be applied, resulting in the mod not working at all, unless you have a version of BuildCraft with #4765 merged (this is the only one we truly depend on).

/!\ If ANY of the mixins are enabled, you should have MixinBooter in your mods list! Otherwise, the mixins will not load. /!\

### [Stop Spurious Searches](https://github.com/BuildCraft/BuildCraft/pull/4764)
The lasers will re-scan for targets when any changes occur in their surroundings, causing false-positive target searches. This mixin stops such spurious searches, only updating the target when blocks truly change.

### [Get Laser Block](https://github.com/BuildCraft/BuildCraft/pull/4765)
Allows our Lasers to function in the first place, by widening a check on the laser block.

### [Performance Optimizations](https://github.com/BuildCraft/BuildCraft/pull/4766)
Global performance optimizations for lasers, reducing the load on both the client and server severalfold.

### [Fix Laser Listener Leaking](https://github.com/BuildCraft/BuildCraft/pull/4767)
Fixes the issue where the Laser listener leaks on chunk unloads, causing performance degradations across the board.

### [Fix Laser Not Working](https://github.com/BuildCraft/BuildCraft/pull/4768)
Fixes the issue where lasers may stop working at world load, due to the table loading after the lasers.

### [Fix Laser Power Overflow](https://github.com/BuildCraft/BuildCraft/pull/4769)
Fixes the issue where laser power could overflow, causing the lasers to stop working altogether (requiring breaking and replacing the affected lasers).

### [Battery Capacity Fix](https://github.com/BuildCraft/BuildCraft/pull/4770)
Allows lasers to scale their battery capacity according to the configured factors, instead of storing the base capacity (which is 1024 MJ = 10240 RF).
