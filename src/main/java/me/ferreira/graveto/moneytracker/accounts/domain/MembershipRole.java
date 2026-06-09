package me.ferreira.graveto.moneytracker.accounts.domain;

public enum MembershipRole {

  OWNER(true, true, true, true, true, true, true, true),
  CONTRIBUTOR(false, false, false, true, true, true, true, true);

  private final boolean canCloseAccount;
  private final boolean canDeleteAccount;
  private final boolean canAddMemberToAccount;

  private final boolean canReadTransaction;
  private final boolean canCreateTransaction;
  private final boolean canDeleteTransaction;
  private final boolean canUpdateTransaction;

  private final boolean canRequestReport;

  MembershipRole(final boolean canCloseAccount,
                 final boolean canDeleteAccount,
                 final boolean canAddMemberToAccount,
                 final boolean canReadTransaction,
                 final boolean canCreateTransaction,
                 final boolean canDeleteTransaction,
                 final boolean canUpdateTransaction,
                 final boolean canRequestReport) {

    this.canCloseAccount = canCloseAccount;
    this.canDeleteAccount = canDeleteAccount;
    this.canAddMemberToAccount = canAddMemberToAccount;
    this.canReadTransaction = canReadTransaction;
    this.canCreateTransaction = canCreateTransaction;
    this.canDeleteTransaction = canDeleteTransaction;
    this.canUpdateTransaction = canUpdateTransaction;
    this.canRequestReport = canRequestReport;
  }

  public boolean canDeleteAccount() {
    return this.canDeleteAccount;
  }

  public boolean canCloseAccount() {
    return this.canCloseAccount;
  }

  public boolean canAddMemberToAccount() {
    return this.canAddMemberToAccount;
  }

  public boolean canReadTransaction() {
    return this.canReadTransaction;
  }

  public boolean canCreateTransaction() {
    return this.canCreateTransaction;
  }

  public boolean canDeleteTransaction() {
    return this.canDeleteTransaction;
  }

  public boolean canUpdateTransaction() {
    return this.canUpdateTransaction;
  }

  public boolean canRequestReport() {
    return this.canRequestReport;
  }

}
