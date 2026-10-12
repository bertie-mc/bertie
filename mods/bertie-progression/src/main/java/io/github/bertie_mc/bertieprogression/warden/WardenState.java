package io.github.bertie_mc.bertieprogression.warden;

public interface WardenState {
    boolean bertie$tendrilsSheared();

    void bertie$setTendrilsSheared(boolean value);

    long bertie$retaliationReadyAt();

    void bertie$setRetaliationReadyAt(long tick);
}
