package me.ferreira.graveto.portfolio.assets.service;

import java.util.List;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.assets.scheduler.payload.AssetPriceUpdateResult;
import me.ferreira.graveto.portfolio.assets.service.command.CreateAssetCommand;
import me.ferreira.graveto.portfolio.assets.service.command.FetchAssetCommand;
import me.ferreira.graveto.portfolio.assets.service.command.SearchAssetCommand;
import me.ferreira.graveto.portfolio.assets.service.payload.AssetSearchRecommendation;

public interface AssetService {

  List<AssetSearchRecommendation> searchAsset(SearchAssetCommand command);

  Asset createAsset(CreateAssetCommand command);

  Asset fetchAsset(FetchAssetCommand command);

  List<AssetPriceUpdateResult> updateAssetPrices();

}
