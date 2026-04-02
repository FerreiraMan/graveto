package me.ferreira.graveto.moneytracker.accounts.domain;

public enum MembershipRole {

    OWNER(true),
    CONTRIBUTOR(true);

    private final boolean canCreateTransaction;

    MembershipRole(final boolean canCreateTransaction) {
        this.canCreateTransaction = canCreateTransaction;
    }

    public boolean canCreateTransaction() {
        return this.canCreateTransaction;
    }

}
