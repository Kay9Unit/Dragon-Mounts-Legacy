package com.github.kay9.dragonmounts;

import com.github.kay9.dragonmounts.data.loot.DragonEggLootMod;
import com.github.kay9.dragonmounts.dragon.*;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryManager;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static net.minecraftforge.registries.ForgeRegistries.Keys;

public class DMLRegistry
{
    protected static void init(IEventBus bus)
    {
        REGISTRIES.values().forEach(r -> r.register(bus));
        REGISTRIES.clear(); // Registration happens once, no need to stick around.
    }

    private static final Map<ResourceKey<? extends Registry<?>>, DeferredRegister<?>> REGISTRIES = new HashMap<>();

    public static final RegistryObject<Block> EGG_BLOCK = register("dragon_egg", Keys.BLOCKS, DMLEggBlock::new);

    public static final RegistryObject<Item> EGG_BLOCK_ITEM = register(EGG_BLOCK.getId().getPath(), Keys.ITEMS, DMLEggBlock.Item::new);
    public static final RegistryObject<Item> SPAWN_EGG = register("spawn_egg", Keys.ITEMS, DragonSpawnEgg::new);

    public static final RegistryObject<SoundEvent> DRAGON_AMBIENT_SOUND = sound("entity.dragon.ambient");
    public static final RegistryObject<SoundEvent> DRAGON_STEP_SOUND = sound("entity.dragon.step");
    public static final RegistryObject<SoundEvent> DRAGON_DEATH_SOUND = sound("entity.dragon.death");
    public static final RegistryObject<SoundEvent> GHOST_DRAGON_AMBIENT = sound("entity.dragon.ambient.ghost");

    public static final RegistryObject<EntityType<TameableDragon>> DRAGON = entity("dragon", EntityType.Builder   .of(TameableDragon::new, MobCategory.CREATURE).sized(TameableDragon.BASE_WIDTH, TameableDragon.BASE_HEIGHT).clientTrackingRange(10).updateInterval(3));
    public static final RegistryObject<EntityType<DragonEgg>> DRAGON_EGG = entity("dragon_egg", EntityType.Builder.of(DragonEgg::new, MobCategory.MISC)         .sized(DragonEgg.WIDTH, DragonEgg.HEIGHT)                    .clientTrackingRange(5) .updateInterval(8));
    public static final RegistryObject<EntityType<FallingEgg>> FALLING_EGG = entity("falling_egg", EntityType.Builder.of(FallingEgg::new, MobCategory.MISC)     .sized(0.98F, 0.98F)                          .clientTrackingRange(10).updateInterval(20));

    public static final RegistryObject<BlockEntityType<DMLEggBlock.Entity>> EGG_BLOCK_ENTITY = register("dragon_egg", Keys.BLOCK_ENTITY_TYPES, () -> BlockEntityType.Builder.of(DMLEggBlock.Entity::new, EGG_BLOCK.get()).build(null));

    public static final RegistryObject<GlobalLootModifierSerializer<DragonEggLootMod>> EGG_LOOT_MODIFIER = register("dragon_egg_loot", Keys.LOOT_MODIFIER_SERIALIZERS, DragonEggLootMod.Serializer::new);

    private static <T extends Entity> RegistryObject<EntityType<T>> entity(String name, EntityType.Builder<T> builder)
    {
        return register(name, Keys.ENTITY_TYPES, () -> builder.build(DragonMountsLegacy.MOD_ID + ":" + name));
    }

    private static RegistryObject<SoundEvent> sound(String name)
    {
        return register(name, Keys.SOUND_EVENTS, () -> new SoundEvent(DragonMountsLegacy.id(name)));
    }

    @SuppressWarnings("unchecked")
    private static <T extends IForgeRegistryEntry<T>, I extends T> RegistryObject<I> register(String name, ResourceKey<Registry<T>> forType, Supplier<? extends I> sup)
    {
        var registry = (DeferredRegister<T>) REGISTRIES.computeIfAbsent(forType, t ->
        {
            var fr = RegistryManager.ACTIVE.getRegistry(forType);
            if (fr == null) return DeferredRegister.create(forType, DragonMountsLegacy.MOD_ID);
            return DeferredRegister.create(fr, DragonMountsLegacy.MOD_ID);
        });
        return registry.register(name, sup);
    }
}
