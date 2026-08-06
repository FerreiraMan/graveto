package me.ferreira.graveto.moneytracker.categories.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.config.AuthUtils;
import me.ferreira.graveto.config.TestSecurityConfig;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.categories.service.command.FindAllCategoriesCommand;
import me.ferreira.graveto.moneytracker.categories.web.dto.response.CategoryResponseDto;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.utils.CategoryUtils;
import me.ferreira.graveto.moneytracker.utils.common.ControllerUtils;
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
    controllers = CategoryController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "me.ferreira.graveto.identity.*"
    ))
@Import(TestSecurityConfig.class)
public class FindAllCategoriesControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @MockitoBean
  private CategoryService service;

  @Test
  void shouldFetchAllCategoriesSuccessfully() throws Exception {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();

    final Category parentCategory = CategoryUtils.createCategory("Gas", null, null, false, TransactionType.EXPENSE);
    final Category childCategory =
        CategoryUtils.createCategory("Diesel", userSid, parentCategory, false, TransactionType.EXPENSE);

    when(service.fetchAllCategories(new FindAllCategoriesCommand(userSid, null, accountSid, null, null))).thenReturn(
        List.of(parentCategory, childCategory));

    // Act
    final MvcTestResult testResult = mvc.get()
        .uri("/categories")
        .param("accountSid", accountSid.toString())
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .exchange();

    // Assert
    assertThat(testResult).hasStatus(HttpStatus.OK);

    final List<CategoryResponseDto> categories =
        ControllerUtils.convertIntoObjectList(testResult, CategoryResponseDto.class);

    assertThat(categories)
        .hasSize(2)
        .satisfiesExactly(
            parent -> {
              assertThat(parent.sid()).isEqualTo(parentCategory.getSid());
              assertThat(parent.displayName()).isEqualTo("Gas");
              assertThat(parent.parentSid()).isNull();
              assertThat(parent.isSystem()).isTrue();
            },
            child -> {
              assertThat(child.sid()).isEqualTo(childCategory.getSid());
              assertThat(child.displayName()).isEqualTo("Diesel");
              assertThat(child.parentSid()).isEqualTo(parentCategory.getSid());
              assertThat(child.isSystem()).isFalse();
            }
        );
  }

  @Test
  void shouldPassDisplayNameFilterToCommand() {
    // Arrange
    final UUID userSid = UUID.randomUUID();

    final ArgumentCaptor<FindAllCategoriesCommand> commandCaptor =
        ArgumentCaptor.forClass(FindAllCategoriesCommand.class);
    when(service.fetchAllCategories(commandCaptor.capture())).thenReturn(List.of());

    // Act
    final MvcTestResult result = mvc.get()
        .uri("/categories")
        .param("displayName", "Book")
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);
    assertThat(commandCaptor.getValue().displayName()).isEqualTo("Book");
  }

  @Test
  void shouldPassAccountSidFilterToCommand() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();

    final ArgumentCaptor<FindAllCategoriesCommand> commandCaptor =
        ArgumentCaptor.forClass(FindAllCategoriesCommand.class);
    when(service.fetchAllCategories(commandCaptor.capture())).thenReturn(List.of());

    // Act
    final MvcTestResult result = mvc.get()
        .uri("/categories")
        .param("accountSid", accountSid.toString())
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);
    assertThat(commandCaptor.getValue().accountSid().toString()).isEqualTo(accountSid.toString());
  }

  @Test
  void shouldPassParentSidFilterToCommand() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID parentSid = UUID.randomUUID();

    final ArgumentCaptor<FindAllCategoriesCommand> commandCaptor =
        ArgumentCaptor.forClass(FindAllCategoriesCommand.class);
    when(service.fetchAllCategories(commandCaptor.capture())).thenReturn(List.of());

    // Act
    final MvcTestResult result = mvc.get()
        .uri("/categories")
        .param("parentSid", parentSid.toString())
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);
    assertThat(commandCaptor.getValue().parentSid().toString()).isEqualTo(parentSid.toString());
  }

  @Test
  void shouldPassTypeFilterToCommand() {
    // Arrange
    final UUID userSid = UUID.randomUUID();

    final ArgumentCaptor<FindAllCategoriesCommand> commandCaptor =
        ArgumentCaptor.forClass(FindAllCategoriesCommand.class);
    when(service.fetchAllCategories(commandCaptor.capture())).thenReturn(List.of());

    // Act
    final MvcTestResult result = mvc.get()
        .uri("/categories")
        .param("type", TransactionType.EXPENSE.name())
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);
    assertThat(commandCaptor.getValue().type().name()).isEqualTo(TransactionType.EXPENSE.name());
  }

  @Test
  void shouldReturnEmptyListWhenNoCategoriesExist() {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    when(service.fetchAllCategories(any())).thenReturn(List.of());

    // Act
    final MvcTestResult result = mvc.get()
        .uri("/categories")
        .with(authentication(AuthUtils.mockAuth(userSid)))
        .exchange();

    // Assert
    assertThat(result).hasStatus(HttpStatus.OK);
    assertThat(result).bodyJson().extractingPath("$").asInstanceOf(InstanceOfAssertFactories.LIST).isEmpty();
  }

}