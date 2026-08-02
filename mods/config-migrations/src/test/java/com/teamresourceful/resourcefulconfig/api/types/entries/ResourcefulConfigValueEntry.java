package com.teamresourceful.resourcefulconfig.api.types.entries;

public interface ResourcefulConfigValueEntry extends ResourcefulConfigEntry {
    Class<?> objectType();

    boolean isArray();

    Object get();

    boolean setArray(Object[] value);

    boolean setByte(byte value);

    boolean setShort(short value);

    boolean setInt(int value);

    boolean setLong(long value);

    boolean setFloat(float value);

    boolean setDouble(double value);

    boolean setBoolean(boolean value);

    boolean setString(String value);

    boolean setEnum(Enum<?> value);
}
