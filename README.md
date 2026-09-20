# Crazy Diamond Stand — Fabric 1.20.1

Requires Fabric Loader 0.15+ and Fabric API (0.92.2+1.20.1).

## Controls (rebindable in Options → Controls → "Crazy Diamond")
| Input | Ability |
|-------|---------|
| V | Summon / dismiss the stand |
| Left mouse button (click or hold) | Punch |
| R | Barrage |
| Z | Return Block |
| X | Heal Mode |
| C | Stone Shot |
| G | Disassemble |
| B | Repair Item |

While the stand is summoned, left click is taken over by the stand (you don't hit or mine
yourself). Press V to dismiss the stand if you want normal left click back.

## Abilities
- **Punch** – heavy hit on whatever you look at, or smashes the block in front of you.
- **Barrage** – 3 seconds of rapid hits (with afterimage fists), big knockback finisher.
- **Return Block** – rebuilds blocks the stand smashed within 24 blocks, nearest first.
  The items those blocks dropped are taken back (from the ground nearby, then from your inventory).
- **Heal Mode** – heals the mob/player you look at for 5 s (yourself if nobody is targeted).
- **Stone Shot** – long-range stone with a golden trail.
- **Disassemble** – breaks the held item back into its crafting ingredients.
- **Repair Item** – gradually restores durability of the held item.

Notes
- Blocks smashed by the stand DO drop items (not in creative mode or with doTileDrops off).
  Only full blocks without block entities (no chests/furnaces) and not bedrock are affected.
  Set `StandActions.BREAK_BLOCKS = false` to turn block smashing off.
- The skin is `src/main/resources/assets/crazydiamond/textures/entity/crazy_diamond.png`
  (64x64, wide-arm layout, second layer supported). Pose and animations live in
  `client/StandModel.java`.

## Build
Open the folder in IntelliJ IDEA and run the Gradle `build` task, or use `gradle build`
(JDK 17). The jar is in `build/libs/` (use the one without `-sources`).
