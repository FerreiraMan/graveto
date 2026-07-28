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

    return isFromUser(command.userSid())
        .and(hasStatus(command.status()))
        .and(hasSourceAccount(command.sourceAccountSid()))
        .and(hasDestinationAccount(command.destinationAccountSid()));
  }

  private static PredicateSpecification<RecurringTransfer> isFromUser(final UUID userSid) {

    if (userSid == null) {
      throw new IllegalArgumentException("User SID is strictly required to view recurring transfers.");
    }

    return (from, builder) ->
        builder.equal(from.get(RecurringTransfer_.userSid), userSid
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

  private static PredicateSpecification<RecurringTransfer> hasSourceAccount(final UUID sourceAccountSid) {

    if (sourceAccountSid == null) {
      return PredicateSpecification.unrestricted();
    }

    return (from, builder) ->
        builder.equal(from.join(RecurringTransfer_.sourceAccount).get(Account_.sid), sourceAccountSid
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
  
}
