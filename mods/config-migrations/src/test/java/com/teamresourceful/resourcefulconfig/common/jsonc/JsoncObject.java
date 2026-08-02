package com.teamresourceful.resourcefulconfig.common.jsonc;

public final class JsoncObject {
    private final String value;

    public JsoncObject(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
