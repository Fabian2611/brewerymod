from pathlib import Path
import json

def hex_to_int(hex_color: str):
    if hex_color.startswith("#"):
        hex_color = hex_color[1:]
    return int(hex_color, 16)

def generate_recipes():
    OUT_BASE_DIR = Path(r"C:\Users\Fabian\Files\Source\Minecraft\Modding\brewery\src\main\resources\data\brewery").resolve()
    OUT_BASE_DIR.mkdir(parents=True, exist_ok=True)
    OUT_DIR_RECIPES = OUT_BASE_DIR / "recipes"
    OUT_DIR_RECIPES.mkdir(parents=True, exist_ok=True)
    OUT_DIR_TYPES = OUT_BASE_DIR / "brew_types"
    OUT_DIR_TYPES.mkdir(parents=True, exist_ok=True)
    VALID_WOOD_TYPES = ["acacia", "birch", "dark_oak", "jungle", "oak", "spruce", "crimson", "warped", "mangrove", "bamboo", "cherry"]
    VALID_DISTILLING_ITEMS = ["redstone", "glowstone_dust", "gunpowder"]

    while True:
        brew_name = input("Enter the name of the brew: ").strip().lower()
        if not brew_name:
            break
        max_purity = int(input("Enter the maximum purity: "))
        brew_color = hex_to_int(input("Enter the brew color (hex): "))
        effects = []
        while True:
            effect_name = input("Enter an effect name (or leave empty to finish): ")
            if not effect_name:
                break
            effect_duration = int(input(f"Enter the duration for {effect_name} (in minutes): ")) * 1200
            if effect_duration <= 0:
                print("Duration must be a positive integer.")
                continue
            effect_amplifier = int(input(f"Enter the amplifier for {effect_name}: "))
            if effect_amplifier < 0 or effect_amplifier > 255:
                print("Amplifier must be a non-negative integer smaller than 256.")
                continue
            effects.append({
                "amplifier": effect_amplifier,
                "duration": effect_duration,
                "effect": effect_name
            })
        brewing_time = int(input("Enter the brewing time (in minutes): ")) * 1200
        brewing_time_error = float(input("Enter the maximum brewing time error (0-1): "))
        aging_time = int(input("Enter the aging time (in days): ")) * 24000
        aging_time_error = float(input("Enter the maximum aging time error (0-1): "))
        brewing_wood_types = input("Enter the allowed wood types (comma-separated) or 'any': ").strip().split(",")
        if not brewing_wood_types or brewing_wood_types == [""]:
            brewing_wood_types = []
        elif brewing_wood_types[0].strip().lower() == "any":
            brewing_wood_types = VALID_WOOD_TYPES
        else:
            brewing_wood_types = [wood.strip() for wood in brewing_wood_types if (wood.strip() and (wood.strip() in VALID_WOOD_TYPES))]
        distilling_item = input("Enter the distilling item or leave empty: ").strip().lower()
        if distilling_item and not (distilling_item in VALID_DISTILLING_ITEMS):
            print(f"Invalid distilling item: {distilling_item}. Please choose from {VALID_DISTILLING_ITEMS}.")
            continue
        ingredients = []
        while True:
            ingredient_name = input("Enter an ingredient name (or leave empty to finish): ")
            if not ingredient_name:
                break
            ingredient_name = "minecraft:" + ingredient_name.strip().lower()
            ingredient_amount_min = int(input(f"Enter the minimum amount for {ingredient_name}: "))
            ingredient_amount_max = int(input(f"Enter the maximum amount for {ingredient_name}: "))
            ingredients.append({
                "item": ingredient_name,
                "maxCount": ingredient_amount_max,
                "minCount": ingredient_amount_min
            })
        
        brew_type = {
            "customLore": f"brewery.brew.{brew_name}.lore",
            "customName": f"brewery.brew.{brew_name}.name",
            "effects": [
                {
                    "amplifier": effect["amplifier"],
                    "duration": effect["duration"],
                    "effect": effect["effect"]
                } for effect in effects
            ],
            "maxAlcoholLevel": 1,
            "maxPurity": max_purity,
            "tintColor": brew_color,
        }

        recipe = {
            "type": "brewery:brewing",
            "brewing_data": {
                "allowedWoodTypes": [
                    wood for wood in brewing_wood_types if wood in VALID_WOOD_TYPES
                ],
                "brew_type": brew_name,
                "distillingItem": distilling_item,
                "inputs": ingredients,
                "maxAgingTimeError": aging_time_error,
                "maxBrewingTimeError": brewing_time_error,
                "optimalAgingTime": aging_time,
                "optimalBrewingTime": brewing_time,
            }
        }

        brew_type_file = OUT_DIR_TYPES / f"{brew_name}.json"
        recipe_file = OUT_DIR_RECIPES / f"{brew_name}.json"

        with brew_type_file.open("w", encoding="utf-8") as f:
            json.dump(brew_type, f, indent=4, ensure_ascii=False)
        with recipe_file.open("w", encoding="utf-8") as f:
            json.dump(recipe, f, indent=4, ensure_ascii=False)
        print(f"Generated brew type file: {brew_type_file}")
        print(f"Generated recipe file: {recipe_file}")

if __name__ == "__main__":
    generate_recipes()
    print("Recipe generation completed.")