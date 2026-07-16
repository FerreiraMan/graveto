package me.ferreira.graveto.common.domain;

public enum RecurringOperationStatus {
  ACTIVE(true, false),
  COMPLETED(false, true),
  PAUSED(true, false),
  CANCELED(false, true);

  private final boolean isValidUpdate;
  private final boolean isTerminalState;

  RecurringOperationStatus(final boolean isValidUpdate, final boolean isTerminalState) {
    this.isValidUpdate = isValidUpdate;
    this.isTerminalState = isTerminalState;
  }

  public boolean isTerminal() {
    return this.isTerminalState;
  }

  public boolean canBeUpdated(final RecurringOperationStatus targetStatus) {
    return targetStatus.isValidUpdate && !this.isTerminalState;
  }

}
