package com.github.kay9.dragonmounts.client;

import com.github.kay9.dragonmounts.dragon.FallingEgg;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;

public class FallingEggEntityRenderer extends EntityRenderer<FallingEgg>
{
    public FallingEggEntityRenderer(EntityRendererProvider.Context ctx)
    {
        super(ctx);
    }

    @Override
    public void render(FallingEgg entity, float yaw, float partialTicks, @NotNull PoseStack ps, @NotNull MultiBufferSource buffer, int light)
    {
        DragonEggRenderer.renderEgg(ps, buffer.getBuffer(Sheets.translucentCullBlockSheet()), light, new ResourceLocation(entity.getBreed()), true);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull FallingEgg entity)
    {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
