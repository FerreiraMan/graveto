package me.ferreira.graveto.moneytracker.analytics.service;

import me.ferreira.graveto.moneytracker.analytics.service.command.CashFlowCommand;
import me.ferreira.graveto.moneytracker.analytics.service.payload.CashFlowResult;

public interface AnalyticService {

    CashFlowResult generateCashFlowReport(CashFlowCommand command);

}
