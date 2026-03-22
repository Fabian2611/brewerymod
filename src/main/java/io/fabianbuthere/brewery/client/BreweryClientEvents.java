package io.fabianbuthere.brewery.client;

import io.fabianbuthere.brewery.BreweryMod;
import io.fabianbuthere.brewery.config.BreweryConfig;
import io.fabianbuthere.brewery.util.BrewType;
import io.fabianbuthere.brewery.util.BrewTypeRegistry;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.jetbrains.annotations.Nullable;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@Mod.EventBusSubscriber(modid = BreweryMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BreweryClientEvents {
    public static void bakeConfig() {
        try {
            cachedTextures = new HashSet<>(BreweryConfig.TINTABLE_TEXTURES.get());
            BreweryMod.LOGGER.debug("Loaded tintable textures: {}", cachedTextures);
        } catch (Exception e) {
            BreweryMod.LOGGER.warn("Failed to load tintable textures config", e);
            cachedTextures = new HashSet<>();
        }
    }

    private static Set<String> cachedTextures = new HashSet<>();

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent event) {
        if (event.getConfig().getSpec() == BreweryConfig.CLIENT_CONFIG) {
            bakeConfig();
        }
    }

    @SubscribeEvent
    public static void onModelBakingCompleted(ModelEvent.ModifyBakingResult event) {
        if (cachedTextures.isEmpty()) {
            try {
                bakeConfig();
            } catch (Exception e) {
                BreweryMod.LOGGER.debug("Config not ready yet, will try again on reload");
            }
        }

        ResourceLocation targetKey = null;
        BakedModel targetModel = null;

        ModelResourceLocation standardKey = new ModelResourceLocation(new ResourceLocation("minecraft", "potion"), "inventory");
        if (event.getModels().containsKey(standardKey)) {
            targetKey = standardKey;
            targetModel = event.getModels().get(standardKey);
        } else {
            for (ResourceLocation loc : event.getModels().keySet()) {
                if (loc.toString().equals("minecraft:potion#inventory") ||
                        (loc.getNamespace().equals("minecraft") && loc.getPath().equals("item/potion"))) {
                    targetKey = loc;
                    targetModel = event.getModels().get(loc);
                    break;
                }
            }
        }

        if (targetModel != null && targetKey != null) {
            event.getModels().put(targetKey, new BreweryPotionModel(targetModel));
        } else {
            BreweryMod.LOGGER.error("BREWERY ERROR: FAILED to find potion model in registry!");
        }
    }

    public static class BreweryPotionModel extends BakedModelWrapper<BakedModel> {
        private final ItemOverrides overrides;
        private final Map<String, BakedModel> customTextureModels = new ConcurrentHashMap<>();

        public BreweryPotionModel(BakedModel original) {
            super(original);
            this.overrides = new BreweryItemOverrides(this, original.getOverrides());
        }

        @Override
        public ItemOverrides getOverrides() {
            return overrides;
        }

        public BakedModel getModelForTexture(String texturePath) {
            return customTextureModels.computeIfAbsent(texturePath, path -> {
                String cleanedPath = path;
                if (cleanedPath.endsWith(".png")) cleanedPath = cleanedPath.substring(0, cleanedPath.length() - 4);
                if (cleanedPath.contains("textures/")) cleanedPath = cleanedPath.replace("textures/", "");

                ResourceLocation textureLoc = new ResourceLocation(cleanedPath);
                return new CustomTextureBakedModel(this.originalModel, textureLoc);
            });
        }
    }

    public static class BreweryItemOverrides extends ItemOverrides {
        private final BreweryPotionModel parent;

        public BreweryItemOverrides(BreweryPotionModel parent, ItemOverrides original) {
            super();
            this.parent = parent;
        }

        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable net.minecraft.client.multiplayer.ClientLevel level, @Nullable net.minecraft.world.entity.LivingEntity entity, int seed) {
            if (stack.hasTag()) {
                if (stack.getOrCreateTag().contains("customTexture")) {
                    String texturePath = stack.getTag().getString("customTexture");
                    if (!texturePath.isEmpty()) {
                        return parent.getModelForTexture(texturePath);
                    }
                    return super.resolve(model, stack, level, entity, seed);
                }

                // Port pre-1.6.1 textures
                if (stack.getOrCreateTag().contains("brewTypeId")) {
                    String brewId = stack.getTag().getString("brewTypeId");
                    BrewType brewType = BrewTypeRegistry.get(brewId);

                    if (brewType != null) {
                        String defaultTexture = brewType.customTexture();
                        if (defaultTexture != null && !defaultTexture.isEmpty()) {
                            return parent.getModelForTexture(defaultTexture);
                        }
                    }
                }
            }
            return super.resolve(model, stack, level, entity, seed);
        }
    }

    public static class CustomTextureBakedModel implements BakedModel {
        private final BakedModel originalModel;
        private final ResourceLocation textureLocation;
        private List<BakedQuad> cachedQuads = null;

        public CustomTextureBakedModel(BakedModel original, ResourceLocation textureLocation) {
            this.originalModel = original;
            this.textureLocation = textureLocation;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
            if (side != null) {
                return Collections.emptyList();
            }

            if (cachedQuads == null) {
                try {
                    String layer0Texture = loadLayerTextureFromModel(textureLocation, "layer0");
                    ResourceLocation textureToUse = textureLocation;

                    if (layer0Texture != null && !layer0Texture.isEmpty()) {
                        textureToUse = new ResourceLocation(layer0Texture);
                    }

                    TextureAtlasSprite sprite = net.minecraft.client.Minecraft.getInstance()
                            .getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                            .apply(textureToUse);
                    cachedQuads = generateQuads(sprite, textureToUse);
                } catch (Exception e) {
                    BreweryMod.LOGGER.error("Failed to generate custom model for " + textureLocation, e);
                    cachedQuads = Collections.emptyList();
                }
            }
            return cachedQuads;
        }

        private boolean shouldApplyTinting(ResourceLocation modelId) {
            String modelIdString = modelId.getNamespace() + ":" + modelId.getPath();
            return cachedTextures.contains(modelIdString);
        }

        @Nullable
        private String loadLayerTextureFromModel(ResourceLocation modelId, String layerKey) {
            try {
                // "brewery:item/tea_jug_steaming" -> "models/item/tea_jug_steaming.json"
                String modelPath = "models/" + modelId.getPath() + ".json";
                ResourceLocation modelLocation = new ResourceLocation(modelId.getNamespace(), modelPath);

                var resource = net.minecraft.client.Minecraft.getInstance()
                        .getResourceManager()
                        .getResource(modelLocation);

                if (resource.isPresent()) {
                    try (InputStreamReader reader = new InputStreamReader(
                            resource.get().open(), StandardCharsets.UTF_8)) {

                        JsonObject modelJson = new Gson().fromJson(reader, JsonObject.class);

                        if (modelJson.has("textures")) {
                            JsonObject textures = modelJson.getAsJsonObject("textures");
                            if (textures.has(layerKey)) {
                                String layerValue = textures.get(layerKey).getAsString();

                                if (!layerValue.contains(":")) {
                                    layerValue = modelId.getNamespace() + ":" + layerValue;
                                }

                                BreweryMod.LOGGER.debug("Loaded {} texture from model {}: {}",
                                        layerKey, modelLocation, layerValue);
                                return layerValue;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                BreweryMod.LOGGER.debug("Could not load layer {} from model {}: {}",
                        layerKey, textureLocation, e.getMessage());
            }

            return null;
        }

        private List<BakedQuad> generateQuads(TextureAtlasSprite sprite, ResourceLocation usedTexture) {
            ItemModelGenerator generator = new ItemModelGenerator();
            FaceBakery bakery = new FaceBakery();
            List<BakedQuad> quads = new ArrayList<>();

            String layer0Path = loadLayerTextureFromModel(textureLocation, "layer0");
            boolean hasModelJson = (layer0Path != null && !layer0Path.isEmpty());

            boolean shouldTint = hasModelJson && shouldApplyTinting(textureLocation);

            for (int layer = 0; layer <= 1; layer++) {
                String layerKey = "layer" + layer;
                ResourceLocation layerTextureLocation;

                if (layer == 0) {
                    layerTextureLocation = hasModelJson ? new ResourceLocation(layer0Path) : textureLocation;
                } else {
                    String layerPath = loadLayerTextureFromModel(textureLocation, layerKey);
                    if (layerPath == null || layerPath.isEmpty()) continue;
                    layerTextureLocation = new ResourceLocation(layerPath);
                }

                try {
                    TextureAtlasSprite layerSprite = net.minecraft.client.Minecraft.getInstance()
                            .getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                            .apply(layerTextureLocation);

                    List<BlockElement> elements = generator.processFrames(layer, layerKey, layerSprite.contents());

                    for (BlockElement element : elements) {
                        for (Map.Entry<Direction, BlockElementFace> entry : element.faces.entrySet()) {
                            Direction direction = entry.getKey();
                            BlockElementFace face = entry.getValue();

                            int tintIndex = (shouldTint && layer == 0) ? 0 : -1;

                            BlockElementFace tintFace = new BlockElementFace(null, tintIndex, face.texture, face.uv);

                            BakedQuad quad = bakery.bakeQuad(
                                    element.from, element.to, tintFace, layerSprite, direction,
                                    BlockModelRotation.X0_Y0, element.rotation, true, layerTextureLocation
                            );
                            quads.add(quad);
                        }
                    }
                } catch (Exception e) {
                    BreweryMod.LOGGER.error("Failed to generate quads for layer {} of {}", layerKey, textureLocation, e);
                }

                if (!hasModelJson) break;
            }
            return quads;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return originalModel.useAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return originalModel.isGui3d();
        }

        @Override
        public boolean usesBlockLight() {
            return false;
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return net.minecraft.client.Minecraft.getInstance()
                    .getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                    .apply(textureLocation);
        }

        @Override
        public ItemTransforms getTransforms() {
            return originalModel.getTransforms();
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }
    }
}
