package com.mamikos.kostapi.user.entity;

/** Fixed at registration; it decides both the credit quota and the API surface. */
public enum UserRole {
    OWNER,
    REGULAR,
    PREMIUM;

    /** Owners advertise kosts and therefore never hold a credit wallet. */
    public boolean hasCreditWallet() {
        return this != OWNER;
    }

    public String authority() {
        return "ROLE_" + name();
    }
}
