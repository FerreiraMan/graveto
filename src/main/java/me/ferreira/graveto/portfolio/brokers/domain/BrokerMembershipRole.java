package me.ferreira.graveto.portfolio.brokers.domain;

public enum BrokerMembershipRole {
  OWNER(true, true, true, true),
  VIEWER(false, false, true, false);

  private final boolean canCreateOrders;
  private final boolean canUpdateOrders;
  private final boolean canRequestValuationOverview;
  private final boolean canRequestPortfolioOverview;

  BrokerMembershipRole(final boolean canCreateOrders, final boolean canUpdateOrders,
                       final boolean canRequestValuationOverview, final boolean canRequestPortfolioOverview) {
    this.canCreateOrders = canCreateOrders;
    this.canUpdateOrders = canUpdateOrders;
    this.canRequestValuationOverview = canRequestValuationOverview;
    this.canRequestPortfolioOverview = canRequestPortfolioOverview;
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

  public boolean canRequestPortfolioOverview() {
    return this.canRequestPortfolioOverview;
  }

}
