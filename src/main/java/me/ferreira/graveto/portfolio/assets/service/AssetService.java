package me.ferreira.graveto.portfolio.assets.service;

import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.assets.service.command.FetchAssetCommand;

public interface AssetService {

  Asset fetchAsset(FetchAssetCommand command);

}
