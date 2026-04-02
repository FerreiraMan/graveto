package me.ferreira.graveto.moneytracker.transactions.domain;

public enum TransactionType {
    INCOME(1),
    EXPENSE(-1),
    TRANSFER_IN(1),
    TRANSFER_OUT(-1),
    OPENING_BALANCE(1);

    private final int balanceMultiplier;

    TransactionType(final int balanceMultiplier) {
        this.balanceMultiplier = balanceMultiplier;
    }

    public int getBalanceMultiplier() {
        return this.balanceMultiplier;
    }

}
