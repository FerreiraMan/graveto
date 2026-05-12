package me.ferreira.graveto.moneytracker.analytics.service;

import me.ferreira.graveto.moneytracker.analytics.service.command.CashFlowCommand;
import me.ferreira.graveto.moneytracker.analytics.service.command.CategorySpendingCommand;
import me.ferreira.graveto.moneytracker.analytics.service.payload.CashFlowResult;
import me.ferreira.graveto.moneytracker.analytics.service.payload.CategorySpendingResult;

public interface AnalyticService {

  CashFlowResult generateCashFlowReport(CashFlowCommand command);

  CategorySpendingResult generateCategorySpendingReport(CategorySpendingCommand command);

}
