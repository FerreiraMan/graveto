package me.ferreira.graveto.moneytracker.transactions.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;
import me.ferreira.graveto.moneytracker.accounts.domain.Account_;
import me.ferreira.graveto.moneytracker.categories.domain.Category_;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionStatus;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction_;
import org.springframework.data.jpa.domain.PredicateSpecification;

public class TransactionsSpecs {

  public static final String MISSING_ACCOUNT_SID = "Account SID is strictly required to view transactions";

  public static PredicateSpecification<Transaction> isFromAccount(final UUID accountSid) {

    if (accountSid == null) {
      throw new IllegalArgumentException(MISSING_ACCOUNT_SID);
    }

    return (from, builder) ->
        builder.equal(from.join(Transaction_.account).get(Account_.sid), accountSid
        );
  }

  public static PredicateSpecification<Transaction> hasCategoryIn(final Set<Long> categoryIds) {

    if (categoryIds == null) {
      return PredicateSpecification.unrestricted();
    }

    return (from, builder) ->
        from.get(Transaction_.category).get(Category_.id).in(categoryIds);
  }

  public static PredicateSpecification<Transaction> withinDateRange(
      final LocalDate startingDate, final LocalDate endingDate) {

    return (from, builder) -> {

      PredicateSpecification<Transaction> spec = PredicateSpecification.unrestricted();

      if (startingDate != null) {

        final LocalDateTime startOfDayOnDate = startingDate.atStartOfDay();
        spec = spec.and((f, b) -> b.greaterThanOrEqualTo(f.get(Transaction_.occurredAt), startOfDayOnDate));
      }
      if (endingDate != null) {

        final LocalDateTime endOfDayOnDate = endingDate.atTime(LocalTime.MAX);
        spec = spec.and((f, b) -> b.lessThanOrEqualTo(f.get(Transaction_.occurredAt), endOfDayOnDate));
      }

      return spec.toPredicate(from, builder);
    };
  }

  public static PredicateSpecification<Transaction> ofType(final TransactionType transactionType) {

    if (transactionType == null) {
      return PredicateSpecification.unrestricted();
    }

    return (from, builder) ->
        builder.equal(from.get(Transaction_.type), transactionType
        );
  }

  public static PredicateSpecification<Transaction> hasStatus(final TransactionStatus transactionStatus) {

    final TransactionStatus targetStatus = (transactionStatus == null) ? TransactionStatus.ACTIVE : transactionStatus;

    return (from, builder) ->
        builder.equal(from.get(Transaction_.status), targetStatus
        );
  }

  public static PredicateSpecification<Transaction> buildPredicate(final TransactionSearchCriteria searchCriteria) {

    return isFromAccount(searchCriteria.accountSid())
        .and(hasStatus(searchCriteria.status()))
        .and(hasCategoryIn(searchCriteria.categoryIds()))
        .and(withinDateRange(searchCriteria.startDate(), searchCriteria.endDate()))
        .and(ofType(searchCriteria.type()));
  }

}
