## Config
This mod has a config file that allows you to change some settings.
This file has an overview of all available settings and their default values.

### Config Location
The config files are located in the `/config/` folder of your Minecraft instance, named `brewery-<common|server|client>.toml`.
On dedicates servers, the config files will be located in the `/world/serverconfig/` and `/config/` folders, named `brewery-<server|common>.toml`.

### Common Config
- `useBuiltinBrews` (boolean):
  - **Default**: `true`
  - **Description**: Whether the mod should use the built-in brews or not. If set to `false`, only brews with 'builtin' set to 'false' will be registered.

### Server Config
- `maxAlcoholStackedDurationSeconds (integer)`:
  - **Default**: `1200`
  - **Range**: `[0, 2147483647]`
  - **Description**: The maximum duration in seconds that alcohol can be stacked to.
  
- `maxAlcoholStackedAmplifier (integer)`:
  - **Default**: `9`
  - **Range**: `[0, 255]`
  - **Description**: The maximum amplifier for stacked alcohol effects. The amplifier determines the strength of the effect, with higher values resulting in stronger effects.

- `minAlcoholLevelForPoison (integer)`:
  - **Default**: `7`
  - **Range**: `[0, 255]`
  - **Description**: Minimum alcohol amplifier needed to have a chance to apply minecraft's poison effect.

- `minAlcoholLevelForAlcoholPoisoning (integer)`:
  - **Default**: `9`
  - **Range**: `[0, 255]`
  - **Description**: Minimum alcohol amplifier needed to have a chance to apply the mod's alcohol poisoning effect.

