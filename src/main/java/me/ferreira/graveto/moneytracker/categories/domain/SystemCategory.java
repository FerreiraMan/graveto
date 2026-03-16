package me.ferreira.graveto.moneytracker.categories.domain;

import java.util.UUID;

public class SystemCategory {

    private SystemCategory() {}

    //  CORE SYSTEM CATEGORIES
    public static final UUID INITIAL_BALANCE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID TRANSFER_IN = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID TRANSFER_OUT = UUID.fromString("00000000-0000-0000-0000-000000000003");

}
