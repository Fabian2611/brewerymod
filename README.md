## Brewery Forgified
Brewery Forgified is a simple Minecraft Mod using the Forge modloader that adds a brewing system to the game.
It is heavily inspired by the Spigot Plugin [Brewery](https://www.spigotmc.org/resources/brewery.3082/) and its still actively maintained fork [BreweryX](https://www.spigotmc.org/resources/breweryx.114777/).

## Issues
If you find any bugs or issues, please report them on the [GitHub Issues page](https://github.com/Fabian2611/brewerymod/issues/).
You can also use the Issues page to suggest new features or improvements, but I can not guarantee that I will implement them.

## Modpack Usage
You are allowed to use this mod in your modpack, as long as you do not claim this mod as your own and do not charge any form of payment in exchange for access to your modpack.
You do not have to credit me, but I'd greatly appreciate it if you did.

## Compatibility
Currently, this mod is only available for Minecraft 1.20.1 using Forge 47.4.0.
I do not plan on updating or backporting this mod to other Minecraft versions.
I do not plan on porting this mod to NeoForge, Fabric, Quilt or any other modloader.*
**If you plan on doing any of the above, feel free to do so.**

*The only exception to this is NeoForge, which I plan to do if I see great enough demand.

## Features
### Brewing
Fill a brewing cauldron with water and brew ingredients and supply it with heat to start brewing. Take the brew out using glass bottles after the brew-specific brewing time to continue with the process.
### Distilling
Some brews need to be distilled. For this, place them into a distillery station together with the correct filter.
### Fermenting
Some brews need to be barrel aged. For this, make a fermentation barrel of the correct wood type for your specific brew, put your brew inside and take it out once ready.
### Purity
After the brewing process is finished, your brew will be assigned a purity. The value will be decided by how precise your ingredient amounts were and how well you chose the brewing and aging times of the brew.
### Custom Brews & Recipes
The mod normally comes shipped with [a bunch of recipes already included](https://github.com/Fabian2611/brewerymod/wiki/Builtins), but you can also choose to add your own ones. A tutorial for this can be found [here](https://github.com/Fabian2611/brewerymod/wiki/Custom-Brews-&-Recipes).

## How to figure out recipes
Ususally, the players won't know the recipes for the brews, so they will have to figure them out themselves.
It is recommended you provide your players with small hints, like the names or descriptions of brew, or an approximate description of their ingredients.

With this, they can tweak the ingredients' amounts and brewing time until they get an "Unfinished Brew", at which point they can distill or barrel age it to get the final brew.

This is already provided for the built-in brews in form of the "Brew Guide" book.
If you want to add your own brews to this book, the instructions can be found [here](https://github.com/Fabian2611/brewerymod/wiki/Guide-Book-Entries).

## Credits
- [Brewery](https://www.spigotmc.org/resources/brewery.3082/) as the main inspiration
- [BreweryX](https://www.spigotmc.org/resources/breweryx.114777/) as inspiration for newer and QoL features
- [TheREDCrafter](https://github.com/TheREDCraafter) for great contribution to the block models
- DerLurch for the distillery station model

## Build validation
If you want to create your own build of the mod, just clone the repository to your local machine and run `gradlew build` and then `gradlew jar` in the root directory. The output jar will be located at `/libs/<version>.jar`. Make sure that your JAVA_HOME system variable is set to a local installation of Java 17, or you explicitly run gradlew with your Java 17 installation.
I recommend the [Eclipse Adoptium Temurin JDK](https://adoptium.net/temurin/releases?version=17&os=any&arch=any).

###### By the way, as you can probably tell, this is my first larger mod, so beware of unusual design choices in the code. Or even better, don't look at the code at all. It's not pretty.
