package me.ferreira.graveto.moneytracker.accounts.domain;

public enum MembershipRole {

    OWNER(true, true, true, true),
    CONTRIBUTOR(true, true, true, true);

    private final boolean canReadTransaction;
    private final boolean canCreateTransaction;
    private final boolean canDeleteTransaction;
    private final boolean canUpdateTransaction;

    MembershipRole(final boolean canReadTransaction,
                   final boolean canCreateTransaction,
                   final boolean canDeleteTransaction,
                   final boolean canUpdateTransaction) {

        this.canReadTransaction = canReadTransaction;
        this.canCreateTransaction = canCreateTransaction;
        this.canDeleteTransaction = canDeleteTransaction;
        this.canUpdateTransaction = canUpdateTransaction;
    }

    public boolean canReadTransaction() {
        return this.canReadTransaction;
    }

    public boolean canCreateTransaction() {
        return this.canCreateTransaction;
    }

    public boolean canDeleteTransaction() {
        return this.canDeleteTransaction;
    }

    public boolean canUpdateTransaction() {
        return this.canUpdateTransaction;
    }

}
