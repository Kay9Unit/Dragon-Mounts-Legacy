package com.github.kay9.dragonmounts.dragon.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import com.google.common.collect.ImmutableMap;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class SetWalkTargetToOwnerIfFarEnough extends Behavior<TamableAnimal>
{
    private static final int STOP_DISTANCE = 4;
    private static final int START_DISTANCE = 10;
    private final float speedModifier;
    private final int stopDistance;
    private final int startDistanceSqr;

    public SetWalkTargetToOwnerIfFarEnough(float speedModifier)
    {
        this(speedModifier, STOP_DISTANCE, START_DISTANCE);
    }

    public SetWalkTargetToOwnerIfFarEnough(float speedModifier, int stopDistance, int startDistance)
    {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT));
        this.speedModifier = speedModifier;
        this.stopDistance = stopDistance;
        this.startDistanceSqr = startDistance * startDistance;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, TamableAnimal animal)
    {
        LivingEntity owner = animal.getOwner();
        return owner != null
                && animal.distanceToSqr(owner) >= this.startDistanceSqr;
    }

    @Override
    protected void start(ServerLevel level, TamableAnimal animal, long gameTime)
    {
        LivingEntity owner = animal.getOwner();
        if (owner == null) return; // In theory, this should never be null because of checkExtraStartConditions
        animal.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(owner, this.speedModifier, this.stopDistance));
        animal.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(owner, true));
    }
}
