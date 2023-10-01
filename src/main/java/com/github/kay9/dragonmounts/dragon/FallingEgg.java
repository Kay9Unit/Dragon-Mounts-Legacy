package com.github.kay9.dragonmounts.dragon;

import com.github.kay9.dragonmounts.DMLRegistry;
import com.github.kay9.dragonmounts.dragon.breed.BreedRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import static com.github.kay9.dragonmounts.dragon.DMLEggBlock.Item.create;

/**
 * Simplified {@link FallingBlockEntity}
 */
public class FallingEgg extends Entity
{
    private static final Logger LOGGER = LogUtils.getLogger();
    protected static final EntityDataAccessor<BlockPos> DATA_START_POS = SynchedEntityData.defineId(FallingEgg.class, EntityDataSerializers.BLOCK_POS);
    protected static final EntityDataAccessor<String> DATA_BREED = SynchedEntityData.defineId(FallingEgg.class, EntityDataSerializers.STRING);
    public int time;
    public int hatchTime;

    public FallingEgg(EntityType<? extends FallingEgg> type, Level level)
    {
        super(type, level);
        this.blocksBuilding = true;
        this.setDeltaMovement(Vec3.ZERO);
    }

    public void setBreed(String breed)
    {
        this.entityData.set(DATA_BREED, breed);
    }

    public String getBreed()
    {
        return this.entityData.get(DATA_BREED);
    }

    public CompoundTag saveBlockEntityData(CompoundTag compound)
    {
        compound.putInt(DragonEgg.NBT_HATCH_TIME, this.hatchTime);
        compound.putString(TameableDragon.NBT_BREED, this.getBreed());
        return compound;
    }

    public void dropItem(float offsetY)
    {
        this.spawnAtLocation(create(
                new ResourceLocation(this.getBreed()),
                this.saveBlockEntityData(new CompoundTag())
        ), offsetY);
    }

    @Override
    public boolean isAttackable()
    {
        return false;
    }

    public void setStartPos(BlockPos pStartPos)
    {
        this.entityData.set(DATA_START_POS, pStartPos);
    }

    public BlockPos getStartPos()
    {
        return this.entityData.get(DATA_START_POS);
    }

    @Override
    protected Entity.@NotNull MovementEmission getMovementEmission()
    {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected void defineSynchedData()
    {
        this.entityData.define(DATA_START_POS, BlockPos.ZERO);
        this.entityData.define(DATA_BREED, BreedRegistry.FIRE_BUILTIN.getId().toString());
    }

    @Override
    public boolean isPickable()
    {
        return !this.isRemoved();
    }

    @Override
    public void tick()
    {
        ++this.time;
        if (!this.isNoGravity())
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.04D, 0.0D));
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (!this.level.isClientSide)
        {
            BlockPos pos = this.blockPosition();
            if (!this.onGround)
            {
                if (!this.level.isClientSide && (this.time > 100 && (pos.getY() <= this.level.getMinBuildHeight() || pos.getY() > this.level.getMaxBuildHeight()) || this.time > 600))
                {
                    if (this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS))
                        this.dropItem(0);
                    this.discard();
                }
            }
            else
            {
                BlockState state = this.level.getBlockState(pos);
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.7D, -0.5D, 0.7D));
                if (!state.is(Blocks.MOVING_PISTON))
                {
                    if (state.canBeReplaced(new DirectionalPlaceContext(this.level, pos, Direction.DOWN, ItemStack.EMPTY, Direction.UP)) && !FallingBlock.isFree(this.level.getBlockState(pos.below())))
                    {
                        if (this.level.setBlock(pos, DMLRegistry.EGG_BLOCK.get().defaultBlockState(), 3))
                        {
                            ((ServerLevel) this.level).getChunkSource().chunkMap.broadcast(this, new ClientboundBlockUpdatePacket(pos, this.level.getBlockState(pos)));
                            this.discard();
                            BlockEntity blockentity = this.level.getBlockEntity(pos);
                            if (blockentity != null)
                            {
                                try
                                {
                                    blockentity.load(this.saveBlockEntityData(blockentity.saveWithoutMetadata()));
                                } catch (Exception exception)
                                {
                                    LOGGER.error("Failed to load block entity from falling block", exception);
                                }
                                blockentity.setChanged();
                            }
                        }
                        else if (this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS))
                        {
                            this.discard();
                            this.dropItem(0);
                        }
                    }
                    else
                    {
                        this.discard();
                        if (this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS))
                            this.dropItem(0);
                    }
                }
            }
        }
        this.setDeltaMovement(this.getDeltaMovement().scale(0.98D));
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, @NotNull DamageSource source)
    {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound)
    {
        compound.putInt("Time", this.time);
        this.saveBlockEntityData(compound);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound)
    {
        this.time = compound.getInt("Time");
        if (compound.contains(DragonEgg.NBT_HATCH_TIME))
            this.hatchTime = compound.getInt(DragonEgg.NBT_HATCH_TIME);
        if (compound.contains(TameableDragon.NBT_BREED))
            this.entityData.set(DATA_BREED, compound.getString(TameableDragon.NBT_BREED));
    }

    @Override
    public boolean displayFireAnimation()
    {
        return false;
    }

    @Override
    public @NotNull Packet<?> getAddEntityPacket()
    {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet)
    {
        super.recreateFromPacket(packet);
        this.blocksBuilding = true;
        this.setPos(packet.getX(), packet.getY(), packet.getZ());
        this.setStartPos(this.blockPosition());
    }
}
