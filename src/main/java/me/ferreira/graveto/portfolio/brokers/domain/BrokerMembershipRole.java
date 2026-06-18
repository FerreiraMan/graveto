package me.ferreira.graveto.portfolio.brokers.domain;

public enum BrokerMembershipRole {
  OWNER(true),
  VIEWER(false);

  private final boolean canCreateOrders;

  BrokerMembershipRole(final boolean canCreateOrders) {
    this.canCreateOrders = canCreateOrders;
  }

  public boolean canCreateOrders() {
    return this.canCreateOrders;
  }

}
