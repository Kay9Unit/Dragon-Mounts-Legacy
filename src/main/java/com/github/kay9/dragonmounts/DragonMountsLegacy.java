package com.github.kay9.dragonmounts;

import com.github.kay9.dragonmounts.client.*;
import com.github.kay9.dragonmounts.data.model.DragonModelPropertiesListener;
import com.github.kay9.dragonmounts.dragon.DragonSpawnEgg;
import com.github.kay9.dragonmounts.dragon.TameableDragon;
import com.github.kay9.dragonmounts.dragon.breed.BreedRegistry;
import com.github.kay9.dragonmounts.dragon.breed.DragonBreed;
import com.github.kay9.dragonmounts.dragon.egg.HatchableEggBlock;
import com.mojang.serialization.Codec;
import com.github.kay9.dragonmounts.dragon.breed.DragonBreed;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DragonMountsLegacy
{
    public static final String MOD_ID = "dragonmounts";
    public static final Logger LOG = LogManager.getLogger(MOD_ID);

    public static ResourceLocation id(String path)
    {
        return new ResourceLocation(MOD_ID, path);
    }

    // ========================
    //          Events
    // ========================

    static boolean overrideVanillaDragonEgg(Level level, BlockPos pos, Player player)
    {
        if (DMLConfig.allowEggOverride() && level.getBlockState(pos).is(Blocks.DRAGON_EGG))
        {
            var end = BreedRegistry.registry(level.registryAccess()).getOptional(DragonBreed.BuiltIn.END);
            if (end.isPresent())
            {
                if (level.isClientSide) player.swing(InteractionHand.MAIN_HAND);
                else
                {
                    var state = DMLRegistry.EGG_BLOCK.get().defaultBlockState().setValue(HatchableEggBlock.HATCHING, true);
                    HatchableEggBlock.place((ServerLevel) level, pos, state, end.get());
                }
                return true;
            }
        }
        return false;
    }

    static void registerEntityAttributes(BiConsumer<EntityType<? extends LivingEntity>, AttributeSupplier> registrar)
    {
        registrar.accept(DMLRegistry.DRAGON.get(), TameableDragon.createAttributes().build());
    }

    static void hookRegistry(TriConsumer<ResourceKey<Registry<DragonBreed>>, Codec<DragonBreed>, Codec<DragonBreed>> registrar)
    {
        registrar.accept(BreedRegistry.REGISTRY_KEY, DragonBreed.CODEC, DragonBreed.NETWORK_CODEC);
    }

    static void clientTick(boolean pre)
    {
        if (!pre) MountControlsMessenger.tick();
    }

    static void registerCreativeTabItems(ResourceKey<CreativeModeTab> tab, Consumer<ItemStack> registrar)
    {
        if (tab == CreativeModeTabs.SPAWN_EGGS) DragonSpawnEgg.populateTab(registrar);
        if (tab == CreativeModeTabs.FUNCTIONAL_BLOCKS) HatchableEggBlock.populateTab(registrar);
    }

    static void onKeyPress(int key, int action, int modifiers)
    {
        KeyMappings.handleKeyPress(key, action);
    }

    static void registerRenderers()
    {
        EntityRenderers.register(DMLRegistry.DRAGON.get(), DragonRenderer::new);
        ForgeHooksClient.registerLayerDefinition(DragonRenderer.MODEL_LOCATION, () -> DragonModel.createBodyLayer(DragonModel.Properties.STANDARD));
    }

    static void registerEggModelLoader(BiConsumer<String, IGeometryLoader<DragonEggModel>> registrar)
    {
        registrar.accept("dragon_egg", DragonEggModel.Loader.INSTANCE);
    }

    static void registerItemColors(ItemColors colors)
    {
        colors.register(DragonSpawnEgg::getColor, DMLRegistry.SPAWN_EGG.get());
    }

    @SuppressWarnings("ConstantConditions") // client instance is null on data gen
    static void registerReloadListenersEarly()
    {
        if (Minecraft.getInstance() != null)
        {
            ((ReloadableResourceManager) Minecraft.getInstance().getResourceManager()).registerReloadListener(DragonModelPropertiesListener.INSTANCE); // Dragon Model Properties need to be reloaded before Entity Models are!
        }
    }

    static void registerKeyBindings(Consumer<KeyMapping> registrar)
    {
        Keybinds.registerKeybinds(registrar);
    }
}