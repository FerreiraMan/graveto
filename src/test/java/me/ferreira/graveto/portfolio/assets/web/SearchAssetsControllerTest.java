package me.ferreira.graveto.portfolio.assets.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.config.AuthUtils;
import me.ferreira.graveto.config.TestSecurityConfig;
import me.ferreira.graveto.portfolio.assets.service.AssetService;
import me.ferreira.graveto.portfolio.assets.service.command.SearchAssetCommand;
import me.ferreira.graveto.portfolio.assets.service.payload.AssetSearchRecommendation;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(
    controllers = AssetController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "me.ferreira.graveto.identity.*"
    ))
@Import(TestSecurityConfig.class)
public class SearchAssetsControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @MockitoBean
  private AssetService service;

  @Test
  void shouldSearchAssetsSuccessfully() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final String keyword = "IWDA";

    final List<AssetSearchRecommendation> recommendations = List.of(
        new AssetSearchRecommendation("IWDA.AS", "iShares Core MSCI World", "ETF", "Amsterdam"),
        new AssetSearchRecommendation("IWDA.L", "iShares Core MSCI World", "ETF", "London")
    );

    final ArgumentCaptor<SearchAssetCommand> commandCaptor = ArgumentCaptor.forClass(SearchAssetCommand.class);
    when(service.searchAsset(commandCaptor.capture())).thenReturn(recommendations);

    // Act
    final MvcTestResult result = mvc.get()
        .uri("/assets/search")
        .param("keyword", keyword)
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);

    final SearchAssetCommand captured = commandCaptor.getValue();
    assertThat(captured.userSid()).isEqualTo(userSid);
    assertThat(captured.keyword()).isEqualTo(keyword);

    assertThat(result).bodyJson().extractingPath("$[0].ticker").asString().isEqualTo("IWDA.AS");
    assertThat(result).bodyJson().extractingPath("$[0].name").asString().isEqualTo("iShares Core MSCI World");
    assertThat(result).bodyJson().extractingPath("$[0].type").asString().isEqualTo("ETF");
    assertThat(result).bodyJson().extractingPath("$[0].exchange").asString().isEqualTo("Amsterdam");
    assertThat(result).bodyJson().extractingPath("$[1].ticker").asString().isEqualTo("IWDA.L");
  }

  @Test
  void shouldReturnEmptyListWhenNoResultsFound() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    when(service.searchAsset(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

    // Act
    final MvcTestResult result = mvc.get()
        .uri("/assets/search")
        .param("keyword", "NONEXISTENT")
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);
    assertThat(result).bodyJson().extractingPath("$").asInstanceOf(InstanceOfAssertFactories.LIST).isEmpty();
  }

  @Test
  void shouldReturnBadRequestWhenKeywordIsTooShort() {
    // Act
    final MvcTestResult result = mvc.get()
        .uri("/assets/search")
        .param("keyword", "I")
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
  }

  @Test
  void shouldReturnBadRequestWhenKeywordIsTooLong() {
    // Act
    final MvcTestResult result = mvc.get()
        .uri("/assets/search")
        .param("keyword", "A".repeat(21))
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
  }

  @Test
  void shouldReturnBadRequestWhenKeywordIsMissing() {
    // Act
    final MvcTestResult result = mvc.get()
        .uri("/assets/search")
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
  }

}
