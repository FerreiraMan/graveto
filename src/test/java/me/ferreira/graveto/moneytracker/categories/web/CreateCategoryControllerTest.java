package me.ferreira.graveto.moneytracker.categories.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.util.UUID;
import java.util.stream.Stream;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.categories.service.command.CreateCategoryCommand;
import me.ferreira.graveto.moneytracker.categories.web.dto.request.CreateCategoryRequestDto;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.utils.common.AuthUtils;
import me.ferreira.graveto.moneytracker.utils.common.ControllerUtils;
import me.ferreira.graveto.moneytracker.utils.common.TestSecurityConfig;
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

@WebMvcTest(
    controllers = CategoryController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "me.ferreira.graveto.identity.*"
    ))
@Import(TestSecurityConfig.class)
public class CreateCategoryControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @MockitoBean
  private CategoryService service;

  private static Stream<Arguments> invalidCategoryCreationRequest() {
    return Stream.of(
        Arguments.of(null, UUID.randomUUID(), TransactionType.EXPENSE, "name"),
        Arguments.of("", UUID.randomUUID(), TransactionType.EXPENSE, "name"),
        Arguments.of("   ", UUID.randomUUID(), TransactionType.EXPENSE, "name"),
        Arguments.of("Lunch", UUID.randomUUID(), null, "transactionType"),
        Arguments.of("Lunch", null, TransactionType.EXPENSE, "accountSid")
    );
  }

  @Test
  void shouldCreateNewCategory() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID categorySid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();
    final UUID parentSid = UUID.randomUUID();
    final String categoryName = "Videogames";

    final CreateCategoryRequestDto request =
        new CreateCategoryRequestDto(categoryName, accountSid, parentSid, TransactionType.EXPENSE);

    final Category mockParent = new Category();
    mockParent.setSid(parentSid);
    final Category mockCategory = new Category();
    mockCategory.setSid(categorySid);
    mockCategory.setName(categoryName);
    mockCategory.setAccountSid(accountSid);
    mockCategory.setDisplayName(categoryName);
    mockCategory.setParent(mockParent);

    final ArgumentCaptor<CreateCategoryCommand> commandCaptor = ArgumentCaptor.forClass(CreateCategoryCommand.class);
    when(service.createCategory(commandCaptor.capture())).thenReturn(mockCategory);

    // Act
    final MvcTestResult testResult = mvc.post()
        .uri("/categories")
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .content(ControllerUtils.asJsonString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(testResult).hasStatus(HttpStatus.CREATED);
    assertThat(testResult).hasHeader("Location", "http://localhost/categories/" + categorySid);

    final CreateCategoryCommand capturedCommand = commandCaptor.getValue();
    assertThat(capturedCommand.userSid()).isEqualTo(userSid);
    assertThat(capturedCommand.name()).isEqualTo(categoryName);
    assertThat(capturedCommand.accountSid()).isEqualTo(accountSid);
    assertThat(capturedCommand.parentSid()).isEqualTo(parentSid);

    assertThat(testResult).bodyJson()
        .extractingPath("$.sid").asString().isEqualTo(categorySid.toString());
    assertThat(testResult).bodyJson()
        .extractingPath("$.name").asString().isEqualTo(categoryName);
    assertThat(testResult).bodyJson()
        .extractingPath("$.accountSid").asString().isEqualTo(accountSid.toString());
    assertThat(testResult).bodyJson()
        .extractingPath("$.parentSid").asString().isEqualTo(parentSid.toString());
    assertThat(testResult).bodyJson()
        .extractingPath("$.isSystem").asBoolean().isFalse();
  }

  @ParameterizedTest
  @MethodSource("invalidCategoryCreationRequest")
  void shouldReturnBadRequestForInvalidPayloadsOnCategoryCreation(final String name,
                                                                  final UUID accountSid,
                                                                  final TransactionType transactionType,
                                                                  final String invalidParam) {

    final CreateCategoryRequestDto request = new CreateCategoryRequestDto(name, accountSid, null, transactionType);

    final MvcTestResult testResult = mvc.post()
        .uri("/categories")
        .content(ControllerUtils.asJsonString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .with(authentication(AuthUtils.mockAuth(UUID.randomUUID())))
        .accept(MediaType.APPLICATION_JSON)
        .exchange();

    assertThat(testResult)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .hasPath("$.invalid_params." + invalidParam);
  }

}
