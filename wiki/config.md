## Config
This mod has a config file that allows you to change some settings.
This file has an overview of all available settings and their default values.

### Config Location
The config files are located in the `/config/` folder of your Minecraft instance, named `brewery-<common|server|client>.toml`.
On dedicates servers, the config file will be located in the `/world/serverconfig/` folder, named `brewery-server.toml`.

### Common Config
- `useBuiltinBrews (boolean)`: 
  - **Warning**: This setting is deprecated and will be replaced in a future version.
  - **Default**: `true`
  - **Description**: This setting determines whether the mod should use the built-in brews or not. If set to `false`, only custom brews defined in datapacks will be used. This is useful if you want to create a completely custom brewing experience without the built-in brews.

### Server Config
- `maxAlcoholStackedDurationSeconds (integer)`:
  - **Default**: `1200`
  - **Range**: `[0, 2147483647]`
  - **Description**: This setting defines the maximum duration in seconds that alcohol can be stacked.
  
- `maxAlcoholStackedAmplifier (integer)`:
  - **Default**: `9`
  - **Range**: `[0, 255]`
  - **Description**: This setting defines the maximum amplifier for stacked alcohol effects. The amplifier determines the strength of the effect, with higher values resulting in stronger effects.
