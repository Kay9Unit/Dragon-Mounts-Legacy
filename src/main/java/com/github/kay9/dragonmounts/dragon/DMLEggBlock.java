package com.github.kay9.dragonmounts.dragon;

import com.github.kay9.dragonmounts.DMLRegistry;
import com.github.kay9.dragonmounts.client.DragonEggRenderer;
import com.github.kay9.dragonmounts.dragon.breed.BreedRegistry;
import com.github.kay9.dragonmounts.dragon.breed.DragonBreed;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.IItemRenderProperties;
import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class DMLEggBlock extends DragonEggBlock implements EntityBlock
{
    public DMLEggBlock()
    {
        super(BlockBehaviour.Properties.of(Material.EGG, MaterialColor.COLOR_BLACK).strength(3f, 9f).lightLevel(s -> 1).noOcclusion());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState)
    {
        return new Entity(pPos, pState);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand pHand, BlockHitResult pHit)
    {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof Entity e)
        {
            startHatching(e.getBreed(level.registryAccess()), e.getHatchTime(), level, pos);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos at, Player player)
    {
        if (level.getBlockEntity(at) instanceof Entity e && e.getBreedId().getPath().equals("end"))
            super.attack(Blocks.DRAGON_EGG.defaultBlockState(), level, at, player); // hacky fix for breed resets...
    }

    @SuppressWarnings("deprecation")
    @Override
    public RenderShape getRenderShape(BlockState pState)
    {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    public static void startHatching(DragonBreed breed, Level level, BlockPos pos)
    {
        startHatching(breed, breed.hatchTime(), level, pos);
    }

    @Override
    protected void falling(FallingBlockEntity falling)
    {
        if (falling.level.getBlockEntity(falling.blockPosition()) instanceof Entity e)
            falling.blockData = e.saveWithoutMetadata();
    }

    public static void startHatching(DragonBreed breed, int hatchTime, Level level, BlockPos pos)
    {
        if (!level.isClientSide)
        {
            DragonEgg egg = DMLRegistry.DRAGON_EGG.get().create(level);
            egg.setEggBreed(breed);
            egg.setHatchTime(hatchTime);
            egg.setPos(pos.getX() + 0.5d, pos.getY() + 0.1d, pos.getZ() + 0.5d);
            level.addFreshEntity(egg);
        }
    }

    public static class Item extends BlockItem
    {
        public Item()
        {
            super(DMLRegistry.EGG_BLOCK.get(), new net.minecraft.world.item.Item.Properties().rarity(Rarity.RARE).tab(CreativeModeTab.TAB_MISC));
        }

        @Override
        public Component getName(ItemStack stack)
        {
            String name;
            var tag = stack.getTag();
            if (tag == null || (name = tag.getString("ItemName")).isEmpty())
                name = BreedRegistry.getFallback(Minecraft.getInstance().level.registryAccess())
                        .getTranslationKey(Minecraft.getInstance().level.registryAccess());
            return new TranslatableComponent(getDescriptionId(), new TranslatableComponent(name));
        }

        @Override
        public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> items)
        {
            if (FMLLoader.getDist().isClient() && allowdedIn(tab) && Minecraft.getInstance().level != null)
            {
                var reg = Minecraft.getInstance().level.registryAccess();
                for (DragonBreed breed : BreedRegistry.registry(reg))
                    items.add(Item.create(breed, reg, breed.hatchTime()));
            }
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltips, TooltipFlag pFlag)
        {
            super.appendHoverText(stack, level, tooltips, pFlag);

            var tag = stack.getTagElement("BlockEntityTag");
            int time;
            if (tag != null && (time = tag.getInt(DragonEgg.NBT_HATCH_TIME)) != 0)
                tooltips.add(new TranslatableComponent(getDescriptionId() + ".remaining_time", time / 20).withStyle(ChatFormatting.GRAY));

            var player = Minecraft.getInstance().player;
            if (player != null && player.getAbilities().instabuild)
                tooltips.add(new TranslatableComponent(getDescriptionId() + ".change_breeds")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC, ChatFormatting.UNDERLINE));
        }

        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand)
        {
            if (player.getAbilities().instabuild && target instanceof TameableDragon dragon)
            {
                var tag = stack.getTagElement("BlockEntityTag");
                if (tag != null)
                {
                    dragon.setBreed(BreedRegistry.get(tag.getString(TameableDragon.NBT_BREED), player.getLevel().registryAccess()));
                    return InteractionResult.sidedSuccess(player.level.isClientSide);
                }
            }
            return super.interactLivingEntity(stack, player, target, hand);
        }

        @Override
        public void initializeClient(Consumer<IItemRenderProperties> consumer)
        {
            consumer.accept(DragonEggRenderer.INSTANCE);
        }

        public static ItemStack create(DragonBreed breed, RegistryAccess reg, int hatchTime)
        {
            var rootTag = new CompoundTag();

            var bETag = new CompoundTag();
            bETag.putString(TameableDragon.NBT_BREED, breed.id(reg).toString());
            bETag.putInt(DragonEgg.NBT_HATCH_TIME, hatchTime);

            rootTag.put("BlockEntityTag", bETag);
            rootTag.putString("ItemName", breed.getTranslationKey(reg));

            var stack = new ItemStack(DMLRegistry.EGG_BLOCK_ITEM.get());
            stack.setTag(rootTag);
            return stack;
        }
    }

    public static class Entity extends BlockEntity
    {
        private ResourceLocation breedId;
        private int hatchTime;

        public Entity(BlockPos pWorldPosition, BlockState pBlockState)
        {
            super(DMLRegistry.EGG_BLOCK_ENTITY.get(), pWorldPosition, pBlockState);
            setBreedId(BreedRegistry.FIRE_BUILTIN.getId());
            setHatchTime(DragonEgg.DEFAULT_HATCH_TIME);
        }

        @Override
        protected void saveAdditional(CompoundTag tag)
        {
            super.saveAdditional(tag);
            tag.putString(TameableDragon.NBT_BREED, breedId.toString());
            tag.putInt(DragonEgg.NBT_HATCH_TIME, hatchTime);
        }

        @Override
        public void load(CompoundTag tag)
        {
            super.load(tag);
            setBreedId(new ResourceLocation(tag.getString(TameableDragon.NBT_BREED)));
            setHatchTime(tag.getInt(DragonEgg.NBT_HATCH_TIME));
        }

        @Nullable
        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket()
        {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        @Override
        public CompoundTag getUpdateTag()
        {
            return saveWithoutMetadata();
        }

        public DragonBreed getBreed(RegistryAccess reg)
        {
            return BreedRegistry.get(getBreedId(), reg);
        }

        public void setBreedId(ResourceLocation id)
        {
            this.breedId = id;
        }

        public ResourceLocation getBreedId()
        {
            return breedId;
        }

        public void setHatchTime(int time)
        {
            hatchTime = time;
        }

        public int getHatchTime()
        {
            return hatchTime;
        }
    }
}
