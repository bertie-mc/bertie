package io.github.bertie_mc.betterhorses.mixin;

import io.github.bertie_mc.betterhorses.*;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(AbstractHorse.class)
public abstract class HorseEquipmentMixin extends Animal implements HorseEquipment {
    @Shadow
    protected SimpleContainer inventory;

    @Unique
    private static final EntityDataAccessor<ItemStack> betterhorses$SHOES =
            SynchedEntityData.defineId(AbstractHorse.class, EntityDataSerializers.ITEM_STACK);

    @Unique
    private static final EntityDataAccessor<ItemStack> betterhorses$SADDLE =
            SynchedEntityData.defineId(AbstractHorse.class, EntityDataSerializers.ITEM_STACK);

    @Unique
    private SimpleContainer betterhorses$shoeInventory;

    @Unique
    private SimpleContainer betterhorses$storageInventory;

    protected HorseEquipmentMixin(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void betterhorses$define(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(betterhorses$SHOES, ItemStack.EMPTY);
        builder.define(betterhorses$SADDLE, ItemStack.EMPTY);
    }

    @Override
    public SimpleContainer betterhorses$shoes() {
        if (betterhorses$shoeInventory == null) {
            betterhorses$shoeInventory = new SimpleContainer(1);
            betterhorses$shoeInventory.addListener(container -> {
                if (!level().isClientSide) {
                    entityData.set(betterhorses$SHOES, container.getItem(0).copy());
                    betterhorses$applyAttributes();
                }
            });
        }
        return betterhorses$shoeInventory;
    }

    @Override
    public SimpleContainer betterhorses$saddleInventory() {
        return inventory;
    }

    @Override
    public SimpleContainer betterhorses$storage() {
        if (betterhorses$storageInventory == null) betterhorses$storageInventory = new SimpleContainer(15);
        return betterhorses$storageInventory;
    }

    @Override
    public ItemStack betterhorses$syncedShoes() {
        return entityData.get(betterhorses$SHOES);
    }

    @Override
    public ItemStack betterhorses$syncedSaddle() {
        return entityData.get(betterhorses$SADDLE);
    }

    @Unique
    private void betterhorses$applyAttributes() {
        ShoeTier tier = betterhorses$tier();
        betterhorses$modifier(
                Attributes.MOVEMENT_SPEED, "shoe_speed", tier.speed / HorsePhysics.BLOCKS_PER_SECOND_PER_ATTRIBUTE);
        betterhorses$modifier(Attributes.SAFE_FALL_DISTANCE, "shoe_fall", tier.safeFall);
        betterhorses$modifier(Attributes.STEP_HEIGHT, "shoe_step", tier.stepHeight);
    }

    @Unique
    private void betterhorses$modifier(Holder<Attribute> attribute, String name, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(BetterHorses.id(name));
        if (value != 0)
            instance.addTransientModifier(
                    new AttributeModifier(BetterHorses.id(name), value, AttributeModifier.Operation.ADD_VALUE));
    }

    @Inject(method = "syncSaddleToClients", at = @At("TAIL"))
    private void betterhorses$syncSaddle(CallbackInfo ci) {
        if (!level().isClientSide)
            entityData.set(betterhorses$SADDLE, inventory.getItem(0).copy());
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void betterhorses$save(CompoundTag tag, CallbackInfo ci) {
        if (!betterhorses$shoes().isEmpty())
            tag.put("BetterHorsesShoes", betterhorses$shoes().getItem(0).save(registryAccess()));
        if (!betterhorses$storage().isEmpty()) {
            CompoundTag storage = new CompoundTag();
            ContainerHelper.saveAllItems(storage, betterhorses$storage().getItems(), registryAccess());
            tag.put("BetterHorsesStorage", storage);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void betterhorses$load(CompoundTag tag, CallbackInfo ci) {
        ItemStack shoes = ItemStack.parseOptional(registryAccess(), tag.getCompound("BetterHorsesShoes"));
        betterhorses$shoes()
                .setItem(0, shoes.getItem() instanceof HorseshoeItem ? shoes.copyWithCount(1) : ItemStack.EMPTY);
        ItemStack saddle = ItemStack.parseOptional(registryAccess(), tag.getCompound("SaddleItem"));
        if (saddle.getItem() instanceof SpecialSaddleItem && (Object) this instanceof Horse)
            inventory.setItem(0, saddle.copyWithCount(1));
        betterhorses$storage().clearContent();
        ContainerHelper.loadAllItems(
                tag.getCompound("BetterHorsesStorage"), betterhorses$storage().getItems(), registryAccess());
    }

    @Inject(method = "dropEquipment", at = @At("TAIL"))
    private void betterhorses$drop(CallbackInfo ci) {
        ItemStack shoes = betterhorses$shoes().removeItemNoUpdate(0);
        if (!shoes.isEmpty()) spawnAtLocation(shoes);
        for (int i = 0; i < betterhorses$storage().getContainerSize(); i++) {
            ItemStack stored = betterhorses$storage().removeItemNoUpdate(i);
            if (!stored.isEmpty()) spawnAtLocation(stored);
        }
    }

    @Inject(method = "openCustomInventoryScreen", at = @At("HEAD"), cancellable = true)
    private void betterhorses$open(Player player, CallbackInfo ci) {
        if (!((Object) this instanceof Horse horse)) return;
        ci.cancel();
        if (player instanceof ServerPlayer serverPlayer && horse.isTamed() && (!isVehicle() || hasPassenger(player)))
            serverPlayer.openMenu(
                    new SimpleMenuProvider((id, inv, who) -> new BetterHorseMenu(id, inv, horse), getDisplayName()),
                    buffer -> buffer.writeVarInt(getId()));
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        if ((Object) this instanceof Horse && betterhorses$syncedSaddle().is(BetterHorses.PASSENGER.get()))
            return passenger instanceof Player && getPassengers().size() < 2 && !isBaby();
        return super.canAddPassenger(passenger);
    }

    @Inject(method = "getPassengerAttachmentPoint", at = @At("RETURN"), cancellable = true)
    private void betterhorses$seat(
            Entity passenger, EntityDimensions dimensions, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        if ((Object) this instanceof Horse && betterhorses$syncedSaddle().is(BetterHorses.PASSENGER.get())) {
            float offset = getPassengers().indexOf(passenger) == 0 ? -0.35F : 0.45F;
            cir.setReturnValue(
                    cir.getReturnValue().add(new Vec3(0, 0, offset).yRot(-getYRot() * (float) Math.PI / 180)));
        }
    }

    @Override
    public boolean canStandOnFluid(FluidState fluid) {
        return ((Object) this instanceof Horse
                        && ((betterhorses$tier().waterWalking() && fluid.is(FluidTags.WATER))
                                || (betterhorses$tier().lavaWalking() && fluid.is(FluidTags.LAVA))))
                || super.canStandOnFluid(fluid);
    }

    @ModifyVariable(method = "executeRidersJump", at = @At("STORE"), ordinal = 0)
    private double betterhorses$jump(double original) {
        return (Object) this instanceof Horse
                ? HorsePhysics.boostedJump(original, betterhorses$tier().jumpHeight, getGravity())
                : original;
    }

    @ModifyVariable(method = "onPlayerJump", at = @At("HEAD"), argsOnly = true)
    private int betterhorses$perfectJump(int charge) {
        return (Object) this instanceof Horse && betterhorses$traveller() ? 100 : charge;
    }
}
