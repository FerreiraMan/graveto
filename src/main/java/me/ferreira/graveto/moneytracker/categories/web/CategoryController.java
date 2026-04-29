package me.ferreira.graveto.moneytracker.categories.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.categories.service.command.CreateCategoryCommand;
import me.ferreira.graveto.moneytracker.categories.web.dto.request.CreateCategoryRequestDTO;
import me.ferreira.graveto.moneytracker.categories.web.dto.response.CategoryResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping(value = "/categories")
@RequiredArgsConstructor
public class CategoryController {

    private static final String CATEGORY_SID_PATH = "/{sid}";

    private final CategoryService categoryService;

    @GetMapping(produces = "application/json")
    public ResponseEntity<List<CategoryResponseDTO>> fetchAllCategories(
            @RequestHeader("X-User-Sid") final UUID userSid) {

        final List<Category> categories = categoryService.fetchAllCategories(userSid);

        final List<CategoryResponseDTO> responseDTO = categories.stream()
                .map(
                c -> new CategoryResponseDTO(
                        c.getSid(),
                        c.getDisplayName(),
                        Objects.nonNull(c.getParent()) ? c.getParent().getSid() : null,
                        Objects.isNull(c.getUserSid())
                ))
                .toList();

        return ResponseEntity.ok().body(responseDTO);
    }

    @PostMapping(produces = "application/json")
    public ResponseEntity<CategoryResponseDTO> createCategory(
            @Valid @RequestBody final CreateCategoryRequestDTO requestDTO,
            @RequestHeader("X-User-Sid") final UUID userSid) {

        final CreateCategoryCommand command = new CreateCategoryCommand(
            userSid,
            requestDTO.name().trim(),
            requestDTO.parentSid(),
            requestDTO.transactionType()
        );

        final Category createdCategory = categoryService.createCategory(command);

        final CategoryResponseDTO response = new CategoryResponseDTO(
            createdCategory.getSid(),
            createdCategory.getDisplayName(),
            Objects.isNull(createdCategory.getParent()) ? null : createdCategory.getParent().getSid(),
            false
        );

        final URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path(CATEGORY_SID_PATH)
                .buildAndExpand(createdCategory.getSid())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

}
