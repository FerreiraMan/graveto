package me.ferreira.graveto.portfolio.assets.web;

import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.portfolio.assets.service.AssetService;
import me.ferreira.graveto.portfolio.assets.service.command.SearchAssetCommand;
import me.ferreira.graveto.portfolio.assets.service.payload.AssetSearchRecommendation;
import me.ferreira.graveto.portfolio.assets.web.dto.response.AssetRecommendationResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/assets")
@RequiredArgsConstructor
@Validated
public class AssetController {

  private static final String ASSET_SID_PATH = "/{sid}";
  private static final String SEARCH_PATH = "/search";

  private final AssetService assetService;

  @GetMapping(value = SEARCH_PATH, produces = "application/json")
  public ResponseEntity<List<AssetRecommendationResponseDto>> searchAsset(
      @AuthenticationPrincipal final UUID userSid,
      @RequestParam @Size(min = 2, max = 20, message = "Keyword must have 2 to 20 characters.") final String keyword) {

    final SearchAssetCommand command = new SearchAssetCommand(userSid, keyword);

    final List<AssetSearchRecommendation> searchRecommendations = assetService.searchAsset(command);

    return ResponseEntity.ok(
        searchRecommendations.stream()
        .map(rec ->
            new AssetRecommendationResponseDto(rec.ticker(), rec.name(), rec.type(), rec.exchange()))
        .toList()
    );
  }

}
