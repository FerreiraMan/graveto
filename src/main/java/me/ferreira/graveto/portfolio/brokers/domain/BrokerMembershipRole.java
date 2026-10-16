package me.ferreira.graveto.portfolio.brokers.domain;

public enum BrokerMembershipRole {
  OWNER(true, true, true, true, true),
  VIEWER(false, false, false,  true, false);

  private final boolean canCreateOrders;
  private final boolean canUpdateOrders;
  private final boolean canAddMemberToBroker;

  private final boolean canRequestValuationOverview;
  private final boolean canRequestPortfolioOverview;

  BrokerMembershipRole(final boolean canCreateOrders, final boolean canUpdateOrders, final boolean canAddMemberToBroker,
                       final boolean canRequestValuationOverview, final boolean canRequestPortfolioOverview) {
    this.canCreateOrders = canCreateOrders;
    this.canUpdateOrders = canUpdateOrders;
    this.canAddMemberToBroker = canAddMemberToBroker;
    this.canRequestValuationOverview = canRequestValuationOverview;
    this.canRequestPortfolioOverview = canRequestPortfolioOverview;
  }

  public boolean canCreateOrders() {
    return this.canCreateOrders;
  }

  public boolean canUpdateOrders() {
    return this.canUpdateOrders;
  }

  public boolean canAddMemberToBroker() {
    return this.canAddMemberToBroker;
  }

  public boolean canRequestValuationOverview() {
    return this.canRequestValuationOverview;
  }

  public boolean canRequestPortfolioOverview() {
    return this.canRequestPortfolioOverview;
  }

}
