## Guide Book Entries
The mod provides a guide book that contains vague hints for all built-in brews.
You can add your own entries to this book via a resource pack.

The entries are located in the `/assets/yournamespace/guide_entries/` folder of the resource pack, and named like `yourbrew.json`.

### Entry Format
An example entry looks like this:
```json5
{
  "title": "yournamespace.brew.yourbrew.name", // The translation key for the title of the page
  "description": "yournamespace.brew.yourbrew.hint", // The translation key for the description of the brew
  "icon_item": "minecraft:potion", // Which item to render as the icon of the brew
  "tint": 16734208, // The color to tint the icon with, in decimal RGB format. Only works when the icon is a potion.
  "order": 10 // The order in which the entry should be displayed in the book. Lower numbers are displayed first. Can be negative.
}
```

When two entries have the same order, they will be sorted lexicographically by their title.
Note that all built-in brews have a 'order' value of 0, and are thus sorted in that way.

You can either choose a different number for your entries to sort them before or after the built-in brews, or you can use 0 so that they are also sorted lexiographically.

### Translation Keys
An explanation of translation keys can be found [here](./custom_recipes.md#localization).