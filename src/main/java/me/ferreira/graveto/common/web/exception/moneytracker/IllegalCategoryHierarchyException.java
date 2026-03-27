package me.ferreira.graveto.common.web.exception.moneytracker;

public class IllegalCategoryHierarchyException extends RuntimeException {
    public IllegalCategoryHierarchyException() {
        super("Cannot use another user's category as a parent.");
    }
}
