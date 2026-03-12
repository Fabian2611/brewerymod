from pathlib import Path
import json

def generate_cocktail_table(brews_path: Path, recipes_path: Path) -> str:
    brews_path, recipes_path = brews_path.resolve(), recipes_path.resolve()
    
    # Updated headers: removed Overageable
    headers = [
        "Cocktail Name", "Effects", "Base Brews", "Extra Ingredients", "Max Purity"
    ]
    table = "| " + " | ".join(headers) + " |\n"
    table += "| " + " | ".join(["---"] * len(headers)) + " |\n"

    rows = []
    
    # Iterate through recipes to find cocktails
    for recipe_file in sorted(recipes_path.glob("*.json")):
        with open(recipe_file, 'r', encoding='utf-8') as f:
            r_data = json.load(f)
        
        # Only process cocktail types
        if r_data.get("type") != "brewery:cocktail":
            continue
            
        c_data = r_data.get("cocktail_data", {})
        result_id = c_data.get("result_brew_type")
        brew_file = brews_path / f"{result_id}.json"

        # Ensure the resulting brew type actually has a definition file
        if not brew_file.exists():
            continue

        with open(brew_file, 'r', encoding='utf-8') as f:
            b_data = json.load(f)

        # 1. Process Effects
        effects_list = b_data.get("effects", [])
        effect_strings = []
        for e in effects_list:
            name = e.get("effect", "?.?.unknown").split(".")[-1].replace("_", " ").title()
            amp = int(e.get("amplifier", 0)) + 1
            dur = int(e.get("duration", 0)) // 20
            effect_strings.append(f"{name} {amp} ({dur}s)")
        effects = ", ".join(effect_strings) or "None"

        # 2. Process Base Brews (e.g., Rum, Sweet Tea)
        bases = []
        for b in c_data.get("brew_inputs", []):
            # Formats 'sweet_tea' to 'Sweet Tea'
            b_name = b.get("brewTypeId", "Unknown").replace("_", " ").title()
            count = b.get("count", 1)
            bases.append(f"{count}x {b_name}")
        base_str = ", ".join(bases)

        # 3. Process Extras (Average of min/max)
        extras = []
        for ex in c_data.get("extras", []):
            item = ex.get("item", "Unknown").split(":")[-1].replace("_", " ").title()
            avg = (ex.get("minCount", 0) + ex.get("maxCount", 0)) // 2
            extras.append(f"{avg}x {item}")
        extras_str = ", ".join(extras) or "None"

        # 4. Meta Data
        purity = b_data.get("maxPurity", "N/A")

        rows.append([
            result_id.replace("_", " ").title(), 
            effects, 
            base_str, 
            extras_str, 
            str(purity)
        ])

    for r in rows:
        table += "| " + " | ".join(r) + " |\n"

    return table

if __name__ == "__main__":
    # Standard paths for your Brewery workspace
    table = generate_cocktail_table(
        Path(r"./src/main/resources/data/brewery/brew_types"), 
        Path(r"./src/main/resources/data/brewery/recipes")
    )
    print(table)

