package me.ferreira.graveto.portfolio.assets.scheduler;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.ferreira.graveto.portfolio.assets.scheduler.payload.AssetPriceUpdateResult;
import me.ferreira.graveto.portfolio.assets.service.AssetService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@AllArgsConstructor
@Component
public class AssetPriceUpdateScheduler {

  private final AssetService assetService;

  @Scheduled(cron = "${scheduled.cron.price}")
  public void dailyAssetPriceUpdate() {

    final List<AssetPriceUpdateResult> results = assetService.updateAssetPrices();

    final List<String> failedSymbols = results.stream()
        .filter(result -> !result.updated()).map(AssetPriceUpdateResult::symbol)
        .toList();
    failedSymbols.forEach(s -> log.warn("Asset with symbol [{}] was not updated.", s));
  }

}
