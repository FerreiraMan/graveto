package me.ferreira.graveto.portfolio.assets.client.impl.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record SearchAssetResponseDto(
    @JsonProperty("ResultSet") ResultSet resultSet
) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ResultSet(
      @JsonProperty("Result") List<Result> result
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Result(
      String symbol,
      String name,
      @JsonProperty("exchDisp") String exchange,
      @JsonProperty("typeDisp") String type
  ) {
  }
}
