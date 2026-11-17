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
import me.ferreira.graveto.moneytracker.transactions.service.command.transactiongroup.UngroupCommand;
import me.ferreira.graveto.moneytracker.transactions.service.payload.UngroupDetails;
import me.ferreira.graveto.moneytracker.transactions.web.TransactionGroupController;
import me.ferreira.graveto.moneytracker.transactions.web.dto.request.transactiongroup.UngroupRequestDto;
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
public class UngroupControllerTest {

  private static final String URI = "/transaction-groups/ungroup";

  @Autowired
  private MockMvcTester mvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockitoBean
  private TransactionGroupService service;

  private static Stream<Arguments> invalidRequests() {
    final Set<UUID> tooManySids = Stream.generate(UUID::randomUUID).limit(501).collect(Collectors.toSet());

    return Stream.of(
        Arguments.of(new UngroupRequestDto(null, Set.of(UUID.randomUUID())), "accountSid"),
        Arguments.of(new UngroupRequestDto(UUID.randomUUID(), null), "transactionSids"),
        Arguments.of(new UngroupRequestDto(UUID.randomUUID(), Set.of()), "transactionSids"),
        Arguments.of(new UngroupRequestDto(UUID.randomUUID(), tooManySids), "transactionSids")
    );
  }

  @ParameterizedTest
  @MethodSource("invalidRequests")
  void shouldReturnBadRequestForInvalidPayloads(
      final UngroupRequestDto request,
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
  void shouldUngroupSuccessfully() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final Set<UUID> transactionSids = Set.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

    final UngroupRequestDto request = new UngroupRequestDto(accountSid, transactionSids);

    final UngroupDetails details = new UngroupDetails(2, 1);

    final ArgumentCaptor<UngroupCommand> commandCaptor = ArgumentCaptor.forClass(UngroupCommand.class);
    when(service.ungroup(commandCaptor.capture())).thenReturn(details);

    // Act
    final MvcTestResult result = mvc.post()
        .uri(URI)
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .content(objectMapper.writeValueAsString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);

    final UngroupCommand captured = commandCaptor.getValue();
    assertThat(captured.userSid()).isEqualTo(userSid);
    assertThat(captured.accountSid()).isEqualTo(accountSid);
    assertThat(captured.transactionSids()).isEqualTo(transactionSids);

    TransactionGroupDtoAssertions.assertUngroupResponse(result, details);
  }

}
