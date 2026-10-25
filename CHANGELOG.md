# Changelog
## [1.8.3 (October 25th, 2026)](#1-8-3)

### Additions
- Added compatability with AppleSkin.

### New Rideable Pokémon
- Vikavolt
- Fearow

### Particles
## New Move Animations
- Water Shuriken

### Cries added/updated for the following Pokémon
- Hoppip
- Skiploom
- Jumpluff
- Houndour
- Houndoom
- Wurmple
- Silcoon
- Beautifly
- Cascoon
- Dustox
- Surskit
- Masquerain
- Torkoal
- Starly
- Staravia
- Staraptor
- Combee
- Vespiquen
- Dusknoir
- Helioptile
- Heliolisk
- Grubbin
- Charjabug
- Vikavolt
- Applin (updated)
- Flapple (updated)
- Appletun
- Runerigus
- Dipplin

### Animation updates for the following Pokémon
- Vikavolt
- Magby
- Magmar
- Ninetales (+ Alolan)
- Spearow
- Fearow
- Squawkabilly
- Mudkip
- Marshtomp
- Swampert

### Changes
- Changed the Misc icon in the Campfire Pot Recipe Book to Health Candy.
- Moved EXP Candies from the Medicine recipe book category to the Misc category.
- Removed Dynamic Lights data from the Magmarizer item for consistency with the block.
- Added `doPokemonPredation` game rule, which controls aggressiveness of Pokémon with the `hate_entity` brain task defined.
- Added `predation_chance` server config value, which controls the possibility of attacking the hated entity found in `hate_entity` brain tasks.
- Prehistoric Eroded Pillar generates slightly more frequently.
- Togekiss was changed from a jet-style mount to bird-style.

### Fixes
- Fixed datapacks with item interaction evolutions that don't specify an item crashing the world on load.
- Fixed the Pokédex evolution tree crashing when a datapack's dex entry is missing an evolution's form.
- Fixed the Pokédex evolution tree crashing with a stack overflow on evolutions that loop back on themselves.
- Fixed the Pokédex evolution tree crashing when looking at a dex that is not part of the "National Dex" aggregate.
- Fixed some display names in the Pokédex evolution tree having unintended font colours.
- Fixed evolution trees not building properly when a Pokémon has several Pokédex entries for different regions.
- Fixed form-changing evolutions sometimes resolving to the source form in the Pokédex evolution tree.
- Broken datapack evolutions are now logged and skipped in the Pokédex evolution tree instead of crashing.
- Fixed the `alpha` property having no tab completion in commands such as `/pokespawn`, `/pokegive` and `/pokeedit`, and added the missing `dmax_level`, `gmax_factor` and `tradable` aliases to tab completion.
- Fixed friendship booster items not being recognised as such.
- Fixed conflicts with other mods that would make use of recipe books in certain ways (Farmer's Delight Cooking Pot).
- Fixed Pokémon battles stalling when a sidemod cancels the `THROWN_POKEBALL_HIT` hit event.
- Fixed choice feature tab completion in `spawnpokemon/pokegive/pokeedit` commands.
- Fixed MoLang that's targeting players intermittedly failing when they have died, requiring them to relog.
- Fixed a NeoForge bug where none of the Cobblemon plants/berries could be placed into pots.
- Fixed party overlay not hiding when the player opens the Pokédex scanner.
- Fixed `hate_entity` task focusing on its own entity, therefore enabling deadly warfare between Zangoose and Seviper.
- Prehistoric Frozen Pond, Prehistoric Frozen Spike, and Prehistoric Lush Den now actually generate (oops).
- Fixed Frogadier floating in its `battle_idle` animation.
- Fixed absence or incorrect mergers in spawning conditions, causing the max height/depth of conditions to use the min value instead.
- Added stricter riding checks on server side to prevent possible packet forgery when a player tries to ride Pokémon.
- Fixed Pokémon cries and animation sounds played from GUIs, such as the starter selection screen and the Pokédex, being tied to the Master volume slider instead of Friendly Creatures.
- Fixed Archeops T-posing during its `air_cry` animation.

### Developer
- Bumped NeoForge to `21.1.195` to get access to MixinExtras `0.5` and the expressions annotation.
- New Platform Event `PLAYER_RESPAWN` which is fired when a player respawns (either through death or returning from the end).
- Platform Events `SERVER_PLAYER_TICK_PRE` and `SERVER_PLAYER_TICK_POST` no longer fire when the player is dead.
  - It is now tied much closer to the actual entity rather than the player or their connection.

### Molang & Datapacks
- Fixed `q.pokemon.nature` returning class instance instead of proper string value (`cobblemon:adamant`).
- Fixed `q.entity.is_looking_at` result being always `1` for whatever entity passed as the target parameter.
- Added hate chance optional parameter to `hate_entity` task. `1.0` will make an entity always attack the target.

### Localization
- Updated translations for:
  - Russian
  - Japanese
  - Brazilian Portuguese
