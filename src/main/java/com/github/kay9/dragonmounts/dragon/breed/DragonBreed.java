package com.github.kay9.dragonmounts.dragon.breed;

import com.github.kay9.dragonmounts.DMLConfig;
import com.github.kay9.dragonmounts.DMLRegistry;
import com.github.kay9.dragonmounts.DragonMountsLegacy;
import com.github.kay9.dragonmounts.abilities.Ability;
import com.github.kay9.dragonmounts.dragon.TameableDragon;
import com.github.kay9.dragonmounts.dragon.egg.HatchableEggBlock;
import com.github.kay9.dragonmounts.habitats.Habitat;
import com.github.kay9.dragonmounts.util.DMLUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@SuppressWarnings("deprecation")
public record DragonBreed(int primaryColor, int secondaryColor, Optional<ParticleOptions> hatchParticles,
                          Map<Attribute, Double> attributes, List<Ability.Factory<Ability>> abilityTypes, List<Habitat> habitats,
                          HolderSet<DamageType> immunities, Optional<Holder<SoundEvent>> ambientSound,
                          ResourceLocation deathLoot, int growthTime, float hatchChance, float sizeModifier,
                          HolderSet<Item> tamingItems, HolderSet<Item> breedingItems, Either<Integer, String> reproLimit)
{
    public static final Codec<DragonBreed> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DMLUtil.HEX_CODEC.fieldOf("primary_color").forGetter(DragonBreed::primaryColor),
            DMLUtil.HEX_CODEC.fieldOf("secondary_color").forGetter(DragonBreed::secondaryColor),
            ParticleTypes.CODEC.optionalFieldOf("hatch_particles").forGetter(DragonBreed::hatchParticles),
            Codec.unboundedMap(BuiltInRegistries.ATTRIBUTE.byNameCodec(), Codec.DOUBLE).optionalFieldOf("attributes", ImmutableMap.of()).forGetter(DragonBreed::attributes),
            Ability.CODEC.listOf().optionalFieldOf("abilities", ImmutableList.of()).forGetter(DragonBreed::abilityTypes),
            Habitat.CODEC.listOf().optionalFieldOf("habitats", ImmutableList.of()).forGetter(DragonBreed::habitats),
            RegistryCodecs.homogeneousList(Registries.DAMAGE_TYPE).optionalFieldOf("immunities", HolderSet.direct()).forGetter(DragonBreed::immunities),
            SoundEvent.CODEC.optionalFieldOf("ambient_sound").forGetter(DragonBreed::ambientSound),
            ResourceLocation.CODEC.optionalFieldOf("death_loot", BuiltInLootTables.EMPTY).forGetter(DragonBreed::deathLoot),
            Codec.INT.optionalFieldOf("growth_time", TameableDragon.BASE_GROWTH_TIME).forGetter(DragonBreed::growthTime),
            Codec.FLOAT.optionalFieldOf("hatch_chance", HatchableEggBlock.DEFAULT_HATCH_CHANCE).forGetter(DragonBreed::hatchChance),
            Codec.FLOAT.optionalFieldOf("size_modifier", TameableDragon.BASE_SIZE_MODIFIER).forGetter(DragonBreed::sizeModifier),
            RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("taming_items", BuiltInRegistries.ITEM.getOrCreateTag(ItemTags.FISHES)).forGetter(DragonBreed::tamingItems),
            RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("breeding_items", BuiltInRegistries.ITEM.getOrCreateTag(ItemTags.FISHES)).forGetter(DragonBreed::breedingItems),
            Codec.either(Codec.INT, Codec.STRING).optionalFieldOf("reproduction_limit", Either.left(-1)).forGetter(DragonBreed::reproLimit)
    ).apply(instance, DragonBreed::new));
    public static final Codec<DragonBreed> NETWORK_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("primary_color").forGetter(DragonBreed::primaryColor),
            Codec.INT.fieldOf("secondary_color").forGetter(DragonBreed::secondaryColor),
            ParticleTypes.CODEC.optionalFieldOf("hatch_particles").forGetter(DragonBreed::hatchParticles),
            SoundEvent.CODEC.optionalFieldOf("ambient_sound").forGetter(DragonBreed::ambientSound),
            Codec.INT.fieldOf("growth_time").forGetter(DragonBreed::growthTime),
            Codec.FLOAT.optionalFieldOf("size_modifier", TameableDragon.BASE_SIZE_MODIFIER).forGetter(DragonBreed::sizeModifier)
    ).apply(instance, DragonBreed::fromNetwork));

    public static DragonBreed fromNetwork(int primaryColor, int secondaryColor, Optional<ParticleOptions> hatchParticles, Optional<Holder<SoundEvent>> ambientSound, int growthTime, float sizeModifier)
    {
        return new DragonBreed(primaryColor, secondaryColor, hatchParticles, Map.of(), List.of(), List.of(), HolderSet.direct(), ambientSound, BuiltInLootTables.EMPTY, growthTime, 0, sizeModifier, HolderSet.direct(), HolderSet.direct(), Either.left(0));
    }

    public void initialize(TameableDragon dragon)
    {
        applyAttributes(dragon);
        for (var factory : abilityTypes())
        {
            var instance = factory.create();
            dragon.getAbilities().add(instance);
            instance.initialize(dragon);
        }

        if (dragon.fireImmune())
        {
            dragon.setPathfindingMalus(BlockPathTypes.LAVA, 8f);
            dragon.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 0f);
            dragon.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 0f);
        }
    }

    public void close(TameableDragon dragon)
    {
        cleanAttributes(dragon);
        for (Ability ability : dragon.getAbilities()) ability.close(dragon);
        dragon.getAbilities().clear();
    }

    public int getReproductionLimit()
    {
        return reproLimit().map(Function.identity(), DMLConfig::getReproLimitFor);
    }

    public ResourceLocation id(RegistryAccess reg)
    {
        return BreedRegistry.registry(reg).getKey(this);
    }

    public static String getTranslationKey(String resourceLocation)
    {
        return "dragon_breed." + resourceLocation.replace(':', '.');
    }

    private void applyAttributes(TameableDragon dragon)
    {
        float healthFrac = dragon.getHealthFraction(); // in case max health is changed

        attributes().forEach((att, value) ->
        {
            AttributeInstance inst = dragon.getAttribute(att);
            if (inst != null) inst.setBaseValue(value);
        });

        dragon.setHealth(dragon.getMaxHealth() * healthFrac);
    }

    private void cleanAttributes(TameableDragon dragon)
    {
        float healthFrac = dragon.getHealthFraction(); // in case max health is changed
        var defaults = DefaultAttributes.getSupplier(DMLRegistry.DRAGON.get());

        attributes().forEach((att, value) ->
        {
            var instance = dragon.getAttribute(att);
            if (instance != null)
            {
                instance.removeModifiers();
                instance.setBaseValue(defaults.getBaseValue(att));
            }
        });

        dragon.setHealth(dragon.getMaxHealth() * healthFrac);
    }

    public static final class BuiltIn
    {
        public static final ResourceKey<DragonBreed> AETHER = key("aether");
        public static final ResourceKey<DragonBreed> END = key("end");
        public static final ResourceKey<DragonBreed> FIRE = key("fire");
        public static final ResourceKey<DragonBreed> FOREST = key("forest");
        public static final ResourceKey<DragonBreed> GHOST = key("ghost");
        public static final ResourceKey<DragonBreed> ICE = key("ice");
        public static final ResourceKey<DragonBreed> NETHER = key("nether");
        public static final ResourceKey<DragonBreed> WATER = key("water");

        private static ResourceKey<DragonBreed> key(String id)
        {
            return ResourceKey.create(BreedRegistry.REGISTRY_KEY, DragonMountsLegacy.id(id));
        }

        private BuiltIn() {}
    }
}