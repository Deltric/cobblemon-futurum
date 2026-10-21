# Changelog
## [1.8.3 (MONTH xth, 2026)](#1-8-3)

### Additions

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

### Model updates for the following Pokémon

### Animation updates for the following Pokémon
- Vikavolt

### Changes
- Changed the Misc icon in the Campfire Pot Recipe Book to Health Candy
- Moved EXP Candies from the Medicine recipe book category to the Misc category
- Removed Dynamic Lights data from the Magmarizer item for consistency with the block
- Added `doPokemonPredation` gamerule, which controls aggressiveness of Pokémon with `hate_entity` brain task defined
- Added `predation_chance` server config value, which controls the possibility of attacking the hated entity found by `hate_entity` brain task 
- Prehistoric Eroded Pillar generates slightly more frequently

### Fixes
- Fixed datapacks with item interaction evolutions that don't specify an item crashing the world on load.
- Fixed the Pokédex evolution tree crashing when a datapack's dex entry is missing an evolution's form.
- Fixed the Pokédex evolution tree crashing with a stack overflow on evolutions that loop back on themselves.
- Fixed the Pokédex evolution tree crashing when looking at a dex that is not part of the "National Dex" aggregate.
- Fixed evolution trees not building properly when a Pokémon has several Pokédex entries for different regions.
- Fixed form-changing evolutions sometimes resolving to the source form in the Pokédex evolution tree.
- Broken datapack evolutions are now logged and skipped in the Pokédex evolution tree instead of crashing.
- Fixed the `alpha` property having no tab completion in commands such as `/pokespawn`, `/pokegive` and `/pokeedit`, and added the missing `dmax_level`, `gmax_factor` and `tradable` aliases to tab completion
- Fixed friendship booster items not being recognised as such
- Fixed conflicts with other mods that would make use of recipe books in certain ways (Farmer's Delight Cooking Pot)
- Fixed Pokémon battles stalling when a sidemod cancels the `THROWN_POKEBALL_HIT` hit event
- Fixed choice feature tab completion in `spawnpokemon/pokegive/pokeedit` commands
- Fixed MoLang that's targeting players intermittedly failing when they have died, needing them to relog
- Fixed a NeoForge bug where none of the Cobblemon plants/berries could be placed into pots
- Fixed party overlay not hiding when the player opens the Pokédex scanner 
- Added compat for AppleSkin
- Fixed some display names having unintended font colours
- Fixed `hate_entity` task focusing on its own entity
  - Thus enabled deadly warfare between Zangoose and Seviper
- Prehistoric Frozen Pond, Prehistoric Frozen Spike, and Prehistoric Lush Den now generate
- Fixed Frogadier floating in its battle_idle animation
- Fixed absence or incorrect mergers in spawning conditions, making max height/depth of conditions to use the min value instead
- Added stricter riding checks on server side to prevent possible packet forgery when player tries to ride pokemon
- Fixed Pokémon cries and animation sounds played from GUIs, such as the starter selection screen and the Pokédex, being tied to the Master volume slider instead of Friendly Creatures

### Developer
- Bumped NeoForge to `21.1.195` to get access to MixinExtras `0.5` and the expressions annotation
- New Platform Event `PLAYER_RESPAWN` which is fired when a player respawns (either through death or returning from the end)
- Platform Events `SERVER_PLAYER_TICK_PRE` and `SERVER_PLAYER_TICK_POST` no longer fire when the player is dead
  - it is now tied much closer to the actual entity rather than the player (or their connection)

### Molang & Datapacks
- Fixed `q.pokemon.nature` returning class instance instead of proper string value (`cobblemon:adamant`)
- Fixed `q.entity.is_looking_at` result being always `1` for whatever entity passed as target
- Added hate chance optional parameter to `hate_entity` task. `1.0` will make an entity always attack the target 

### Localization
- Updated translations for:
