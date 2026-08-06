package me.ferreira.graveto.moneytracker.categories.web;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.categories.service.command.CreateCategoryCommand;
import me.ferreira.graveto.moneytracker.categories.service.command.FindAllCategoriesCommand;
import me.ferreira.graveto.moneytracker.categories.web.dto.request.CategoryFilterRequestDto;
import me.ferreira.graveto.moneytracker.categories.web.dto.request.CreateCategoryRequestDto;
import me.ferreira.graveto.moneytracker.categories.web.dto.response.CategoryResponseDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(value = "/categories")
@RequiredArgsConstructor
public class CategoryController {

  private static final String CATEGORY_SID_PATH = "/{sid}";

  private final CategoryService categoryService;

  @GetMapping(produces = "application/json")
  public ResponseEntity<List<CategoryResponseDto>> fetchAllCategories(
      @AuthenticationPrincipal final UUID userSid,
      @Valid @ModelAttribute final CategoryFilterRequestDto requestDto) {

    final FindAllCategoriesCommand command = new FindAllCategoriesCommand(
        userSid,
        StringUtils.trimToNull(requestDto.displayName()),
        requestDto.accountSid(),
        requestDto.parentSid(),
        requestDto.type());

    final List<Category> categories = categoryService.fetchAllCategories(command);

    return ResponseEntity.ok().body(categories.stream().map(CategoryResponseDto::from).toList());
  }

  @PostMapping(produces = "application/json")
  public ResponseEntity<CategoryResponseDto> createCategory(
      @AuthenticationPrincipal final UUID userSid,
      @Valid @RequestBody final CreateCategoryRequestDto requestDto) {

    final CreateCategoryCommand command = new CreateCategoryCommand(
        userSid,
        requestDto.name().trim(),
        requestDto.accountSid(),
        requestDto.parentSid(),
        requestDto.transactionType()
    );

    final Category createdCategory = categoryService.createCategory(command);

    final URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path(CATEGORY_SID_PATH)
        .buildAndExpand(createdCategory.getSid())
        .toUri();

    return ResponseEntity.created(location).body(CategoryResponseDto.from(createdCategory));
  }

}
