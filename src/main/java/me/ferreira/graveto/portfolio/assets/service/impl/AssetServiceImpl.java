package me.ferreira.graveto.portfolio.assets.service.impl;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.ferreira.graveto.portfolio.assets.client.MarketDataClient;
import me.ferreira.graveto.portfolio.assets.client.impl.dto.response.SearchAssetResponseDto;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.assets.service.AssetService;
import me.ferreira.graveto.portfolio.assets.service.command.FetchAssetCommand;
import me.ferreira.graveto.portfolio.assets.service.command.SearchAssetCommand;
import me.ferreira.graveto.portfolio.assets.service.payload.AssetSearchRecommendation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class AssetServiceImpl implements AssetService {

  private final MarketDataClient marketDataClient;

  @Override
  public List<AssetSearchRecommendation> searchAsset(final SearchAssetCommand command) {

    final List<SearchAssetResponseDto.Result> assetSearchResponse =
        marketDataClient.searchAsset(command.userSid(), command.keyword());

    return assetSearchResponse.stream()
        .map(result -> new AssetSearchRecommendation(result.symbol(), result.name(), result.type(), result.exchange()))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Asset fetchAsset(final FetchAssetCommand command) {
    return null;
  }

}
