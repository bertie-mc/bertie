package io.github.bertie_mc.betterhorses;

public interface HorseAppearance {
    enum Style {
        NORMAL,
        ZOMBIE,
        SKELETON;

        public static Style byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : NORMAL;
        }
    }

    Style betterhorses$appearance();

    void betterhorses$setAppearance(Style appearance);
}
