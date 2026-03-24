package me.ferreira.graveto.moneytracker.categories.web;

import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.categories.web.dto.response.CategoryResponseDTO;
import me.ferreira.graveto.moneytracker.utils.CategoryUtils;
import me.ferreira.graveto.moneytracker.utils.common.ControllerUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = CategoryController.class)
public class CategoryControllerTest {

    @Autowired
    private MockMvcTester mvc;
    @MockitoBean
    private CategoryService service;

    @Test
    void shouldReturnAllCategories() throws Exception {
        // Arrange
        final UUID userSid = UUID.randomUUID();

        final Category parentCategory = CategoryUtils.createCategory("Gas", null, null, false);
        final Category childCategory = CategoryUtils.createCategory("Diesel", userSid, parentCategory, false);

        when(service.fetchAllCategories(userSid)).thenReturn(List.of(parentCategory, childCategory));

        // Act
        final MvcTestResult testResult = mvc.get()
                .uri("/categories")
                .header("X-User-Sid", userSid)
                .exchange();

        // Assert
        assertThat(testResult).hasStatus(HttpStatus.OK);

        final List<CategoryResponseDTO> categories = ControllerUtils.convertIntoObjectList(testResult, CategoryResponseDTO.class);

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
