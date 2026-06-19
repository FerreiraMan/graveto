package me.ferreira.graveto.portfolio.assets.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.assets.service.AssetService;
import me.ferreira.graveto.portfolio.assets.service.command.FetchAssetCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class AssetServiceImpl implements AssetService {

  @Override
  @Transactional
  public Asset fetchAsset(FetchAssetCommand command) {
    return null;
  }

}
