package me.ferreira.graveto.moneytracker.accounts.domain;

public enum MembershipRole {

    OWNER(true, true),
    CONTRIBUTOR(true, true);

    private final boolean canCreateTransaction;
    private final boolean canDeleteTransaction;

    MembershipRole(final boolean canCreateTransaction, final boolean canDeleteTransaction) {
        this.canCreateTransaction = canCreateTransaction;
        this.canDeleteTransaction = canDeleteTransaction;
    }

    public boolean canCreateTransaction() {
        return this.canCreateTransaction;
    }

    public boolean canDeleteTransaction() {
        return this.canDeleteTransaction;
    }

}
