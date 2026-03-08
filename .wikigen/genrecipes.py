from pathlib import Path
import json
import re

OUT_BASE_DIR = Path(r"/home/fabian/files/Source/Minecraft/Modding/brewery/addsrc").resolve()
OUT_BASE_DIR.mkdir(parents=True, exist_ok=True)
OUT_DIR_RECIPES = OUT_BASE_DIR / "recipes"
OUT_DIR_RECIPES.mkdir(parents=True, exist_ok=True)
OUT_DIR_TYPES = OUT_BASE_DIR / "brew_types"
OUT_DIR_TYPES.mkdir(parents=True, exist_ok=True)

VALID_WOOD_TYPES = [
    "acacia",
    "birch",
    "dark_oak",
    "jungle",
    "oak",
    "spruce",
    "crimson",
    "warped",
    "mangrove",
    "bamboo",
    "cherry"
]

VALID_DISTILLING_ITEMS = ["redstone", "glowstone_dust", "gunpowder"]

VALID_EFFECTS = {
    "alcohol": "effect.brewery.alcohol",
    "hangover": "effect.brewery.hangover",
    "alcohol_poisoning": "effect.brewery.alcohol_poisoning",
    "absorption": "effect.minecraft.absorption",
    "bad_luck": "effect.minecraft.unluck",
    "bad_omen": "effect.minecraft.bad_omen",
    "blindness": "effect.minecraft.blindness",
    "conduit_power": "effect.minecraft.conduit_power",
    "darkness": "effect.minecraft.darkness",
    "dolphins_grace": "effect.minecraft.dolphins_grace",
    "fire_resistance": "effect.minecraft.fire_resistance",
    "glowing": "effect.minecraft.glowing",
    "haste": "effect.minecraft.haste",
    "hunger": "effect.minecraft.hunger",
    "invisibility": "effect.minecraft.invisibility",
    "jump_boost": "effect.minecraft.jump_boost",
    "mining_fatigue": "effect.minecraft.mining_fatigue",
    "levitation": "effect.minecraft.levitation",
    "luck": "effect.minecraft.luck",
    "nausea": "effect.minecraft.nausea",
    "night_vision": "effect.minecraft.night_vision",
    "poison": "effect.minecraft.poison",
    "regeneration": "effect.minecraft.regeneration",
    "resistance": "effect.minecraft.resistance",
    "saturation": "effect.minecraft.saturation",
    "slowness": "effect.minecraft.slowness",
    "slow_falling": "effect.minecraft.slow_falling",
    "speed": "effect.minecraft.speed",
    "strength": "effect.minecraft.strength",
    "water_breathing": "effect.minecraft.water_breathing",
    "weakness": "effect.minecraft.weakness",
    "wither": "effect.minecraft.wither"
}

NAMESPACE_REGEX = re.compile(r"^[a-z0-9._-]+$")
BREW_ID_REGEX = re.compile(r"^[a-z_]+$")


def hex_to_int(hex_color: str):
    hex_color = (hex_color or "").strip()
    if hex_color.startswith("#"):
        hex_color = hex_color[1:]
    if not re.fullmatch(r"[0-9a-fA-F]{6}", hex_color):
        raise ValueError("Invalid hex color. Enter a value like #RRGGBB.")
    return int(hex_color, 16)


def prompt_string(prompt_text, default=None, allow_empty=False):
    suffix = f" [{default}]" if default else ""
    while True:
        response = input(f"{prompt_text}{suffix}: ").strip()
        if not response:
            if default:
                return default
            if allow_empty:
                return ""
            print("Please enter a value.")
            continue
        return response


def prompt_int(prompt_text, default=None, min_value=None, max_value=None):
    suffix = f" [{default}]" if default is not None else ""
    while True:
        response = input(f"{prompt_text}{suffix}: ").strip()
        if not response:
            if default is not None:
                value = default
            else:
                print("Please enter a number.")
                continue
        else:
            try:
                value = int(response)
            except ValueError:
                print("Please enter a valid integer.")
                continue
        if min_value is not None and value < min_value:
            print(f"Value must be >= {min_value}.")
            continue
        if max_value is not None and value > max_value:
            print(f"Value must be <= {max_value}.")
            continue
        return value


