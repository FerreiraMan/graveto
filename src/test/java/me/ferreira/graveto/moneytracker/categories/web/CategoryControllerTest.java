package me.ferreira.graveto.moneytracker.categories.web;

import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.categories.service.command.CreateCategoryCommand;
import me.ferreira.graveto.moneytracker.categories.web.dto.request.CreateCategoryRequestDTO;
import me.ferreira.graveto.moneytracker.categories.web.dto.response.CategoryResponseDTO;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.utils.CategoryUtils;
import me.ferreira.graveto.moneytracker.utils.common.ControllerUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

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

    @Test
    void shouldCreateNewCategory() {
        // Arrange
        final UUID userSid = UUID.randomUUID();
        final UUID categorySid = UUID.randomUUID();
        final UUID parentSid = UUID.randomUUID();
        final String categoryName = "Videogames";

        final CreateCategoryRequestDTO request = new CreateCategoryRequestDTO(categoryName, parentSid, TransactionType.EXPENSE);

        final Category mockParent = new Category();
        mockParent.setSid(parentSid);
        final Category mockCategory = new Category();
        mockCategory.setSid(categorySid);
        mockCategory.setName(categoryName);
        mockCategory.setDisplayName(categoryName);
        mockCategory.setParent(mockParent);

        final ArgumentCaptor<CreateCategoryCommand> commandCaptor = ArgumentCaptor.forClass(CreateCategoryCommand.class);
        when(service.createCategory(commandCaptor.capture())).thenReturn(mockCategory);

        // Act
        final MvcTestResult testResult = mvc.post()
                .uri("/categories")
                .header("X-User-Sid", userSid)
                .content(ControllerUtils.asJsonString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .exchange();

        // Assert
        assertThat(testResult).hasStatus(HttpStatus.CREATED);
        assertThat(testResult).hasHeader("Location", "http://localhost/categories/" + categorySid);

        final CreateCategoryCommand capturedCommand = commandCaptor.getValue();
        assertThat(capturedCommand.userSid()).isEqualTo(userSid);
        assertThat(capturedCommand.name()).isEqualTo(categoryName);
        assertThat(capturedCommand.parentSid()).isEqualTo(parentSid);

        assertThat(testResult).bodyJson()
                .extractingPath("$.sid").asString().isEqualTo(categorySid.toString());
        assertThat(testResult).bodyJson()
                .extractingPath("$.name").asString().isEqualTo(categoryName);
        assertThat(testResult).bodyJson()
                .extractingPath("$.parentSid").asString().isEqualTo(parentSid.toString());
        assertThat(testResult).bodyJson()
                .extractingPath("$.isSystem").asBoolean().isFalse();
    }

    @ParameterizedTest
    @MethodSource("invalidCategoryCreationRequest")
    void shouldReturnBadRequestForInvalidPayloadsOnCategoryCreation(final String name,
                                                                    final TransactionType transactionType,
                                                                    final String invalidParam) {

        final CreateCategoryRequestDTO request = new CreateCategoryRequestDTO(name, null, transactionType);

        final MvcTestResult testResult = mvc.post()
                .uri("/categories")
                .content(ControllerUtils.asJsonString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Sid", UUID.randomUUID())
                .accept(MediaType.APPLICATION_JSON)
                .exchange();

        assertThat(testResult)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPath("$.invalid_params." + invalidParam);
    }

    private static Stream<Arguments> invalidCategoryCreationRequest() {
        return Stream.of(
                Arguments.of((Object) null, TransactionType.EXPENSE, "name"),
                Arguments.of("", TransactionType.EXPENSE, "name"),
                Arguments.of("   ", TransactionType.EXPENSE, "name"),
                Arguments.of("Lunch", null, "transactionType")
        );
    }

}
