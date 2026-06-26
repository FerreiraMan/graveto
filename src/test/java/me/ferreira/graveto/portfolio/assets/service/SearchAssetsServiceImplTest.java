package me.ferreira.graveto.portfolio.assets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.portfolio.assets.client.MarketDataClient;
import me.ferreira.graveto.portfolio.assets.client.impl.dto.response.SearchAssetResponseDto;
import me.ferreira.graveto.portfolio.assets.service.command.SearchAssetCommand;
import me.ferreira.graveto.portfolio.assets.service.impl.AssetServiceImpl;
import me.ferreira.graveto.portfolio.assets.service.payload.AssetSearchRecommendation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SearchAssetsServiceImplTest {

  @InjectMocks
  private AssetServiceImpl assetService;
  @Mock
  private MarketDataClient marketDataClient;

  @Test
  void shouldReturnMappedRecommendations() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final String keyword = "IWDA";
    final SearchAssetCommand command = new SearchAssetCommand(userSid, keyword);

    final List<SearchAssetResponseDto.Result> clientResults = List.of(
        new SearchAssetResponseDto.Result("IWDA.AS", "iShares Core MSCI World", "Amsterdam", "ETF"),
        new SearchAssetResponseDto.Result("IWDA.L", "iShares Core MSCI World", "London", "ETF")
    );

    when(marketDataClient.searchAsset(userSid, keyword)).thenReturn(clientResults);

    // Act
    final List<AssetSearchRecommendation> result = assetService.searchAsset(command);

    // Assert
    assertThat(result).hasSize(2);
    assertThat(result.getFirst().ticker()).isEqualTo("IWDA.AS");
    assertThat(result.getFirst().name()).isEqualTo("iShares Core MSCI World");
    assertThat(result.getFirst().exchange()).isEqualTo("Amsterdam");
    assertThat(result.getFirst().type()).isEqualTo("ETF");
    assertThat(result.get(1).ticker()).isEqualTo("IWDA.L");
  }

  @Test
  void shouldReturnEmptyListWhenClientReturnsNoResults() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final SearchAssetCommand command = new SearchAssetCommand(userSid, "NONEXISTENT");

    when(marketDataClient.searchAsset(userSid, "NONEXISTENT")).thenReturn(List.of());

    // Act
    final List<AssetSearchRecommendation> result = assetService.searchAsset(command);

    // Assert
    assertThat(result).isEmpty();
  }

}
