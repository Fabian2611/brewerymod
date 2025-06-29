from pathlib import Path
import json

def generate_doc_table(brews_path: Path, recipes_path: Path) -> str:
    brews_path = brews_path.resolve()
    recipes_path = recipes_path.resolve()
    table = "| Name | Effects | Ingredients | Brewing Time (min) | Distillation Item | Aging Time (min / ingame days) | Aging Wood Types | Max Purity |\n| --- | --- | --- | --- | --- | --- | --- | --- |\n"

    data: list[tuple[str, str, str, str, str, str, str]] = []
    for brew_file in brews_path.glob("*.json"):
        brew_name = brew_file.stem
        with open(brew_file, 'r', encoding='utf-8') as f:
            brew_data = json.load(f)
        
        effects_raw = brew_data.get("effects", "N/A")

        if isinstance(effects_raw, list) and effects_raw:
            effects = ", ".join(map(lambda x: x.get("effect").split(".")[2] + " " + str(int(x.get("amplifier")) + 1) + " for " + str(x.get("duration") / 20) + "s", effects_raw))

        max_purity = brew_data.get("maxPurity", "N/A")

        with open(recipes_path / f"{brew_name}.json", 'r', encoding='utf-8') as f:
            recipe_data = json.load(f)
        
        brewing_data = recipe_data.get("brewing_data", {})
        ingredients_raw = brewing_data.get("inputs", "N/A")

        ingredients = ", ".join(map(lambda x: str((x.get("minCount", -1) + x.get("maxCount", -1)) // 2) + "x " + x.get("item", "N/A"), ingredients_raw))

        brewing_time = brewing_data.get("optimalBrewingTime", "N/A") / 1200
        distillation_item = brewing_data.get("distillingItem", "N/A")
        aging_time = str(brewing_data.get("optimalAgingTime", "N/A") / 1200) + " / " + str(brewing_data.get("optimalAgingTime", "N/A") // 24000)
        aging_wood_types_raw = brewing_data.get("allowedWoodTypes", "N/A")

        if isinstance(aging_wood_types_raw, list) and aging_wood_types_raw:
            aging_wood_types = ", ".join(aging_wood_types_raw)
        else:
            aging_wood_types = "N/A"

        if distillation_item == "":
            distillation_item = "N/A"
        if aging_time == 0:
            aging_time = "N/A"

        data.append((brew_name, effects, ingredients, brewing_time, distillation_item, aging_time, aging_wood_types, max_purity))
    
    for brew in data:
        table += f"| {brew[0]} | {brew[1]} | {brew[2]} | {brew[3]} | {brew[4]} | {brew[5]} | {brew[6]} | {brew[7]} |\n"
    return table


if __name__ == "__main__":
    table = generate_doc_table(Path(r"src\main\resources\data\brewery\brew_types"), Path(r"src\main\resources\data\brewery\recipes"))
    print(table)
