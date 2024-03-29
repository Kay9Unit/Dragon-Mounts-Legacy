package com.github.kay9.dragonmounts.abilities;

import com.github.kay9.dragonmounts.dragon.TameableDragon;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.ForgeEventFactory;

public abstract class FootprintAbility implements Ability
{
    @Override
    public void onMove(TameableDragon dragon)
    {
        if (!dragon.isAdult() || !dragon.isOnGround()) return;
        if (!ForgeEventFactory.getMobGriefingEvent(dragon.getLevel(), dragon)) return;

        var chance = getFootprintChance(dragon);
        if (chance == 0) return;

        for (int i = 0; i < 4; i++)
        {
            // place only if randomly selected
            if (dragon.getRandom().nextFloat() > chance) continue;

            double bx = dragon.getX() + (i % 2 * 2 - 1) * dragon.getScale();
            double by = dragon.getY();
            double bz = dragon.getZ() + (i / 2f % 2 * 2 - 1) * dragon.getScale();
            var pos = new BlockPos(bx, by, bz);

            placeFootprint(dragon, pos);
        }
    }

    protected float getFootprintChance(TameableDragon dragon)
    {
        return 0.05f;
    }

    protected abstract void placeFootprint(TameableDragon dragon, BlockPos pos);
}
