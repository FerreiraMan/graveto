package me.ferreira.graveto.moneytracker.analytics.web;

import me.ferreira.graveto.moneytracker.analytics.service.AnalyticService;
import me.ferreira.graveto.moneytracker.analytics.service.command.CategorySpendingCommand;
import me.ferreira.graveto.moneytracker.analytics.service.payload.CategorySpendingResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = AnalyticsController.class)
public class FetchCategorySpendingReportControllerTest {

    @Autowired
    private MockMvcTester mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private AnalyticService service;

    @Test
    void shouldReturnCategorySpendingReportAndMapToResponseDTO() {
        // Arrange
        final UUID userSid = UUID.randomUUID();
        final UUID accountSid = UUID.randomUUID();
        final int targetYear = 2026;

        final UUID parentCategorySid = UUID.randomUUID();
        final UUID childCategorySid = UUID.randomUUID();

        final CategorySpendingResult.CategoryAggregate childAggregate = new CategorySpendingResult.CategoryAggregate(
                childCategorySid,
                "Gas",
                BigDecimal.valueOf(50),
                Map.of(1, BigDecimal.valueOf(50)),
                List.of()
        );

        final CategorySpendingResult.CategoryAggregate parentAggregate = new CategorySpendingResult.CategoryAggregate(
                parentCategorySid,
                "Transportation",
                BigDecimal.valueOf(150),
                Map.of(1, BigDecimal.valueOf(150)),
                List.of(childAggregate)
        );

        final CategorySpendingResult mockResult = new CategorySpendingResult(
                targetYear,
                List.of(parentAggregate)
        );

        final ArgumentCaptor<CategorySpendingCommand> commandCaptor = ArgumentCaptor.forClass(CategorySpendingCommand.class);
        when(service.generateCategorySpendingReport(commandCaptor.capture())).thenReturn(mockResult);

        // Act
        final MvcTestResult testResult = mvc.get()
                .uri("/analytics/{accountSid}/category-spending", accountSid)
                .queryParam("year", String.valueOf(targetYear))
                .header("X-User-Sid", userSid)
                .exchange();

        // Assert
        assertThat(testResult).hasStatus(HttpStatus.OK);

        final CategorySpendingCommand capturedCommand = commandCaptor.getValue();
        assertThat(capturedCommand.userSid()).isEqualTo(userSid);
        assertThat(capturedCommand.accountSid()).isEqualTo(accountSid);
        assertThat(capturedCommand.year()).isEqualTo(targetYear);

        assertThat(testResult).bodyJson()
                .extractingPath("$.year").asNumber().isEqualTo(targetYear);
        assertThat(testResult).bodyJson()
                .extractingPath("$.categories[0].categorySid").asString().isEqualTo(parentCategorySid.toString());
        assertThat(testResult).bodyJson()
                .extractingPath("$.categories[0].categoryName").asString().isEqualTo("Transportation");
        assertThat(testResult).bodyJson()
                .extractingPath("$.categories[0].yearlyTotal").asNumber().isEqualTo(150);

        assertThat(testResult).bodyJson()
                .extractingPath("$.categories[0].childCategories[0].categorySid").asString().isEqualTo(childCategorySid.toString());
        assertThat(testResult).bodyJson()
                .extractingPath("$.categories[0].childCategories[0].categoryName").asString().isEqualTo("Gas");
        assertThat(testResult).bodyJson()
                .extractingPath("$.categories[0].childCategories[0].yearlyTotal").asNumber().isEqualTo(50);
    }

    @Test
    void shouldDefaultToCurrentYearIfNoYearIsProvidedInQuery() {
        // Arrange
        final UUID userSid = UUID.randomUUID();
        final UUID accountSid = UUID.randomUUID();
        final int expectedDefaultYear = Year.now().getValue();

        final CategorySpendingResult mockResult = new CategorySpendingResult(
                expectedDefaultYear, List.of()
        );

        final ArgumentCaptor<CategorySpendingCommand> commandCaptor = ArgumentCaptor.forClass(CategorySpendingCommand.class);
        when(service.generateCategorySpendingReport(commandCaptor.capture())).thenReturn(mockResult);

        // Act
        mvc.get()
                .uri("/analytics/{accountSid}/category-spending", accountSid)
                .header("X-User-Sid", userSid)
                .exchange();

        // Assert
        final CategorySpendingCommand capturedCommand = commandCaptor.getValue();
        assertThat(capturedCommand.year()).isEqualTo(expectedDefaultYear);
    }

    @Test
    void shouldReturnBadRequestForInvalidAccountSid() {
        // Act
        final MvcTestResult testResult = mvc.get()
                .uri("/analytics/{accountSid}/category-spending", "invalid-uuid-string")
                .header("X-User-Sid", UUID.randomUUID())
                .exchange();

        // Assert
        assertThat(testResult).hasStatus(HttpStatus.BAD_REQUEST);
    }

}
