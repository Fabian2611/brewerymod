package io.fabianbuthere.brewery.client;

import io.fabianbuthere.brewery.BreweryMod;
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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = BreweryMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BreweryClientEvents {

    @SubscribeEvent
    public static void onModelBakingCompleted(ModelEvent.ModifyBakingResult event) {
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
                if (stack.getTag().contains("customTexture")) {
                    String texturePath = stack.getTag().getString("customTexture");
                    if (!texturePath.isEmpty()) {
                        return parent.getModelForTexture(texturePath);
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
                     TextureAtlasSprite sprite = net.minecraft.client.Minecraft.getInstance()
                            .getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                            .apply(textureLocation);
                     cachedQuads = generateQuads(sprite);
                 } catch (Exception e) {
                     BreweryMod.LOGGER.error("Failed to generate custom model for " + textureLocation, e);
                     cachedQuads = Collections.emptyList();
                 }
            }
            return cachedQuads;
        }

        private List<BakedQuad> generateQuads(TextureAtlasSprite sprite) {
            ItemModelGenerator generator = new ItemModelGenerator();
            // processFrames(tintIndex, animationKey, spriteContents)
            List<BlockElement> elements = generator.processFrames(0, "layer0", sprite.contents());
            
            FaceBakery bakery = new FaceBakery();
            List<BakedQuad> quads = new ArrayList<>();
            
            for (BlockElement element : elements) {
                for (Map.Entry<Direction, BlockElementFace> entry : element.faces.entrySet()) {
                    Direction direction = entry.getKey();
                    BlockElementFace face = entry.getValue();
                    
                    // Force tintIndex to -1 (no tint) so the potion color doesn't override the texture
                    // Use null for cullFor (standard for items) to avoid field access issues if cullFor is private/renamed
                    BlockElementFace noTintFace = new BlockElementFace(null, -1, face.texture, face.uv);

                    BakedQuad quad = bakery.bakeQuad(
                        element.from, 
                        element.to, 
                        noTintFace, 
                        sprite, 
                        direction, 
                        BlockModelRotation.X0_Y0, 
                        element.rotation, 
                        true, 
                        textureLocation
                    );
                    quads.add(quad);
                }
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