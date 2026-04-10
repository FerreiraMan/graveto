package me.ferreira.graveto.moneytracker.transactions.repository;

import me.ferreira.graveto.moneytracker.accounts.domain.Account_;
import me.ferreira.graveto.moneytracker.categories.domain.Category_;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.domain.Transaction_;
import me.ferreira.graveto.moneytracker.transactions.service.command.FindAllTransactionsCommand;
import org.springframework.data.jpa.domain.PredicateSpecification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

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

    public static PredicateSpecification<Transaction> hasCategory(final UUID categorySid) {

        if (categorySid == null) {
            return PredicateSpecification.unrestricted();
        }

        return (from, builder) ->
                builder.equal(from.join(Transaction_.category).get(Category_.sid), categorySid
         );
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

    public static PredicateSpecification<Transaction> buildFromCommand(final FindAllTransactionsCommand command) {

        return isFromAccount(command.accountSid())
            .and(hasCategory(command.categorySid()))
            .and(withinDateRange(command.startDate(), command.endDate()))
            .and(ofType(command.type()));
    }

}
