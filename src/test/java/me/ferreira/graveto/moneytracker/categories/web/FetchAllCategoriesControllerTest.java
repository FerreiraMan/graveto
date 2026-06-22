package me.ferreira.graveto.moneytracker.categories.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.util.List;
import java.util.UUID;
import me.ferreira.graveto.config.AuthUtils;
import me.ferreira.graveto.config.TestSecurityConfig;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.categories.service.command.FetchAllCategoriesCommand;
import me.ferreira.graveto.moneytracker.categories.web.dto.response.CategoryResponseDto;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.utils.CategoryUtils;
import me.ferreira.graveto.moneytracker.utils.common.ControllerUtils;
import org.junit.jupiter.api.Test;
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
public class FetchAllCategoriesControllerTest {

  @Autowired
  private MockMvcTester mvc;
  @MockitoBean
  private CategoryService service;

  @Test
  void shouldReturnAllCategories() throws Exception {
    // Arrange
    final UUID userSid = UUID.randomUUID();
    final UUID accountSid = UUID.randomUUID();

    final Category parentCategory = CategoryUtils.createCategory("Gas", null, null, false, TransactionType.EXPENSE);
    final Category childCategory =
        CategoryUtils.createCategory("Diesel", userSid, parentCategory, false, TransactionType.EXPENSE);

    when(service.fetchAllCategories(new FetchAllCategoriesCommand(userSid, accountSid))).thenReturn(
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
              assertThat(parent.name()).isEqualTo("Gas");
              assertThat(parent.parentSid()).isNull();
              assertThat(parent.isSystem()).isTrue();
            },
            child -> {
              assertThat(child.sid()).isEqualTo(childCategory.getSid());
              assertThat(child.name()).isEqualTo("Diesel");
              assertThat(child.parentSid()).isEqualTo(parentCategory.getSid());
              assertThat(child.isSystem()).isFalse();
            }
        );
  }

}
