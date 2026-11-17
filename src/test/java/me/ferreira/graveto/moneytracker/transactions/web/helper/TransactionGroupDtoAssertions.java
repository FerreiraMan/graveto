package me.ferreira.graveto.moneytracker.transactions.web.helper;

import static org.assertj.core.api.Assertions.assertThat;

import me.ferreira.graveto.moneytracker.transactions.service.payload.AssignToGroupDetails;
import me.ferreira.graveto.moneytracker.transactions.service.payload.UngroupDetails;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

public final class TransactionGroupDtoAssertions {

  private TransactionGroupDtoAssertions() {
  }

  public static void assertAssignResponse(final MvcTestResult testResult, final AssignToGroupDetails expected) {

    assertThat(testResult).bodyJson().extractingPath("$.groupSid").asString()
        .isEqualTo(expected.groupSid().toString());
    assertThat(testResult).bodyJson().extractingPath("$.groupName").asString()
        .isEqualTo(expected.groupName());
    assertThat(testResult).bodyJson().extractingPath("$.totalTransactions").asNumber()
        .isEqualTo(expected.totalTransactions().intValue());
    assertThat(testResult).bodyJson().extractingPath("$.added").asNumber()
        .isEqualTo(expected.added());
    assertThat(testResult).bodyJson().extractingPath("$.moved").asNumber()
        .isEqualTo(expected.moved());
    assertThat(testResult).bodyJson().extractingPath("$.alreadyInGroup").asNumber()
        .isEqualTo(expected.alreadyInGroup());

    assertThat(testResult).bodyJson().doesNotHavePath("$.removed");
    assertThat(testResult).bodyJson().doesNotHavePath("$.alreadyUngrouped");
  }

  public static void assertUngroupResponse(final MvcTestResult testResult, final UngroupDetails expected) {

    assertThat(testResult).bodyJson().extractingPath("$.removed").asNumber()
        .isEqualTo(expected.removed());
    assertThat(testResult).bodyJson().extractingPath("$.alreadyUngrouped").asNumber()
        .isEqualTo(expected.alreadyUngrouped());

    assertThat(testResult).bodyJson().doesNotHavePath("$.groupSid");
    assertThat(testResult).bodyJson().doesNotHavePath("$.groupName");
    assertThat(testResult).bodyJson().doesNotHavePath("$.totalTransactions");
    assertThat(testResult).bodyJson().doesNotHavePath("$.added");
    assertThat(testResult).bodyJson().doesNotHavePath("$.moved");
    assertThat(testResult).bodyJson().doesNotHavePath("$.alreadyInGroup");
  }

}
