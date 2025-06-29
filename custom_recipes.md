## Custom Brews & Recipes
You can add your own brews and recipes to the mod by using datapacks.

## Adding a brew type
For adding a new brew type, you need to create a new JSON file in the `data/brewery/brew_types/` folder of your datapack.
It should be named like your brew type, e.g. `cool_brew.json`.
The content will look something like this:

```json5
{
  "customLore": "yournamespace.brew.cool_brew.name", // The lore of the brew, as seen when hovering over it. Can be a translation key or literal text.
  "customName": "yournamespace.brew.cool_brew.name", // The name of the brew. Can be a translation key or literal text.
  "effects": [ // This opens a list of effects that the brew will apply when consumed.
    {
      "amplifier": 1, // The amplifier of the effect, e.g. 0 for level I, 1 for level II, etc.
      "duration": 4800, // The duration of the effect in ticks, where one second consists of 20 ticks, e.g. 4800 for 4 minutes.
      "effect": "effect.brewery.alcohol" // The registry key of the effect that will be applied. This can be a custom effect or a vanilla effect.
    },
    {
      "amplifier": 0,
      "duration": 4800,
      "effect": "effect.minecraft.fire_resistance" // For vanilla effects, the registry key is usually `effect.minecraft.<effect_id>`.
                                                   // For e.g. Fire Resistance, you can find the ID at https://minecraft.wiki/w/Fire_Resistance#ID.
    }
  ],
  "maxAlcoholLevel": 2, // Unused.
  "maxPurity": 5, // The maximum possible purity of the brew.
  "tintColor": 10231566 // The color of the brew, as a decimal RGB value. This is used for the tint of the brew item and the water color in the brewing cauldron.
                        // You can calculate this value as (red * 65536) + (green * 256) + blue, where red, green and blue are the RGB values in the range of 0-255.
                        // I usually use a color picker tool to get the RGB hex value and then convert it to decimal.
}
```
For the customLore and customName, you can use translation keys to allow for localization. `yournamespace` should be replaced with your resource pack's namespace, where the localization files are stored. More on this can be found at the [Minecraft Wiki](https://minecraft.wiki/w/Resource_pack#Language).
You can also (unintendedly) use literal text. If your text collides with a translation key, the translation key will take priority.

## Adding a recipe
To add a recipe for your brew type, you need to create a new JSON file in the `data/brewery/recipes/` folder of your datapack.
It should be named like your brew type, e.g. `cool_brew.json`, except when you define multiple recipes for a single brew type. In this case, you should name the files like `cool_brew_from_sugarcane.json`, `cool_brew_from_bamboo.json`, etc. to distinguish them.
The content will look something like this:

```json5
{
  "type": "brewery:brewing", // This line needs to be present in every brewing recipe file.
  "brewing_data": {
    "allowedWoodTypes": [ // A list of wood types that can be used for aging the brew. If empty, no aging is allowed for this brew type. Can contain "acacia", "bamboo", "birch", "cherry", "dark_oak", "jungle", "mangrove", "oak", "spruce", "crimson" and "warped".
      "oak",
      "spruce"
    ],
    "brew_type": "cool_brew", // The brew type that this recipe is for. This should match the name of the brew type JSON file.
    "distillingItem": "glowstone_dust", // The item that is used for distilling the brew. This must be one of "glowstone_dust", "redstone" or "gunpowder". Can also be "" (empty) to disallow distilling for this brew type.
    "inputs": [ // A list of items that are needed inside of the brewing cauldron in order to start this recipe. 
      {
        "item": "minecraft:sugar_cane", // The full item id of the item, including the namespace.
        "maxCount": 17, // The maximum amount of this item that can be used in the recipe. If the player has input more than this amount, the brew will fail.
        "minCount": 11  // The minimum amount of this item that is needed in the recipe. If the player has input less than this amount, the brewing will not start.
                        // The optimal amount for purity calculation is the average of minCount and maxCount, so in this case 14. To ensure that the player can achieve the optimal purity, you should set minCount and maxCount so that this value is an integer.
      }
    ],
    "optimalAgingTime": 336000, // The optimal aging time in ticks, where one second consists of 20 ticks. This should usually be some multiple of 24000 (20 minutes), since this is the time it takes for a day to pass in Minecraft. e.g. 336000 for 28 days.
    "optimalBrewingTime": 6000, // The optimal brewing time in ticks, where one second consists of 20 ticks, e.g. 6000 for 300 seconds, or 5 minutes.
    "maxAgingTimeError": 0.1 ,  // The maximum allowed error for the aging time in percent so that the brew still succeeds, e.g. 0.1 for 10%. This means that the aging time can be between 90% and 110% of the optimal aging time.
    "maxBrewingTimeError": 0.15 // The maximum allowed error for the brewing time in percent so that the brew still succeeds, e.g. 0.15 for 15%. This means that the brewing time can be between 85% and 115% of the optimal brewing time.
  }
}
```