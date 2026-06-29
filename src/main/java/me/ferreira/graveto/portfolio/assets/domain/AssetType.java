package me.ferreira.graveto.portfolio.assets.domain;

public enum AssetType {

  ETF("E"),
  CRYPTOCURRENCY("C"),
  EQUITY("S"),
  FUTURES("F"),
  FUND("M"),
  INDEX("I");

  private final String typeCorrespondency;

  AssetType(final String typeCorrespondency) {
    this.typeCorrespondency = typeCorrespondency;
  }

}