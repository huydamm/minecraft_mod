package com.huybao.firstmod.data;

import com.mojang.serialization.Codec;

// The four pickable classes. NONE means the selection screen hasn't been shown/answered yet.
public enum PlayerClass {
    NONE,
    KNIGHT,
    ARCHER,
    GIANT,
    VAGABOND; // the "No Class" option

    public static PlayerClass byId(int id) {
        PlayerClass[] values = values();
        return (id >= 0 && id < values.length) ? values[id] : NONE;
    }

    public static PlayerClass byName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return NONE; // unknown value in an old save -> treat as "not chosen yet"
        }
    }

    // Saved as the enum name; missing/unknown decodes to NONE so old saves don't crash.
    public static final Codec<PlayerClass> CODEC =
            Codec.STRING.xmap(PlayerClass::byName, PlayerClass::name);
}
