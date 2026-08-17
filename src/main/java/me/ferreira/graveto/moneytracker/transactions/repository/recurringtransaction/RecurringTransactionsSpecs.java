package me.ferreira.graveto.moneytracker.transactions.repository.recurringtransaction;

import java.util.UUID;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.moneytracker.accounts.domain.Account_;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransaction;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransaction_;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransaction.FindAllRecurringTransactionsCommand;
import org.springframework.data.jpa.domain.PredicateSpecification;

public class RecurringTransactionsSpecs {

  public static PredicateSpecification<RecurringTransaction> buildFromCommand(
      final FindAllRecurringTransactionsCommand command) {

    return isFromAccount(command.accountSid())
        .and(hasStatus(command.status()));
  }

  private static PredicateSpecification<RecurringTransaction> isFromAccount(final UUID accountSid) {

    if (accountSid == null) {
      throw new IllegalArgumentException("Account SID is strictly required to view recurring transactions.");
    }

    return (from, builder) ->
        builder.equal(from.join(RecurringTransaction_.account).get(Account_.sid), accountSid
        );
  }

  private static PredicateSpecification<RecurringTransaction> hasStatus(final RecurringOperationStatus status) {

    if (status == null) {
      return PredicateSpecification.unrestricted();
    }

    return (from, builder) ->
        builder.equal(from.get(RecurringTransaction_.status), status
        );
  }

}
