from pathlib import Path
import json

BREWS_PATH = Path(r"C:\Users\Fabian\Files\Source\Minecraft\Modding\brewery\src\main\resources\data\brewery\brew_types").resolve()
OUT_PATH = Path(r"C:\Users\Fabian\Files\Source\Minecraft\Modding\brewery\src\main\resources\assets\brewery\guide_entries").resolve()

def get_tint_color(entry: Path) -> int:
    with entry.open('r', encoding='utf-8') as file:
        data = json.load(file)
        return data.get("tintColor", 0)


for entry in BREWS_PATH.glob("*.json"):
    if not entry.is_file():
        continue
    id = entry.stem
    tint = get_tint_color(entry)

    out = {
        "title": f"brewery.brew.{id}.name",
        "description": f"brewery.brew.{id}.hint",
        "tint": tint,
        "icon_item": "minecraft:potion",
        "order": 0
    }

    out_path = OUT_PATH / f"{id}.json"
    with out_path.open('w', encoding='utf-8') as file:
        json.dump(out, file, indent=4, ensure_ascii=False)
    
