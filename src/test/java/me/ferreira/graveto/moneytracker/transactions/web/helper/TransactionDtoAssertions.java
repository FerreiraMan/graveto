package me.ferreira.graveto.moneytracker.transactions.web.helper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.format.DateTimeFormatter;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

public final class TransactionDtoAssertions {

  private TransactionDtoAssertions() {
  }

  public static void assertSingleResponse(final MvcTestResult testResult, final Transaction expected) {
    assertTransactionFields(testResult, expected, "$");
  }

  public static void assertPageableResponse(final MvcTestResult testResult, final Transaction expected,
                                            final int index) {
    assertTransactionFields(testResult, expected, "$.content[" + index + "]");
  }

  private static void assertTransactionFields(final MvcTestResult testResult, final Transaction expected,
                                              final String basePath) {

    assertThat(testResult).bodyJson().extractingPath(basePath + ".sid").asString()
        .isEqualTo(expected.getSid().toString());
    assertThat(testResult).bodyJson().extractingPath(basePath + ".amount").asNumber()
        .isEqualTo(expected.getAmount().intValue());
    assertThat(testResult).bodyJson().extractingPath(basePath + ".currency").asString()
        .isEqualTo(expected.getCurrency().name());
    assertThat(testResult).bodyJson().extractingPath(basePath + ".type").asString()
        .isEqualTo(expected.getType().name());
    assertThat(testResult).bodyJson().extractingPath(basePath + ".account.sid").asString()
        .isEqualTo(expected.getAccount().getSid().toString());
    assertThat(testResult).bodyJson().extractingPath(basePath + ".account.name").asString()
        .isEqualTo(expected.getAccount().getInstitution());
    assertThat(testResult).bodyJson().extractingPath(basePath + ".category.sid").asString()
        .isEqualTo(expected.getCategory().getSid().toString());
    assertThat(testResult).bodyJson().extractingPath(basePath + ".category.name").asString()
        .isEqualTo(expected.getCategory().getDisplayName());

    final String expectedOccurredAt = expected.getOccurredAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    assertThat(testResult).bodyJson().extractingPath(basePath + ".occurredAt").asString()
        .isEqualTo(expectedOccurredAt);

    if (expected.getDescription() != null) {
      assertThat(testResult).bodyJson().extractingPath(basePath + ".description").asString()
          .isEqualTo(expected.getDescription());
    } else {
      assertThat(testResult).bodyJson().doesNotHavePath(basePath + ".description");
    }

    if (expected.getCorrelationId() != null) {
      assertThat(testResult).bodyJson().extractingPath(basePath + ".correlationId").asString()
          .isEqualTo(expected.getCorrelationId().toString());
    } else {
      assertThat(testResult).bodyJson().doesNotHavePath(basePath + ".correlationId");
    }

    if (expected.getDeletedAt() != null) {
      final String expectedDeletedAt = expected.getDeletedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
      assertThat(testResult).bodyJson().extractingPath(basePath + ".deletedAt").asString()
          .isEqualTo(expectedDeletedAt);
    } else {
      assertThat(testResult).bodyJson().doesNotHavePath(basePath + ".deletedAt");
    }
  }

}