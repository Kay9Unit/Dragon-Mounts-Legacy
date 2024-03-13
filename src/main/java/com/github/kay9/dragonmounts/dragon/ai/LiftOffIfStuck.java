package com.github.kay9.dragonmounts.dragon.ai;

import com.github.kay9.dragonmounts.dragon.TameableDragon;
import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

// Is this temporary until writing a custom move behavior?
public class LiftOffIfStuck extends Behavior<TameableDragon>
{
    private static final long TIME_STUCK_TO_LIFT_OFF = 100;
    private final long timeStuckToLiftOff;

    public LiftOffIfStuck()
    {
        this(TIME_STUCK_TO_LIFT_OFF);
    }

    public LiftOffIfStuck(long timeStuckToLiftOff)
    {
        super(ImmutableMap.of(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryStatus.VALUE_PRESENT));
        this.timeStuckToLiftOff = timeStuckToLiftOff;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, TameableDragon dragon)
    {
        return dragon.canLiftOff()
                && dragon.getBrain().getMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE).filter(cantReachSince -> level.getGameTime() - cantReachSince >= this.timeStuckToLiftOff).isPresent();
    }

    @Override
    protected void start(ServerLevel level, TameableDragon dragon, long gameTime)
    {
        dragon.liftOff();
    }
}
