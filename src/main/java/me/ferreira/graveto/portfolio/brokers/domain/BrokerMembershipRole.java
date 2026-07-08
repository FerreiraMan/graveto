package me.ferreira.graveto.portfolio.brokers.domain;

public enum BrokerMembershipRole {
  OWNER(true, true, true),
  VIEWER(false, false, true);

  private final boolean canCreateOrders;
  private final boolean canUpdateOrders;
  private final boolean canRequestValuationOverview;

  BrokerMembershipRole(final boolean canCreateOrders, final boolean canUpdateOrders,
                       final boolean canRequestValuationOverview) {
    this.canCreateOrders = canCreateOrders;
    this.canUpdateOrders = canUpdateOrders;
    this.canRequestValuationOverview = canRequestValuationOverview;
  }

  public boolean canCreateOrders() {
    return this.canCreateOrders;
  }

  public boolean canUpdateOrders() {
    return this.canUpdateOrders;
  }

  public boolean canRequestValuationOverview() {
    return this.canRequestValuationOverview;
  }

}
