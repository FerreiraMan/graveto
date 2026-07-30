package me.ferreira.graveto.moneytracker.transactions.web.helper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.format.DateTimeFormatter;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransaction;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

public final class RecurringTransactionDtoAssertions {

  private RecurringTransactionDtoAssertions() {
  }

  public static void assertSingleResponse(final MvcTestResult testResult, final RecurringTransaction expected) {
    assertRecurringTransactionFields(testResult, expected, "$");
  }

  public static void assertListResponse(final MvcTestResult testResult, final RecurringTransaction expected,
                                        final int index) {
    assertRecurringTransactionFields(testResult, expected, "$[" + index + "]");
  }

  private static void assertRecurringTransactionFields(final MvcTestResult testResult,
                                                       final RecurringTransaction expected,
                                                       final String basePath) {

    assertThat(testResult).bodyJson().extractingPath(basePath + ".sid").asString()
        .isEqualTo(expected.getSid().toString());
    assertThat(testResult).bodyJson().extractingPath(basePath + ".account.sid").asString()
        .isEqualTo(expected.getAccount().getSid().toString());
    assertThat(testResult).bodyJson().extractingPath(basePath + ".account.name").asString()
        .isEqualTo(expected.getAccount().getInstitution());
    assertThat(testResult).bodyJson().extractingPath(basePath + ".category.sid").asString()
        .isEqualTo(expected.getCategory().getSid().toString());
    assertThat(testResult).bodyJson().extractingPath(basePath + ".category.name").asString()
        .isEqualTo(expected.getCategory().getDisplayName());
    assertThat(testResult).bodyJson().extractingPath(basePath + ".userSid").asString()
        .isEqualTo(expected.getUserSid().toString());
    assertThat(testResult).bodyJson().extractingPath(basePath + ".description").asString()
        .isEqualTo(expected.getDescription());
    assertThat(testResult).bodyJson().extractingPath(basePath + ".amount").asNumber()
        .isEqualTo(expected.getAmount().intValue());
    assertThat(testResult).bodyJson().extractingPath(basePath + ".currency").asString()
        .isEqualTo(expected.getCurrency().name());
    assertThat(testResult).bodyJson().extractingPath(basePath + ".transactionType").asString()
        .isEqualTo(expected.getType().name());
    assertThat(testResult).bodyJson().extractingPath(basePath + ".frequency").asString()
        .isEqualTo(expected.getFrequency().name());

    final String expectedNextExecutionDate =
        expected.getNextExecutionDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
    assertThat(testResult).bodyJson().extractingPath(basePath + ".nextExecutionDate").asString()
        .isEqualTo(expectedNextExecutionDate);

    assertThat(testResult).bodyJson().extractingPath(basePath + ".status").asString()
        .isEqualTo(expected.getStatus().name());

    if (expected.getEndDate() != null) {
      final String expectedEndDate = expected.getEndDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
      assertThat(testResult).bodyJson().extractingPath(basePath + ".endDate").asString()
          .isEqualTo(expectedEndDate);
    } else {
      assertThat(testResult).bodyJson().doesNotHavePath(basePath + ".endDate");
    }
  }

}
