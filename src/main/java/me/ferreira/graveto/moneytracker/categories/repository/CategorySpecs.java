package me.ferreira.graveto.moneytracker.categories.repository;

import java.util.UUID;
import me.ferreira.graveto.moneytracker.categories.domain.Category;
import me.ferreira.graveto.moneytracker.categories.domain.Category_;
import me.ferreira.graveto.moneytracker.categories.service.command.FindAllCategoriesCommand;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import org.springframework.data.jpa.domain.PredicateSpecification;

public class CategorySpecs {

  public static PredicateSpecification<Category> buildFromCommand(
      final FindAllCategoriesCommand command) {

    return isNotInternal()
        .and(hasDisplayName(command.displayName()))
        .and(hasAccountSid(command.accountSid()))
        .and(hasParent(command.parentSid()))
        .and(isOfType(command.type()));
  }

  private static PredicateSpecification<Category> isNotInternal() {

    return (from, builder) ->
        builder.isFalse(from.get(Category_.isInternal)
        );
  }

  private static PredicateSpecification<Category> hasDisplayName(final String displayName) {

    if (displayName == null) {
      return PredicateSpecification.unrestricted();
    }

    return (from, builder) ->
        builder.like(builder.lower(from.get(Category_.displayName)), "%" + displayName.toLowerCase() + "%"
        );
  }

  private static PredicateSpecification<Category> hasAccountSid(final UUID accountSid) {

    if (accountSid == null) {
      return (from, builder) -> builder.isNull(from.get(Category_.accountSid));
    }

    return (from, builder) ->
        builder.or(
            builder.equal(from.get(Category_.accountSid), accountSid),
            builder.isNull(from.get(Category_.accountSid))
        );
  }

  private static PredicateSpecification<Category> hasParent(final UUID parentSid) {

    if (parentSid == null) {
      return PredicateSpecification.unrestricted();
    }

    return (from, builder) ->
        builder.equal(from.join(Category_.parent).get(Category_.sid), parentSid
        );
  }

  private static PredicateSpecification<Category> isOfType(final TransactionType type) {

    if (type == null) {
      return PredicateSpecification.unrestricted();
    }

    return (from, builder) ->
        builder.equal(from.get(Category_.transactionType), type
        );
  }

}
