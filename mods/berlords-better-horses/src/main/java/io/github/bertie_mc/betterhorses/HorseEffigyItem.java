package io.github.bertie_mc.betterhorses;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

public final class HorseEffigyItem extends Item {
    public static final String HORSE_KEY = "betterhorses:horse";

    public HorseEffigyItem(Properties properties) {
        super(properties);
    }

    public static boolean occupied(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).contains(HORSE_KEY);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return occupied(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Horse horse)) return InteractionResult.PASS;
        if (occupied(stack) || !horse.isTamed() || !horse.isAlive() || horse.isVehicle() || horse.isPassenger())
            return InteractionResult.FAIL;
        if (!player.level().isClientSide) {
            CompoundTag data = new CompoundTag();
            if (!horse.save(data)) return InteractionResult.FAIL;
            // Return the lead on capture; never restore a stale holder or fence link on release.
            data.remove("leash");
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(HORSE_KEY, data));
            horse.dropLeash(true, true);
            horse.discard();
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (!occupied(stack)) return InteractionResult.PASS;
        if (context.getClickedFace() != net.minecraft.core.Direction.UP) return InteractionResult.FAIL;
        Player player = context.getPlayer();
        if (player == null || !player.mayUseItemAt(context.getClickedPos().above(), context.getClickedFace(), stack))
            return InteractionResult.FAIL;
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        Vec3 hit = context.getClickLocation();
        return release(stack, level, new Vec3(hit.x, hit.y + 0.01, hit.z), context.getRotation())
                ? InteractionResult.CONSUME
                : InteractionResult.FAIL;
    }

    public static boolean release(ItemStack stack, ServerLevel level, Vec3 position, float rotation) {
        if (!occupied(stack)) return false;
        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getCompound(HORSE_KEY)
                .copy();
        if (!data.getString("id").equals("minecraft:horse") || !data.hasUUID("UUID")) return false;
        for (ServerLevel dimension : level.getServer().getAllLevels())
            if (dimension.getEntity(data.getUUID("UUID")) != null) return false;
        Horse horse = EntityType.HORSE.create(level);
        if (horse == null) return false;
        horse.load(data);
        if (!horse.isAlive() || !horse.isTamed()) return false;
        horse.moveTo(position.x, position.y, position.z, rotation, 0);
        horse.setDeltaMovement(Vec3.ZERO);
        horse.fallDistance = 0;
        if (!level.getWorldBorder().isWithinBounds(horse.getBoundingBox())
                || !level.noCollision(horse, horse.getBoundingBox())) return false;
        if (!level.addFreshEntity(horse)) return false;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(HORSE_KEY));
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(
                occupied(stack) ? "tooltip.betterhorses.effigy_full" : "tooltip.betterhorses.effigy_empty"));
        if (occupied(stack)) lines.add(Component.translatable("tooltip.betterhorses.effigy_release"));
    }
}
