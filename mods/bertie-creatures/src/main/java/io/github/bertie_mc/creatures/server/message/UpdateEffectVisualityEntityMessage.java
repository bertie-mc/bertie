package io.github.bertie_mc.creatures.server.message;

import io.github.bertie_mc.creatures.BertieCreatures;
import io.github.bertie_mc.creatures.server.misc.ACSoundRegistry;
import io.github.bertie_mc.creatures.server.potion.ACEffectRegistry;
import io.github.bertie_mc.creatures.server.potion.IrradiatedEffect;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateEffectVisualityEntityMessage(
        int entityID, int fromEntityID, int potionType, int duration, boolean remove) implements CustomPacketPayload {

    public UpdateEffectVisualityEntityMessage(int entityID, int fromEntityID, int potionType, int duration) {
        this(entityID, fromEntityID, potionType, duration, false);
    }

    public static final CustomPacketPayload.Type<UpdateEffectVisualityEntityMessage> ID =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(BertieCreatures.MODID, "update_effect_visuality"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateEffectVisualityEntityMessage> CODEC =
            new StreamCodec<>() {
                @Override
                public UpdateEffectVisualityEntityMessage decode(RegistryFriendlyByteBuf buf) {
                    return new UpdateEffectVisualityEntityMessage(
                            buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, UpdateEffectVisualityEntityMessage packet) {
                    buf.writeInt(packet.entityID);
                    buf.writeInt(packet.fromEntityID);
                    buf.writeInt(packet.potionType);
                    buf.writeInt(packet.duration);
                    buf.writeBoolean(packet.remove);
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(UpdateEffectVisualityEntityMessage message, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player playerSided = context.player();
            if (playerSided != null) {
                Entity entity = playerSided.level().getEntity(message.entityID);
                Entity senderEntity = playerSided.level().getEntity(message.fromEntityID);
                if (entity instanceof LivingEntity living
                        && senderEntity != null
                        && senderEntity.distanceTo(living) < 32) {
                    Holder<MobEffect> mobEffect = null;
                    int level = 0;
                    switch (message.potionType) {
                        case 0:
                            mobEffect = ACEffectRegistry.IRRADIATED;
                            break;
                        case 1:
                            mobEffect = ACEffectRegistry.BUBBLED;
                            entity.playSound(ACSoundRegistry.SEA_STAFF_BUBBLE.get());
                            break;
                        case 3:
                            mobEffect = ACEffectRegistry.STUNNED;
                            break;
                        case 4:
                            mobEffect = ACEffectRegistry.IRRADIATED;
                            level = IrradiatedEffect.BLUE_LEVEL;
                            break;
                    }
                    if (mobEffect != null) {
                        if (message.remove) {
                            living.removeEffect(mobEffect);
                        } else {
                            living.addEffect(new MobEffectInstance(mobEffect, message.duration, level));
                        }
                    }
                }
            }
        });
    }
}
