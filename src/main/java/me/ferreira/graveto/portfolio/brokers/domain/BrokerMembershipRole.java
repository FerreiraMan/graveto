package me.ferreira.graveto.portfolio.brokers.domain;

public enum BrokerMembershipRole {
  OWNER(true, true),
  VIEWER(false, false);

  private final boolean canCreateOrders;
  private final boolean canUpdateOrders;

  BrokerMembershipRole(final boolean canCreateOrders, final boolean canUpdateOrders) {
    this.canCreateOrders = canCreateOrders;
    this.canUpdateOrders = canUpdateOrders;
  }

  public boolean canCreateOrders() {
    return this.canCreateOrders;
  }

  public boolean canUpdateOrders() {
    return this.canUpdateOrders;
  }

}
