package com.berlord.filters;

/** Dependency-free matching policy used by the Slag component adapter. */
final class WoodenFilterPolicy {

    private static final String EVENT_ID = "bertie:wooden";
    private static final String SLAG_NAMESPACE = "slag:";
    private static final String WOODEN_MATERIAL = "slag:wooden";

    private WoodenFilterPolicy() {
    }

    static boolean handles(String eventId) {
        return EVENT_ID.equals(eventId);
    }

    static boolean matches(String requestedType, String modularType, Iterable<String> partMaterials) {
        if (requestedType == null || !modularType.equals(SLAG_NAMESPACE + requestedType)) {
            return false;
        }

        for (String material : partMaterials) {
            if (WOODEN_MATERIAL.equals(material)) {
                return true;
            }
        }
        return false;
    }
}
