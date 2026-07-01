package me.ferreira.graveto.portfolio.orders.domain;

public enum OrderType {

  BUY(1),
  SELL(-1);

  private final int quantityMultiplier;

  OrderType(final int quantityMultiplier) {
    this.quantityMultiplier = quantityMultiplier;
  }

  public int getQuantityMultiplier() {
    return this.quantityMultiplier;
  }

  public boolean isBuyOrder() {
    return this.equals(BUY);
  }

}
