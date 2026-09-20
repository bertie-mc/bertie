package io.github.bertie_mc.creatures.server.entity.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

public class ACBossEvent extends ServerBossEvent {

    private final int renderType;

    public ACBossEvent(Component component, int renderType) {
        super(component, BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
        this.renderType = renderType;
    }

    public int getRenderType() {
        return renderType;
    }

    public void addPlayer(ServerPlayer serverPlayer) {
        super.addPlayer(serverPlayer);
    }

    public void removePlayer(ServerPlayer serverPlayer) {
        super.removePlayer(serverPlayer);
    }
}
