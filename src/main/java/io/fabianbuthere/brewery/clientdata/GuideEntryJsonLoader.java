package io.fabianbuthere.brewery.clientdata;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import io.fabianbuthere.brewery.clientdata.model.GuideEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;

import java.util.Map;

public class GuideEntryJsonLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String DIRECTORY = "guide_entries";

    public GuideEntryJsonLoader() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, net.minecraft.util.profiling.ProfilerFiller profiler) {
        GuideRegistry.clear();
        int count = 0;
        for (Map.Entry<ResourceLocation, JsonElement> e : objects.entrySet()) {
            try {
                JsonObject json = GsonHelper.convertToJsonObject(e.getValue(), "guide_entry");
                String id = e.getKey().getPath();

                String titleKey = GsonHelper.getAsString(json, "title");
                String descriptionKey = GsonHelper.getAsString(json, "description");
                int tint = GsonHelper.getAsInt(json, "tint");
                String iconItem = GsonHelper.getAsString(json, "icon_item", "");
                int order = GsonHelper.getAsInt(json, "order", 0);

                GuideRegistry.register(new GuideEntry(id, titleKey, descriptionKey, tint, iconItem, order));
                count++;
            } catch (Exception ex) {
                LOGGER.error("Failed to load guide entry {}: {}", e.getKey(), ex.toString());
            }
        }
        LOGGER.info("Loaded {} guide entries.", count);
    }
}
