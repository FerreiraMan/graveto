package me.ferreira.graveto.moneytracker.transactions.web.helper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.format.DateTimeFormatter;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.service.transfer.payload.TransferResult;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

public final class TransferDtoAssertions {

  private TransferDtoAssertions() {
  }

  public static void assertResponse(final MvcTestResult testResult, final TransferResult expected) {
    assertTransactionResponse(testResult, expected.expense(), "$.sourceTransaction");
    assertTransactionResponse(testResult, expected.income(), "$.destinationTransaction");
  }

  private static void assertTransactionResponse(final MvcTestResult testResult, final Transaction transaction,
                                                final String basePath) {
    assertThat(testResult).bodyJson().extractingPath("$.correlationId").asString()
        .isEqualTo(transaction.getCorrelationId().toString());
    assertThat(testResult).bodyJson().extractingPath(basePath + ".sid").asString()
        .isEqualTo(transaction.getSid().toString());
    assertThat(testResult).bodyJson().extractingPath(basePath + ".amount").asNumber()
        .isEqualTo(transaction.getAmount().intValue());
    assertThat(testResult).bodyJson().extractingPath(basePath + ".currency").asString()
        .isEqualTo(transaction.getCurrency().name());
    assertThat(testResult).bodyJson().extractingPath(basePath + ".type").asString()
        .isEqualTo(transaction.getType().name());
    assertThat(testResult).bodyJson().extractingPath(basePath + ".account.sid").asString()
        .isEqualTo(transaction.getAccount().getSid().toString());
    assertThat(testResult).bodyJson().extractingPath(basePath + ".account.name").asString()
        .isEqualTo(transaction.getAccount().getInstitution());
    assertThat(testResult).bodyJson().extractingPath(basePath + ".category.sid").asString()
        .isEqualTo(transaction.getCategory().getSid().toString());
    assertThat(testResult).bodyJson().extractingPath(basePath + ".category.name").asString()
        .isEqualTo(transaction.getCategory().getDisplayName());

    final String expectedOccurredAt = transaction.getOccurredAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    assertThat(testResult).bodyJson().extractingPath(basePath + ".occurredAt").asString()
        .isEqualTo(expectedOccurredAt);

    if (transaction.getDescription() != null) {
      assertThat(testResult).bodyJson().extractingPath(basePath + ".description").asString()
          .isEqualTo(transaction.getDescription());
    } else {
      assertThat(testResult).bodyJson().doesNotHavePath(basePath + ".description");
    }

    if (transaction.getCorrelationId() != null) {
      assertThat(testResult).bodyJson().extractingPath(basePath + ".correlationId").asString()
          .isEqualTo(transaction.getCorrelationId().toString());
    } else {
      assertThat(testResult).bodyJson().doesNotHavePath(basePath + ".correlationId");
    }

    if (transaction.getDeletedAt() != null) {
      final String expectedDeletedAt = transaction.getDeletedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
      assertThat(testResult).bodyJson().extractingPath(basePath + ".deletedAt").asString()
          .isEqualTo(expectedDeletedAt);
    } else {
      assertThat(testResult).bodyJson().doesNotHavePath(basePath + ".deletedAt");
    }
  }

}
