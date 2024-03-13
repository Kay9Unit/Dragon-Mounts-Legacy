package com.github.kay9.dragonmounts.dragon.ai;

import com.github.kay9.dragonmounts.dragon.TameableDragon;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;

public class TeleportToOwnerIfFarEnough extends Behavior<TameableDragon>
{
    private static final int MIN_HORIZONTAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING = 2;
    private static final int MAX_HORIZONTAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING = 3;
    private static final int MIN_VERTICAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING = 0;
    private static final int MAX_VERTICAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING = 1;
    private static final int TELEPORT_DISTANCE = 32;
    private final int teleportDistanceSqr;
    private TameableDragon dragon;
    private ServerLevel level;

    public TeleportToOwnerIfFarEnough()
    {
        this(TELEPORT_DISTANCE);
    }

    public TeleportToOwnerIfFarEnough(int teleportDistance)
    {
        super(ImmutableMap.of(MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT));
        this.teleportDistanceSqr = teleportDistance * teleportDistance;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, TameableDragon dragon)
    {
        LivingEntity owner = dragon.getOwner();
        return owner != null
                && !dragon.isOrderedToSit()
                && !dragon.isLeashed()
                && dragon.distanceToSqr(owner) >= this.teleportDistanceSqr;
    }

    @Override
    protected void start(ServerLevel level, TameableDragon dragon, long gameTime)
    {
        this.level = level;
        this.dragon = dragon;
        LivingEntity owner = dragon.getOwner();
        if (owner == null) return; // In theory, this should never be null because of checkExtraStartConditions

        BlockPos ownerPos = owner.blockPosition();

        for(int i = 0; i < 10; ++i)
        {
            BlockPos target = this.randomBlockPosNearPos(
                    ownerPos,
                    MIN_HORIZONTAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING,
                    MAX_HORIZONTAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING,
                    MIN_VERTICAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING,
                    MAX_VERTICAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING
            );
            boolean flag = this.maybeTeleportTo(target);
            if (flag)
            {
                return;
            }
        }
    }

    private boolean maybeTeleportTo(BlockPos pos)
    {
        LivingEntity owner = this.dragon.getOwner();
        if (owner.blockPosition().closerThan(pos, 2.0D))
        {
            return false;
        }
        if (!this.canTeleportTo(pos))
        {
            return false;
        }
        this.dragon.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ(), this.dragon.getYRot(), this.dragon.getXRot());
        this.dragon.getNavigation().stop();
        return true;
    }

    private boolean canTeleportTo(BlockPos pos)
    {
        if (!this.dragon.canFly())
        {
            BlockPathTypes blockpathtypes = WalkNodeEvaluator.getBlockPathTypeStatic(this.level, pos.mutable());
            if (blockpathtypes != BlockPathTypes.WALKABLE)
            {
                return false;
            }

            BlockState blockstate = this.level.getBlockState(pos.below());
            if (blockstate.getBlock() instanceof LeavesBlock)
            {
                return false;
            }
        }

        BlockPos blockPos = pos.subtract(this.dragon.blockPosition());
        AABB targetBoundingBox = this.dragon.getBoundingBox().move(blockPos);
        return this.level.noCollision(this.dragon, targetBoundingBox)
                && !this.level.containsAnyLiquid(targetBoundingBox);
    }

    private int randomIntInclusive(int min, int max)
    {
        return this.dragon.getRandom().nextInt(max - min + 1) + min;
    }

    private int randomIntInclusive(int farLow, int nearLow, int nearHigh, int farHigh)
    {
        if (nearLow == nearHigh)
        {
            return this.randomIntInclusive(farLow, farHigh);
        }

        return this.dragon.getRandom().nextBoolean() ?
                this.randomIntInclusive(farLow, nearLow) :
                this.randomIntInclusive(nearHigh, farHigh);
    }

    private BlockPos randomBlockPosNearPos(BlockPos origin, int minDist, int maxDist, int minYDist, int maxYDist)
    {
        int x = this.randomIntInclusive(-maxDist, -minDist, minDist, maxDist);
        int y = this.randomIntInclusive(-maxYDist, -minYDist, minYDist, maxYDist);
        int z = this.randomIntInclusive(-maxDist, -minDist, minDist, maxDist);
        return origin.offset(x, y, z);
    }
}
