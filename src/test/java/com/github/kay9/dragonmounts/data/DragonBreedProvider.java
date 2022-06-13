package com.github.kay9.dragonmounts.data;

import com.github.kay9.dragonmounts.DragonMountsLegacy;
import com.github.kay9.dragonmounts.abilities.FrostWalkerAbility;
import com.github.kay9.dragonmounts.abilities.GreenToesAbility;
import com.github.kay9.dragonmounts.abilities.SnowStepperAbility;
import com.github.kay9.dragonmounts.dragon.DragonBreed;
import com.github.kay9.dragonmounts.dragon.TameableDragon;
import com.github.kay9.dragonmounts.habitats.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

import java.io.IOException;
import java.util.Optional;

import static com.google.common.collect.ImmutableMap.of;

public class DragonBreedProvider implements DataProvider
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static final DragonBreed AETHER = new DragonBreed(DragonMountsLegacy.id("aether"),
            0x718AA9,
            0xE6E6E6,
            Optional.empty(),
            DragonBreed.ModelProperties.STANDARD,
            of(Attributes.FLYING_SPEED, TameableDragon.BASE_SPEED_FLYING * 1.45),
            list(),
            list(new HeightHabitat(false, 200)),
            set(),
            Optional.empty(),
            BuiltInLootTables.EMPTY,
            TameableDragon.DEFAULT_GROWTH_TIME);

    static final DragonBreed END = new DragonBreed(DragonMountsLegacy.id("end"),
            0x161616,
            0xff63e8,
            Optional.of(ParticleTypes.PORTAL),
            DragonBreed.ModelProperties.STANDARD,
            of(Attributes.MAX_HEALTH, TameableDragon.BASE_HEALTH * 1.25),
            list(), // teleport ability?
            list(DragonBreathHabitat.INSTANCE),
            set("dragonBreath"),
            Optional.empty(),
            BuiltInLootTables.EMPTY,
            TameableDragon.DEFAULT_GROWTH_TIME);

    static final DragonBreed FIRE = DragonBreed.FIRE;

    static final DragonBreed FOREST = new DragonBreed(DragonMountsLegacy.id("forest"),
            0x054a00,
            0x0a9600,
            Optional.of(ParticleTypes.HAPPY_VILLAGER),
            DragonBreed.ModelProperties.STANDARD,
            of(),
            list(GreenToesAbility.INSTANCE),
            list(new NearbyBlocksHabitat(BlockTagProvider.FOREST_DRAGON_HABITAT_BLOCKS), new BiomeHabitat(BiomeTagProvider.FOREST_DRAGON_BIOMES)),
            set(),
            Optional.empty(),
            BuiltInLootTables.EMPTY,
            TameableDragon.DEFAULT_GROWTH_TIME);

    static final DragonBreed GHOST = new DragonBreed(DragonMountsLegacy.id("ghost"),
            0xc4c4c4,
            0xc2f8ff,
            Optional.empty(),
            new DragonBreed.ModelProperties(true, false, true),
            of(),
            list(),
            list(new PickyHabitat(ImmutableList.of(new HeightHabitat(true, 0), new LightHabitat(true, 3)))),
            set("drown"),
            Optional.of(SoundEvents.SKELETON_AMBIENT),
            BuiltInLootTables.EMPTY,
            TameableDragon.DEFAULT_GROWTH_TIME);

    static final DragonBreed ICE = new DragonBreed(DragonMountsLegacy.id("ice"),
            0xffffff,
            0x00E1FF,
            Optional.of(ParticleTypes.SNOWFLAKE),
            DragonBreed.ModelProperties.STANDARD,
            of(),
            list(FrostWalkerAbility.INSTANCE, SnowStepperAbility.INSTANCE),
            list(new NearbyBlocksHabitat(BlockTagProvider.ICE_DRAGON_HABITAT_BLOCKS)),
            set("drown", "freeze"),
            Optional.empty(),
            BuiltInLootTables.EMPTY,
            TameableDragon.DEFAULT_GROWTH_TIME);

    static final DragonBreed NETHER = new DragonBreed(DragonMountsLegacy.id("nether"),
            0x912400,
            0x2e0b00,
            Optional.of(ParticleTypes.SOUL_FIRE_FLAME),
            DragonBreed.ModelProperties.STANDARD,
            of(Attributes.ARMOR, 8d),
            list(),
            list(new NearbyBlocksHabitat(BlockTagProvider.NETHER_DRAGON_HABITAT_BLOCKS), new BiomeHabitat(BiomeTagProvider.NETHER_DRAGON_BIOMES)),
            set("inFire", "onFire", "lava", "hotFloor"),
            Optional.empty(),
            BuiltInLootTables.EMPTY,
            TameableDragon.DEFAULT_GROWTH_TIME);

    static final DragonBreed WATER = new DragonBreed(DragonMountsLegacy.id("water"),
            0x0062ff,
            0x5999ff,
            Optional.of(ParticleTypes.DRIPPING_WATER),
            new DragonBreed.ModelProperties(true, true, false),
            of(),
            list(),
            list(new FluidHabitat(FluidTags.WATER)),
            set("drown"),
            Optional.empty(),
            BuiltInLootTables.EMPTY,
            TameableDragon.DEFAULT_GROWTH_TIME);

    private final DataGenerator generator;

    public DragonBreedProvider(DataGenerator generator)
    {
        this.generator = generator;
    }

    @Override
    public void run(CachedOutput cache) throws IOException
    {
        for (var breed : new DragonBreed[]{AETHER, END, FIRE, FOREST, GHOST, ICE, NETHER, WATER})
        {
            String path = breed.id().getNamespace();
            String name = breed.id().getPath();
            DataProvider.saveStable(cache,
                    DragonBreed.CODEC.encodeStart(JsonOps.INSTANCE, breed).getOrThrow(false, DragonMountsLegacy.LOG::error),
                    generator.getOutputFolder().resolve("data/" + path + "/dragon_breeds/" + name + ".json"));
        }
    }

    @Override
    public String getName()
    {
        return "Dragon Breeds";
    }

    private static <T> ImmutableList<T> list(T... objs)
    {
        return ImmutableList.copyOf(objs);
    }

    private static <T> ImmutableSet<T> set(T... objs)
    {
        return ImmutableSet.copyOf(objs);
    }
}
