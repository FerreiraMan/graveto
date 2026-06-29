package me.ferreira.graveto.portfolio.assets.domain;

import static org.assertj.core.api.Assertions.assertThat;

import me.ferreira.graveto.common.domain.Currency;
import org.junit.jupiter.api.Test;

public class AssetTest {

  @Test
  void shouldCreateAssetWithGeneratedSid() {
    // Act
    final Asset asset = Asset.create("IWDA", "iShares Core MSCI World", AssetType.ETF, Currency.EUR);

    // Assert
    assertThat(asset.getSid()).isNotNull();
    assertThat(asset.getTicker()).isEqualTo("IWDA");
    assertThat(asset.getName()).isEqualTo("iShares Core MSCI World");
    assertThat(asset.getAssetType()).isEqualTo(AssetType.ETF);
    assertThat(asset.getCurrency()).isEqualTo(Currency.EUR);
    assertThat(asset.getStockExchange()).isNull();
    assertThat(asset.getIsin()).isNull();
    assertThat(asset.getCurrentPrice()).isNull();
  }

}
