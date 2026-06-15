package com.bbd.procurement.purchaseorder.domain;

public enum SourcingType {
    BUY,
    MAKE;

    public static SourcingType from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return SourcingType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
