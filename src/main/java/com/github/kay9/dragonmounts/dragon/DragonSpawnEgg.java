package com.github.kay9.dragonmounts.dragon;

import com.github.kay9.dragonmounts.DMLRegistry;
import com.github.kay9.dragonmounts.dragon.breed.BreedRegistry;
import com.github.kay9.dragonmounts.dragon.breed.DragonBreed;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.fml.loading.FMLLoader;

@SuppressWarnings("DataFlowIssue")
public class DragonSpawnEgg extends ForgeSpawnEggItem
{
    private static final String DATA_TAG = "ItemData";
    private static final String DATA_ITEM_NAME = "ItemName";
    private static final String DATA_PRIM_COLOR = "PrimaryColor";
    private static final String DATA_SEC_COLOR = "SecondaryColor";

    public DragonSpawnEgg()
    {
        super(DMLRegistry.DRAGON, 0, 0, new Item.Properties().tab(CreativeModeTab.TAB_MISC));
    }

    @Override
    public void fillItemCategory(CreativeModeTab pCategory, NonNullList<ItemStack> pItems)
    {
        if (FMLLoader.getDist().isClient() && allowdedIn(pCategory) && Minecraft.getInstance().level != null)
        {
            var reg = Minecraft.getInstance().level.registryAccess();
            for (DragonBreed breed : BreedRegistry.registry(reg))
                pItems.add(create(breed, reg));
        }
    }

    public static ItemStack create(DragonBreed breed, RegistryAccess reg)
    {
        var id = breed.id(reg);
        var root = new CompoundTag();

        // entity tag
        var entityTag = new CompoundTag();
        entityTag.putString(TameableDragon.NBT_BREED, id.toString());
        root.put(EntityType.ENTITY_TAG, entityTag);

        // name & colors
        // storing these in the stack nbt is more performant than getting the breed everytime
        var itemDataTag = new CompoundTag();
        itemDataTag.putString(DATA_ITEM_NAME, String.join(".", DMLRegistry.SPAWN_EGG.get().getDescriptionId(), id.getNamespace(), id.getPath()));
        itemDataTag.putInt(DATA_PRIM_COLOR, breed.primaryColor());
        itemDataTag.putInt(DATA_SEC_COLOR, breed.secondaryColor());
        root.put(DATA_TAG, itemDataTag);

        ItemStack stack = new ItemStack(DMLRegistry.SPAWN_EGG.get());
        stack.setTag(root);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack)
    {
        var tag = stack.getTagElement(DATA_TAG);
        return tag == null || !tag.contains(DATA_ITEM_NAME) ? super.getName(stack) : new TranslatableComponent(tag.getString(DATA_ITEM_NAME));

    }

    public static int getColor(ItemStack stack, int tintIndex)
    {
        int prim;
        int sec;
        var tag = stack.getTagElement(DATA_TAG);
        if (tag != null)
        {
            prim = tag.getInt(DATA_PRIM_COLOR);
            sec = tag.getInt(DATA_SEC_COLOR);
        }
        else
        {
            var fire = BreedRegistry.getFallback(Minecraft.getInstance().level.registryAccess());
            prim = fire.primaryColor();
            sec = fire.secondaryColor();
        }

        return tintIndex == 0? prim : sec;
    }
}
