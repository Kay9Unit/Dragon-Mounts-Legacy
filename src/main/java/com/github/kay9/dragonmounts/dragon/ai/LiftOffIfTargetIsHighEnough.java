package com.github.kay9.dragonmounts.dragon.ai;

import com.github.kay9.dragonmounts.dragon.TameableDragon;
import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;

import java.util.Optional;

// Is this temporary until writing a custom move behavior?
public class LiftOffIfTargetIsHighEnough extends Behavior<TameableDragon>
{
    private static final int HEIGHT_NEEDED = 3;
    private final int heightNeeded;

    public LiftOffIfTargetIsHighEnough()
    {
        this(HEIGHT_NEEDED);
    }

    public LiftOffIfTargetIsHighEnough(int heightNeeded)
    {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_PRESENT));
        this.heightNeeded = heightNeeded;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, TameableDragon dragon)
    {
        Optional<WalkTarget> walkTarget = dragon.getBrain().getMemory(MemoryModuleType.WALK_TARGET);
        double verticalDistance = walkTarget.get().getTarget().currentPosition().y - dragon.getY();
        return dragon.canLiftOff()
                && verticalDistance >= this.heightNeeded;
    }

    @Override
    protected void start(ServerLevel level, TameableDragon dragon, long gameTime)
    {
        dragon.liftOff();
    }
}
