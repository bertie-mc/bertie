package io.github.bertie_mc.creatures.server;

import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class CommonProxy {
    public Player getClientSidePlayer() {
        return null;
    }

    public float getPartialTicks() {
        return 0;
    }

    public boolean isKeyDown(int key) {
        return false;
    }

    public boolean isFirstPersonPlayer(Entity entity) {
        return false;
    }

    public void playWorldSound(Object emitter, byte type) {}

    public void clearSoundCacheFor(Entity entity) {}

    public void blockRenderingEntity(UUID id) {}

    public void releaseRenderingEntity(UUID id) {}
}
