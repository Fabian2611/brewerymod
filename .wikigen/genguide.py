import os
import json

# Paths defined by your structure
SOURCE_DIR = "/home/fabian/files/Source/Minecraft/Modding/brewery/src/main/resources/data/brewery/brew_types/"
DEST_DIR = "/home/fabian/files/Source/Minecraft/Modding/brewery/src/main/resources/assets/brewery/guide_entries/"

def generate_guide_entries():
    # Ensure the destination directory exists
    if not os.path.exists(DEST_DIR):
        os.makedirs(DEST_DIR)

    # Process every .json file in the source directory
    for filename in os.listdir(SOURCE_DIR):
        if not filename.endswith(".json"):
            continue

        source_path = os.path.join(SOURCE_DIR, filename)
        dest_path = os.path.join(DEST_DIR, filename)

        # Only proceed if the file doesn't already exist in the destination
        if os.path.exists(dest_path):
            print(f"Skipping {filename}: Already exists in destination.")
            continue

        try:
            with open(source_path, 'r') as f:
                data = json.load(f)

            # Logic for icon_item mapping
            # If customTexture exists (e.g. "minecraft:item/suspicious_stew"), 
            # we strip the "item/" part to get the valid item ID.
            icon_item = "minecraft:potion"
            if "customTexture" in data:
                raw_texture = data["customTexture"]
                icon_item = raw_texture.replace("item/", "")

            # Construct the new JSON structure
            guide_entry = {
                "title": data.get("customName", ""),
                "description": data.get("customName", "").replace(".name", ".hint"),
                "tint": data.get("tintColor", 0),
                "icon_item": icon_item,
                "order": 0
            }

            # Save the new file
            with open(dest_path, 'w') as f:
                json.dump(guide_entry, f, indent=4)
            
            print(f"Generated: {filename}")

        except Exception as e:
            print(f"Failed to process {filename}: {e}")

if __name__ == "__main__":
    generate_guide_entries()