def prompt_float(prompt_text, default=None, min_value=None, max_value=None):
    suffix = f" [{default}]" if default is not None else ""
    while True:
        response = input(f"{prompt_text}{suffix}: ").strip()
        if not response:
            if default is not None:
                value = default
            else:
                print("Please enter a number.")
                continue
        else:
            try:
                value = float(response)
            except ValueError:
                print("Please enter a valid number.")
                continue
        if min_value is not None and value < min_value:
            print(f"Value must be >= {min_value}.")
            continue
        if max_value is not None and value > max_value:
            print(f"Value must be <= {max_value}.")
            continue
        return value


def prompt_namespace(default="brewery"):
    while True:
        namespace = input(f"Namespace (lowercase letters, digits, ., _, -) [{default}]: ").strip().lower()
        if not namespace:
            namespace = default
        if NAMESPACE_REGEX.fullmatch(namespace):
            return namespace
        print("Invalid namespace. Stick to lowercase letters, digits, ., _, and -.")


def prompt_brew_id():
    while True:
        value = input("Brew ID (lowercase letters and underscores, leave blank to finish): ").strip().lower()
        if not value:
            return ""
        if BREW_ID_REGEX.fullmatch(value):
            return value
        print("Brew ID must only contain lowercase letters and underscores.")


def prompt_hex_color(prompt_text, default="#ffffff"):
    while True:
        value = prompt_string(prompt_text, default)
        try:
            return hex_to_int(value)
        except ValueError as err:
            print(err)


def prompt_effects():
    effect_items = list(VALID_EFFECTS.items())
    print("\nEffects (enter the number, key, or a custom registry ID). Press Enter without typing to finish.")
    for idx, (key, registry) in enumerate(effect_items, 1):
        print(f"  {idx:02d}. {key} -> {registry}")
    effects = []
    while True:
        choice = input("Effect: ").strip()
        if not choice:
            break
        if choice.isdigit():
            index = int(choice) - 1
            if 0 <= index < len(effect_items):
                effect_key = effect_items[index][1]
            else:
                print(f"Choose a number between 1 and {len(effect_items)}.")
                continue
        elif choice in VALID_EFFECTS:
            effect_key = VALID_EFFECTS[choice]
        else:
            effect_key = choice
        duration = prompt_int("Duration in minutes", default=1, min_value=1)
        amplifier = prompt_int("Amplifier (0-255)", default=0, min_value=0, max_value=255)
        effects.append({
            "effect": effect_key,
            "duration": duration * 1200,
            "amplifier": amplifier
        })
    return effects


def prompt_wood_types():
    print("\nWood types (comma-separated). Type 'any' to allow every valid wood type.")
    print(f"Valid wood types: {', '.join(VALID_WOOD_TYPES)}")
    while True:
        raw = input("Allowed wood types: ").strip().lower()
        if not raw:
            return []
        choices = [part.strip() for part in raw.split(",") if part.strip()]
        if not choices:
            return []
        if "any" in choices:
            return VALID_WOOD_TYPES.copy()
        invalid = [wood for wood in choices if wood not in VALID_WOOD_TYPES]
        if invalid:
            print(f"Invalid wood types: {', '.join(invalid)}")
            continue
        seen = []
        for wood in choices:
            if wood not in seen:
                seen.append(wood)
        return seen


def prompt_distilling_item():
    print("\nDistilling items: redstone, glowstone_dust, gunpowder (leave blank for none).")
    while True:
        value = input("Distilling item: ").strip().lower()
        if not value:
            return ""
        if value in VALID_DISTILLING_ITEMS:
            return value
        print(f"Choose one of: {', '.join(VALID_DISTILLING_ITEMS)}, or leave blank.")


def prompt_ingredients():
    print("\nIngredients (enter item IDs, namespace optional; blank to finish).")
    ingredients = []
    while True:
        name = input("Item ID: ").strip().lower()
        if not name:
            break
        if ":" not in name:
            name = f"minecraft:{name}"
        min_count = prompt_int("Minimum amount", default=1, min_value=0)
        max_count = prompt_int("Maximum amount", default=min_count, min_value=min_count)
        ingredients.append({
            "item": name,
            "minCount": min_count,
            "maxCount": max_count
        })
    return ingredients


