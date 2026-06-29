package me.ferreira.graveto.portfolio.assets.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.portfolio.assets.domain.Asset;
import me.ferreira.graveto.portfolio.assets.service.AssetService;
import me.ferreira.graveto.portfolio.assets.service.command.CreateAssetCommand;
import me.ferreira.graveto.portfolio.assets.service.command.SearchAssetCommand;
import me.ferreira.graveto.portfolio.assets.service.payload.AssetSearchRecommendation;
import me.ferreira.graveto.portfolio.assets.web.dto.request.CreateAssetRequestDto;
import me.ferreira.graveto.portfolio.assets.web.dto.response.AssetRecommendationResponseDto;
import me.ferreira.graveto.portfolio.assets.web.dto.response.AssetResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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

  @PostMapping(produces = "application/json")
  public ResponseEntity<AssetResponseDto> createAsset(
      @AuthenticationPrincipal final UUID userSid,
      @Valid @RequestBody final CreateAssetRequestDto requestDto) {

    final CreateAssetCommand command = new CreateAssetCommand(
        userSid,
        requestDto.symbol().trim(),
        requestDto.name(),
        requestDto.type(),
        requestDto.currency()
    );

    final Asset createdAsset = assetService.createAsset(command);

    final AssetResponseDto response = new AssetResponseDto(
        createdAsset.getSid(),
        createdAsset.getTicker(),
        createdAsset.getName(),
        createdAsset.getAssetType().name(),
        createdAsset.getCurrency().name(),
        createdAsset.getStockExchange().getCountry().getName(),
        createdAsset.getStockExchange().getMarket()
    );

    final URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path(ASSET_SID_PATH)
        .buildAndExpand(createdAsset.getSid())
        .toUri();

    return ResponseEntity.created(location).body(response);
  }

}
