package me.ferreira.graveto.moneytracker.categories.service.impl;

import lombok.AllArgsConstructor;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.domain.SystemCategory;
import me.ferreira.graveto.moneytracker.categories.repository.CategoryRepository;
import me.ferreira.graveto.moneytracker.categories.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    public static final String SYSTEM_CATEGORY_NOT_FOUND = "System category is missing from the database.";

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public Category getInitialBalanceCategory() {

        return categoryRepository.findBySid(SystemCategory.INITIAL_BALANCE)
                .orElseThrow(() -> new IllegalStateException(SYSTEM_CATEGORY_NOT_FOUND));
    }
}
