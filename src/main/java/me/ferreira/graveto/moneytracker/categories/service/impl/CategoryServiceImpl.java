package me.ferreira.graveto.moneytracker.categories.service.impl;

import lombok.AllArgsConstructor;
import me.ferreira.graveto.common.web.exception.moneytracker.CategoryAlreadyExistsException;
import me.ferreira.graveto.common.web.exception.moneytracker.CategoryNotFoundException;
import me.ferreira.graveto.common.web.exception.moneytracker.IllegalCategoryHierarchyException;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.domain.SystemCategory;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryRepository;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import me.ferreira.graveto.moneytracker.categories.service.command.CreateCategoryCommand;
import me.ferreira.graveto.moneytracker.categories.service.command.FetchCategoryCommand;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@AllArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final String INTERNAL_CATEGORY_SID_INVALID = "Requested SID is not a valid Internal Category.";
    private static final String INTERNAL_CATEGORY_NOT_FOUND = "Internal Category is missing from the database.";
    private static final String CATEGORY_NAME_NOT_PRESENT = "Category name cannot be empty.";
    private static final String DEFAULT_CATEGORIES_NOT_FOUND = "CRITICAL: Default Categories are missing from the database.";

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public Category fetchInternalCategory(final UUID systemCategorySid) {

        if (!SystemCategory.allSids().contains(systemCategorySid)) {
            throw new IllegalArgumentException(INTERNAL_CATEGORY_SID_INVALID);
        }

        return categoryRepository.findBySid(systemCategorySid)
                .orElseThrow(() -> new IllegalStateException(INTERNAL_CATEGORY_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Category fetchCategory(FetchCategoryCommand command) {

        return categoryRepository.findBySidOrUserSid(command.categorySid(), command.userSid())
                .orElseThrow(() -> new CategoryNotFoundException(command.categorySid()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> fetchAllCategories(final UUID userSid) {

        final List<Category> categories = categoryRepository.findAllByUserSid(userSid);

        final boolean hasSystemCategories = categories.stream()
                .anyMatch(c -> Objects.isNull(c.getUserSid()));

        if(!hasSystemCategories) {
            throw new IllegalStateException(DEFAULT_CATEGORIES_NOT_FOUND);
        }

        return categories;
    }

    @Override
    @Transactional
    public Category createCategory(final CreateCategoryCommand command) {

        final String sanitizedName = validateAndSanitizeName(command.name());

        if(categoryRepository.existsByNameForUserOrSystem(sanitizedName, command.userSid())) {
            throw new CategoryAlreadyExistsException(command.name());
        }

        Category parentCategory = null;

        if(command.parentSid() != null) {

            parentCategory = categoryRepository.findBySid(command.parentSid())
                    .orElseThrow(() -> new CategoryNotFoundException(command.parentSid()));

            if (Objects.nonNull(parentCategory.getUserSid()) && !parentCategory.getUserSid().equals(command.userSid())) {
                throw new IllegalCategoryHierarchyException();
            }

        }

        final Category category = Category.create(sanitizedName, command.name(), command.userSid(), parentCategory);

        return categoryRepository.save(category);
    }

    private String validateAndSanitizeName(final String name) {

        if(StringUtils.isBlank(name)) {
            throw new IllegalArgumentException(CATEGORY_NAME_NOT_PRESENT);
        }

        return StringUtils.stripAccents(name)
                .trim()
                .toUpperCase()
                .replaceAll("\\s+", "_");
    }

}
