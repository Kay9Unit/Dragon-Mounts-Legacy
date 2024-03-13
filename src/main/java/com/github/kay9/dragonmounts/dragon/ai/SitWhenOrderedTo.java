package com.github.kay9.dragonmounts.dragon.ai;

import com.github.kay9.dragonmounts.DMLRegistry;
import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class SitWhenOrderedTo extends Behavior<TamableAnimal>
{
    public SitWhenOrderedTo()
    {
        super(ImmutableMap.of(DMLRegistry.SITTING.get(), MemoryStatus.REGISTERED));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, TamableAnimal animal)
    {
        return animal.isTame()
                && animal.isOrderedToSit()
                && !animal.isInWaterOrBubble();
    }

    @Override
    protected void start(ServerLevel level, TamableAnimal animal, long gameTime)
    {
        animal.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
        animal.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        animal.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        animal.getBrain().setMemory(DMLRegistry.SITTING.get(), true);
        animal.setInSittingPose(true);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, TamableAnimal animal, long gameTime)
    {
        // Don't call super here because it just returns false.
        return animal.isOrderedToSit()
                && !animal.isInWaterOrBubble();
    }

    @Override
    protected boolean timedOut(long pGameTime)
    {
        // Don't want the dragons to randomly stand up.
        return false;
    }

    @Override
    protected void stop(ServerLevel level, TamableAnimal animal, long gameTime)
    {
        animal.setInSittingPose(false);
        animal.getBrain().eraseMemory(DMLRegistry.SITTING.get());
    }
}
