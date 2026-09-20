package io.github.bertie_mc.creatures.server.entity.util;

public interface UnderzealotSacrifice {

    void triggerSacrificeIn(int time);

    boolean isValidSacrifice(int distanceFromGround);
}
