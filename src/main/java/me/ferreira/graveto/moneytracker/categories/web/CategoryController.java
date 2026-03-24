package me.ferreira.graveto.moneytracker.categories.web;

import lombok.RequiredArgsConstructor;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.categories.web.dto.response.CategoryResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping(value = "/categories")
@RequiredArgsConstructor
public class CategoryController {

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

}
