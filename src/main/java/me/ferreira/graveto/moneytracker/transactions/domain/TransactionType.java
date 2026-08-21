package me.ferreira.graveto.moneytracker.transactions.domain;

public enum TransactionType {

  INCOME(1, true),
  EXPENSE(-1, true),
  TRANSFER_IN(1, true),
  TRANSFER_OUT(-1, true),
  OPENING_BALANCE(1, false);

  private final int balanceMultiplier;
  private final boolean isValidCashFlow;

  TransactionType(final int balanceMultiplier, final boolean isValidCashFlow) {
    this.balanceMultiplier = balanceMultiplier;
    this.isValidCashFlow = isValidCashFlow;
  }

  public int getBalanceMultiplier() {
    return this.balanceMultiplier;
  }

  public boolean isValidCashFlow() {
    return this.isValidCashFlow;
  }

}
