package me.ferreira.graveto.portfolio.assets.client;

import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.portfolio.assets.client.impl.dto.response.SearchAssetResponseDto;

public interface MarketDataClient {

  List<SearchAssetResponseDto.Result> searchAsset(UUID userSid, String keyword);

}
