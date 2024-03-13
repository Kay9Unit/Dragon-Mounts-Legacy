package com.github.kay9.dragonmounts.dragon.ai;

import com.github.kay9.dragonmounts.DMLRegistry;
import com.github.kay9.dragonmounts.dragon.TameableDragon;

import com.mojang.datafixers.util.Pair;

import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.schedule.Activity;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;

import java.util.Optional;

public class DragonAi
{
    private static final UniformInt RETREAT_DURATION = TimeUtil.rangeOfSeconds(5, 20);
    private static final long RETALIATE_DURATION = 200L;
    protected static final ImmutableList<? extends SensorType<? extends Sensor<? super TameableDragon>>> SENSOR_TYPES = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS);
    protected static final ImmutableList<? extends MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(MemoryModuleType.BREED_TARGET, MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER, MemoryModuleType.LOOK_TARGET, MemoryModuleType.WALK_TARGET, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.PATH, MemoryModuleType.ATTACK_TARGET, MemoryModuleType.ATTACK_COOLING_DOWN, MemoryModuleType.NEAREST_VISIBLE_ADULT, MemoryModuleType.AVOID_TARGET, DMLRegistry.SITTING.get());

    public static Brain.Provider<TameableDragon> brainProvider()
    {
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    public static Brain<?> makeBrain(Brain<TameableDragon> brain)
    {
        initCoreActivity(brain);
        initIdleActivity(brain);
        initFightActivity(brain);
        initAvoidActivity(brain);
        initSitActivity(brain);
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();
        return brain;
    }

    private static void initCoreActivity(Brain<TameableDragon> brain)
    {
        brain.addActivity(Activity.CORE, 0, ImmutableList.of(
                new Swim(0.8f),
                new SitWhenOrderedTo(),
                new FightWithOwner(),
                new LookAtTargetSink(45, 90),
                new LiftOffIfTargetIsHighEnough(),
                new LiftOffIfStuck(),
                new MoveToTargetSink()));
    }

    private static void initIdleActivity(Brain<TameableDragon> brain)
    {
        brain.addActivity(Activity.IDLE, 10, ImmutableList.of(
                new AnimalMakeLove(DMLRegistry.DRAGON.get(), 1.0f),
                new TeleportToOwnerIfFarEnough(),
                new SetWalkTargetToOwnerIfFarEnough(1.0f),
                new RunSometimes<>(new SetEntityLookTarget(EntityType.PLAYER, 10.0f), UniformInt.of(30, 60)),
                new StartAttacking<>(DragonAi::canAttackRandomly, DragonAi::findNearestValidAttackTarget),
                getIdleMovementBehaviors()));
    }

    private static void initFightActivity(Brain<TameableDragon> brain)
    {
        brain.addActivityAndRemoveMemoryWhenStopped(Activity.FIGHT, 10, ImmutableList.of(
                new AnimalMakeLove(DMLRegistry.DRAGON.get(), 1.0F),
                new SetWalkTargetFromAttackTargetIfTargetOutOfReach(1.0F),
                new RunIf<>(TameableDragon::isAdult, new MeleeAttack(40)),
                new RunIf<>(TameableDragon::isJuvenile, new MeleeAttack(15)),
                new StopAttackingIfTargetInvalid<>(),
                new EraseMemoryIf<>(DragonAi::wantsToStopFighting, MemoryModuleType.ATTACK_TARGET)
        ), MemoryModuleType.ATTACK_TARGET);
    }

    private static void initAvoidActivity(Brain<TameableDragon> brain)
    {
        brain.addActivityAndRemoveMemoryWhenStopped(Activity.AVOID, 10, ImmutableList.of(
                SetWalkTargetAwayFrom.entity(MemoryModuleType.AVOID_TARGET, 1.5f, 16, false),
                new RunSometimes<>(new SetEntityLookTarget(EntityType.PLAYER, 10.0F), UniformInt.of(30, 60)),
                getIdleMovementBehaviors(),
                new EraseMemoryIf<>(DragonAi::wantsToStopFleeing, MemoryModuleType.AVOID_TARGET)
        ), MemoryModuleType.AVOID_TARGET);
    }

    private static void initSitActivity(Brain<TameableDragon> brain)
    {
        brain.addActivityAndRemoveMemoryWhenStopped(DMLRegistry.SIT.get(), 10, ImmutableList.of(
                new RunSometimes<>(new SetEntityLookTarget(EntityType.PLAYER, 10.0F), UniformInt.of(30, 60))
        ), DMLRegistry.SITTING.get());
    }

    private static RunOne<TameableDragon> getIdleMovementBehaviors()
    {
        return new RunOne<>(ImmutableList.of(
                Pair.of(new RandomStroll(1.0f), 2),
                Pair.of(new SetWalkTargetFromLookTarget(1.0f, 3), 2),
                Pair.of(new DoNothing(30, 60), 1)));
    }

    public static void updateActivity(TameableDragon dragon)
    {
        dragon.getBrain().setActiveActivityToFirstValid(ImmutableList.of(
                Activity.FIGHT,
                Activity.AVOID,
                DMLRegistry.SIT.get(),
                Activity.IDLE));
    }

    private static boolean wantsToStopFighting(TameableDragon dragon)
    {
        // If we trigger breeding or the target becomes an ally, stop attacking.
        return dragon.getBrain().hasMemoryValue(MemoryModuleType.BREED_TARGET)
                || dragon.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).filter(target -> dragon.wantsToAttack(target, dragon.getOwner())).isEmpty();
    }

    public static void wasHurtBy(TameableDragon dragon, LivingEntity attacker)
    {
        Brain<TameableDragon> brain = dragon.getBrain();
        brain.eraseMemory(MemoryModuleType.BREED_TARGET);
        if (dragon.isBaby()) // Should juveniles have their own behavior?
        {
            retreatFromNearestTarget(dragon, attacker);
        }
        else
        {
            maybeRetaliate(dragon, attacker);
        }
    }

    private static void maybeRetaliate(TameableDragon dragon, LivingEntity attacker)
    {
        if (!dragon.getBrain().isActive(Activity.AVOID)
                && !BehaviorUtils.isOtherTargetMuchFurtherAwayThanCurrentAttackTarget(dragon, attacker, 4.0D)
                && Sensor.isEntityAttackable(dragon, attacker)
                && dragon.wantsToAttack(attacker, dragon.getOwner()))
        {
            setAttackTarget(dragon, attacker);
        }
    }

    private static void setAttackTarget(TameableDragon dragon, LivingEntity target)
    {
        Brain<TameableDragon> brain = dragon.getBrain();
        net.minecraftforge.event.entity.living.LivingChangeTargetEvent changeTargetEvent = net.minecraftforge.common.ForgeHooks.onLivingChangeTarget(dragon, target, net.minecraftforge.event.entity.living.LivingChangeTargetEvent.LivingTargetType.BEHAVIOR_TARGET);
        if(!changeTargetEvent.isCanceled()) {
            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            brain.eraseMemory(MemoryModuleType.BREED_TARGET);
            brain.setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, target, RETALIATE_DURATION);
            net.minecraftforge.common.ForgeHooks.onLivingSetAttackTarget(dragon, changeTargetEvent.getNewTarget(), net.minecraftforge.event.entity.living.LivingChangeTargetEvent.LivingTargetType.BEHAVIOR_TARGET); // TODO: Remove in 1.20
        }
    }

    private static void retreatFromNearestTarget(TameableDragon dragon, LivingEntity target)
    {
        Brain<TameableDragon> brain = dragon.getBrain();
        LivingEntity avoidTarget = BehaviorUtils.getNearestTarget(dragon, brain.getMemory(MemoryModuleType.AVOID_TARGET), target);
        avoidTarget = BehaviorUtils.getNearestTarget(dragon, brain.getMemory(MemoryModuleType.ATTACK_TARGET), avoidTarget);
        // At this point avoidTarget is the closest of target, AVOID_TARGET, and ATTACK_TARGET
        setAvoidTarget(dragon, avoidTarget);
    }

    private static void setAvoidTarget(TameableDragon dragon, LivingEntity target)
    {
        Brain<TameableDragon> brain = dragon.getBrain();
        brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        brain.setMemoryWithExpiry(MemoryModuleType.AVOID_TARGET, target, RETREAT_DURATION.sample(dragon.level.random));
    }

    private static boolean wantsToStopFleeing(TameableDragon dragon)
    {
        return dragon.isAdult();
    }

    private static Optional<? extends LivingEntity> findNearestValidAttackTarget(TameableDragon dragon)
    {
        return dragon.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
                .orElse(NearestVisibleLivingEntities.empty())
                .findClosest(e -> e instanceof Animal && !(e instanceof TameableDragon) && Sensor.isEntityAttackable(dragon, e));
    }

    private static boolean canAttackRandomly(TameableDragon dragon)
    {
        return dragon.isAdult() && !dragon.isTame();
    }
}