def prompt_brew(namespace):
    brew_id = prompt_brew_id()
    if not brew_id:
        return None
    print(f"\nConfiguring brew '{brew_id}'")
    display_name = prompt_string("Display name", allow_empty=True)
    display_lore = prompt_string("Lore", allow_empty=True)
    max_purity = prompt_int("Max purity", default=5, min_value=0)
    tint_color = prompt_hex_color("Brew color (#RRGGBB)", default="#ffffff")
    effects = prompt_effects()
    print("\nBrewing time, errors, and aging time will be converted to ticks.")
    brewing_minutes = prompt_int("Brewing time (minutes)", default=10, min_value=0)
    brewing_error = prompt_float("Brewing time max error (0-1)", default=0.1, min_value=0.0, max_value=1.0)
    aging_days = prompt_int("Aging time (days)", default=0, min_value=0)
    aging_error = prompt_float("Aging time max error (0-1)", default=0.1, min_value=0.0, max_value=1.0)
    wood_types = prompt_wood_types()
    distilling_item = prompt_distilling_item()
    ingredients = prompt_ingredients()

    name_key = f"{namespace}.brew.{brew_id}.name"
    lore_key = f"{namespace}.brew.{brew_id}.lore"

    brew_type = {
        "customLore": lore_key,
        "customName": name_key,
        "effects": effects,
        "maxAlcoholLevel": 1,
        "maxPurity": max_purity,
        "tintColor": tint_color
    }

    recipe = {
        "type": "brewery:brewing",
        "brewing_data": {
            "allowedWoodTypes": [wood for wood in wood_types if wood in VALID_WOOD_TYPES],
            "brew_type": brew_id,
            "distillingItem": distilling_item,
            "inputs": ingredients,
            "maxAgingTimeError": aging_error,
            "maxBrewingTimeError": brewing_error,
            "optimalAgingTime": aging_days * 24000,
            "optimalBrewingTime": brewing_minutes * 1200
        }
    }

    return {
        "brew_id": brew_id,
        "brew_type": brew_type,
        "recipe": recipe,
        "name_key": name_key,
        "display_name": display_name,
        "lore_key": lore_key,
        "display_lore": display_lore
    }


def generate_recipes():
    namespace = prompt_namespace()
    print("\nBrewing and aging times are entered in minutes/days respectively and are converted to ticks.")
    print("Item IDs without a namespace default to minecraft: automatically.")

    collected = []
    while True:
        brew = prompt_brew(namespace)
        if brew is None:
            break
        collected.append(brew)

    if not collected:
        print("No brews entered. Exiting.")
        return

    ids_seen = set()

    for brew in collected:
        brew_id = brew["brew_id"]
        if brew_id in ids_seen:
            print(f"Skipping duplicate Brew ID '{brew_id}'.")
            continue
        ids_seen.add(brew_id)
        brew_type_file = OUT_DIR_TYPES / f"{brew_id}.json"
        recipe_file = OUT_DIR_RECIPES / f"{brew_id}.json"
        with brew_type_file.open("w", encoding="utf-8") as fos:
            json.dump(brew["brew_type"], fos, indent=4, ensure_ascii=False)
        with recipe_file.open("w", encoding="utf-8") as fos:
            json.dump(brew["recipe"], fos, indent=4, ensure_ascii=False)
        print(f"Generated brew '{brew_id}' -> {brew_type_file.name}, {recipe_file.name}")
        if brew["display_name"]:
            print(f"  Translation key: {brew['name_key']} -> {brew['display_name']}")
        if brew["display_lore"]:
            print(f"  Lore key: {brew['lore_key']} -> {brew['display_lore']}")

    if not ids_seen:
        print("No valid brews to write.")
        return

    print("Remember to add the printed translation keys to your resource pack if desired.")


if __name__ == "__main__":
    generate_recipes()
    print("Recipe generation completed.")
