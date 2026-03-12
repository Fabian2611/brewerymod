from pathlib import Path
import json

def generate_doc_table(brews_path: Path, recipes_path: Path) -> str:
    # Use resolve() to handle relative paths correctly
    brews_path, recipes_path = brews_path.resolve(), recipes_path.resolve()
    
    # Added "Overageable" to headers
    headers = [
        "Name", "Effects", "Ingredients", "Brewing (min)",  
        "Distillation", "Aging (min / days)", "Wood Types", "Max Purity", "Overageable"
    ]
    table = "| " + " | ".join(headers) + " |\n"
    table += "| " + " | ".join(["---"] * len(headers)) + " |\n"

    rows = []
    # Using sorted() ensures the table is alphabetical
    for brew_file in sorted(brews_path.glob("*.json")):
        brew_name = brew_file.stem
        recipe_file = recipes_path / f"{brew_name}.json"
        
        if not recipe_file.exists():
            continue

        with open(brew_file, 'r', encoding='utf-8') as f:
            b_data = json.load(f)
        with open(recipe_file, 'r', encoding='utf-8') as f:
            r_data = json.load(f)

        if r_data.get("type", "") != "brewery:brewing":
            continue

        # 1. Process Effects
        effects_list = b_data.get("effects", [])
        effect_strings = []
        for e in effects_list:
            name = e.get("effect", "?.?.unknown").split(".")[-1].replace("_", " ").title()
            amp = int(e.get("amplifier", 0)) + 1
            dur = int(e.get("duration", 0)) // 20
            effect_strings.append(f"{name} {amp} ({dur}s)")
        effects = ", ".join(effect_strings) or "None"

        # 2. Process Ingredients (Average of min/max)
        b_info = r_data.get("brewing_data", {})
        inputs = b_info.get("inputs", [])
        ing_list = []
        for i in inputs:
            avg = (i.get("minCount", 0) + i.get("maxCount", 0)) // 2
            item = i.get("item", "Unknown").split(":")[-1].replace("_", " ").title()
            ing_list.append(f"{avg}x {item}")
        ingredients = ", ".join(ing_list)

        # 3. Conversions & Formatting
        brew_min = round(b_info.get("optimalBrewingTime", 0) / 1200, 1)
        distill = b_info.get("distillingItem", "N/A").split(":")[-1] or "N/A"
        
        age_ticks = b_info.get("optimalAgingTime", 0)
        age_min = round(age_ticks / 1200, 1)
        age_days = round(age_ticks / 24000, 1)
        aging_str = f"{age_min} / {age_days}" if age_ticks > 0 else "N/A"

        woods = ", ".join(b_info.get("allowedWoodTypes", [])) or "Any"
        purity = b_data.get("maxPurity", "N/A")
        
        # 4. Process Overageable (pulls from brew_types JSON)
        overageable = "Yes" if b_data.get("overageable", False) else "No"

        rows.append([
            brew_name.replace("_", " ").title(), effects, ingredients, 
            str(brew_min), distill, aging_str, woods, str(purity), overageable
        ])

    for r in rows:
        table += "| " + " | ".join(r) + " |\n"

    return table

if __name__ == "__main__":
    # Note: Ensure these paths remain accurate to your T15Gen2 environment
    table = generate_doc_table(
        Path(r"./src/main/resources/data/brewery/brew_types"), 
        Path(r"./src/main/resources/data/brewery/recipes")
    )
    print(table)
