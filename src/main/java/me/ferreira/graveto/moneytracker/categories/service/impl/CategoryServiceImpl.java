package me.ferreira.graveto.moneytracker.categories.service.impl;

import lombok.AllArgsConstructor;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.domain.SystemCategory;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryRepository;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@AllArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    public static final String SYSTEM_CATEGORY_NOT_FOUND = "System category is missing from the database.";
    public static final String SYSTEM_CATEGORIES_NOT_FOUND = "CRITICAL: System categories are missing from the database.";

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public Category getInitialBalanceCategory() {

        return categoryRepository.findBySid(SystemCategory.INITIAL_BALANCE.getSid())
                .orElseThrow(() -> new IllegalStateException(SYSTEM_CATEGORY_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> fetchAllCategories(UUID userSid) {

        final List<Category> categories = categoryRepository.findAllByUserSid(userSid);

        final boolean hasSystemCategories = categories.stream()
                .anyMatch(c -> Objects.isNull(c.getUserSid()));

        if(!hasSystemCategories) {
            throw new IllegalStateException(SYSTEM_CATEGORIES_NOT_FOUND);
        }

        return categories;
    }

}
