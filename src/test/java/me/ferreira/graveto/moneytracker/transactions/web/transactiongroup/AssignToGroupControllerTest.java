package me.ferreira.graveto.moneytracker.transactions.web.transactiongroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import me.ferreira.graveto.config.AuthUtils;
import me.ferreira.graveto.config.TestSecurityConfig;
import me.ferreira.graveto.moneytracker.transactions.service.TransactionGroupService;
import me.ferreira.graveto.moneytracker.transactions.service.command.transactiongroup.AssignToGroupCommand;
import me.ferreira.graveto.moneytracker.transactions.service.payload.AssignToGroupDetails;
import me.ferreira.graveto.moneytracker.transactions.web.TransactionGroupController;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.transactiongroup.AssignToGroupRequestDto;
import me.ferreira.graveto.moneytracker.transactions.web.helper.TransactionGroupDtoAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(
    controllers = TransactionGroupController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "me.ferreira.graveto.identity.*"
    ))
@Import(TestSecurityConfig.class)
public class AssignToGroupControllerTest {

  private static final String URI = "/transaction-groups";

  @Autowired
  private MockMvcTester mvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockitoBean
  private TransactionGroupService service;

  private static Stream<Arguments> invalidRequests() {
    final Set<UUID> sids = Set.of(UUID.randomUUID());
    final Set<UUID> tooManySids = Stream.generate(UUID::randomUUID).limit(501).collect(Collectors.toSet());

    return Stream.of(
        Arguments.of(new AssignToGroupRequestDto(
            null, null, "Algarve", sids, false), "accountSid"),
        Arguments.of(new AssignToGroupRequestDto(
            UUID.randomUUID(), null, "Algarve", null, false), "transactionSids"),
        Arguments.of(new AssignToGroupRequestDto(
            UUID.randomUUID(), null, "Algarve", Set.of(), false), "transactionSids"),
        Arguments.of(new AssignToGroupRequestDto(
            UUID.randomUUID(), null, "Algarve", tooManySids, false), "transactionSids"),
        Arguments.of(new AssignToGroupRequestDto(
            UUID.randomUUID(), null, "a".repeat(256), sids, false), "groupName"),
        Arguments.of(new AssignToGroupRequestDto(
            UUID.randomUUID(), null, null, sids, false), "groupTargetProvided"),
        Arguments.of(new AssignToGroupRequestDto(
            UUID.randomUUID(), null, "   ", sids, false), "groupTargetProvided")
    );
  }

  @ParameterizedTest
  @MethodSource("invalidRequests")
  void shouldReturnBadRequestForInvalidPayloads(
      final AssignToGroupRequestDto request,
      final String expectedErrorField) {

    final MvcTestResult result = mvc.post()
        .uri(URI)
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    assertThat(result)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .hasPath("$.invalid_params." + expectedErrorField);
  }

  @Test
  void shouldAssignToGroupByNameSuccessfully() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final Set<UUID> transactionSids = Set.of(UUID.randomUUID(), UUID.randomUUID());

    final AssignToGroupRequestDto request =
        new AssignToGroupRequestDto(accountSid, null, "Algarve", transactionSids, false);

    final AssignToGroupDetails details = new AssignToGroupDetails(UUID.randomUUID(), "Algarve", 5L, 2, 0, 0);

    final ArgumentCaptor<AssignToGroupCommand> commandCaptor = ArgumentCaptor.forClass(AssignToGroupCommand.class);
    when(service.assignToGroup(commandCaptor.capture())).thenReturn(details);

    // Act
    final MvcTestResult result = mvc.post()
        .uri(URI)
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);

    final AssignToGroupCommand captured = commandCaptor.getValue();
    assertThat(captured.userSid()).isEqualTo(userSid);
    assertThat(captured.accountSid()).isEqualTo(accountSid);
    assertThat(captured.groupSid()).isNull();
    assertThat(captured.groupName()).isEqualTo("Algarve");
    assertThat(captured.transactionSids()).isEqualTo(transactionSids);
    assertThat(captured.replaceExistingGroup()).isFalse();

    TransactionGroupDtoAssertions.assertAssignResponse(result, details);
  }

  @Test
  void shouldAssignToGroupBySidAndPassReplaceFlagToService() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final UUID groupSid = UUID.randomUUID();
    final Set<UUID> transactionSids = Set.of(UUID.randomUUID());

    final AssignToGroupRequestDto request =
        new AssignToGroupRequestDto(accountSid, groupSid, null, transactionSids, true);

    final AssignToGroupDetails details = new AssignToGroupDetails(groupSid, "Algarve", 7L, 0, 1, 0);

    final ArgumentCaptor<AssignToGroupCommand> commandCaptor = ArgumentCaptor.forClass(AssignToGroupCommand.class);
    when(service.assignToGroup(commandCaptor.capture())).thenReturn(details);

    // Act
    final MvcTestResult result = mvc.post()
        .uri(URI)
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);

    final AssignToGroupCommand captured = commandCaptor.getValue();
    assertThat(captured.userSid()).isEqualTo(userSid);
    assertThat(captured.groupSid()).isEqualTo(groupSid);
    assertThat(captured.groupName()).isNull();
    assertThat(captured.replaceExistingGroup()).isTrue();

    TransactionGroupDtoAssertions.assertAssignResponse(result, details);
  }

  @Test
  void shouldDefaultReplaceExistingGroupToFalseWhenOmitted() {
    // Arrange
    final String body = """
        {"accountSid": "%s", "groupName": "Algarve", "transactionSids": ["%s"]}
        """.formatted(UUID.randomUUID(), UUID.randomUUID());

    final AssignToGroupDetails details = new AssignToGroupDetails(UUID.randomUUID(), "Algarve", 1L, 1, 0, 0);

    final ArgumentCaptor<AssignToGroupCommand> commandCaptor = ArgumentCaptor.forClass(AssignToGroupCommand.class);
    when(service.assignToGroup(commandCaptor.capture())).thenReturn(details);

    // Act
    final MvcTestResult result = mvc.post()
        .uri(URI)
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .content(body)
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);
    assertThat(commandCaptor.getValue().replaceExistingGroup()).isFalse();
  }

}
