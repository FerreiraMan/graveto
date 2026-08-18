package me.ferreira.graveto.moneytracker.transactions.repository.recurringtransfer;

import java.util.UUID;
import me.ferreira.graveto.common.domain.RecurringOperationStatus;
import me.ferreira.graveto.moneytracker.accounts.domain.Account_;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransfer;
import me.ferreira.graveto.moneytracker.transactions.domain.RecurringTransfer_;
import me.ferreira.graveto.moneytracker.transactions.service.command.recurringtransfer.FindAllRecurringTransfersCommand;
import org.springframework.data.jpa.domain.PredicateSpecification;

public class RecurringTransfersSpecs {

  public static PredicateSpecification<RecurringTransfer> buildFromCommand(
      final FindAllRecurringTransfersCommand command) {

    return isFromAccount(command.accountSid())
        .and(hasDestinationAccount(command.destinationAccountSid()))
        .and(hasStatus(command.status()));
  }

  private static PredicateSpecification<RecurringTransfer> isFromAccount(final UUID accountSid) {

    if (accountSid == null) {
      throw new IllegalArgumentException("Account SID is strictly required to view recurring transfers.");
    }

    return (from, builder) ->
        builder.equal(from.join(RecurringTransfer_.sourceAccount).get(Account_.sid), accountSid
        );
  }

  private static PredicateSpecification<RecurringTransfer> hasDestinationAccount(final UUID destinationAccountSid) {

    if (destinationAccountSid == null) {
      return PredicateSpecification.unrestricted();
    }

    return (from, builder) ->
        builder.equal(from.join(RecurringTransfer_.destinationAccount).get(Account_.sid), destinationAccountSid
        );
  }

  private static PredicateSpecification<RecurringTransfer> hasStatus(final RecurringOperationStatus status) {

    if (status == null) {
      return PredicateSpecification.unrestricted();
    }

    return (from, builder) ->
        builder.equal(from.get(RecurringTransfer_.status), status
        );
  }
  
}
