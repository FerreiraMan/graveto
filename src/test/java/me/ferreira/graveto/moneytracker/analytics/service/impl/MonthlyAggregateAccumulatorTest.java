package me.ferreira.graveto.moneytracker.analytics.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import me.ferreira.graveto.moneytracker.transactions.domain.TransactionType;
import me.ferreira.graveto.moneytracker.transactions.domain.projection.MonthlyAggregateProjection;
import org.junit.jupiter.api.Test;

class MonthlyAggregateAccumulatorTest {

  @Test
  void shouldSumIncomeAndExpensePerYearRegardlessOfTargetYear() {
    final List<MonthlyAggregateProjection> projections = List.of(
        projection(2024, 1, TransactionType.INCOME, "100.00"),
        projection(2024, 2, TransactionType.INCOME, "50.00"),
        projection(2025, 1, TransactionType.EXPENSE, "30.00")
    );

    final MonthlyAggregateAccumulator accumulator = new MonthlyAggregateAccumulator(projections, 2025);

    assertThat(accumulator.incomeForYear(2024)).isEqualByComparingTo("150.00");
    assertThat(accumulator.expenseForYear(2025)).isEqualByComparingTo("30.00");
    assertThat(accumulator.incomeForYear(2023)).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldOnlyPopulateMonthlyBucketsForTheTargetYear() {
    final List<MonthlyAggregateProjection> projections = List.of(
        projection(2024, 1, TransactionType.INCOME, "100.00"),
        projection(2025, 1, TransactionType.INCOME, "999.00")
    );

    final MonthlyAggregateAccumulator accumulator = new MonthlyAggregateAccumulator(projections, 2024);

    assertThat(accumulator.incomeForMonth(1)).isEqualByComparingTo("100.00");
    assertThat(accumulator.incomeForYear(2025)).isEqualByComparingTo("999.00");
  }

  @Test
  void shouldTrackTransfersInAndOutSeparatelyFromIncomeAndExpense() {
    final List<MonthlyAggregateProjection> projections = List.of(
        projection(2026, 1, TransactionType.TRANSFER_IN, "500.00"),
        projection(2026, 3, TransactionType.TRANSFER_OUT, "200.00")
    );

    final MonthlyAggregateAccumulator accumulator = new MonthlyAggregateAccumulator(projections, 2026);

    assertThat(accumulator.transfersInForYear(2026)).isEqualByComparingTo("500.00");
    assertThat(accumulator.transfersOutForYear(2026)).isEqualByComparingTo("200.00");
    assertThat(accumulator.transfersInForMonth(1)).isEqualByComparingTo("500.00");
    assertThat(accumulator.transfersOutForMonth(3)).isEqualByComparingTo("200.00");

    assertThat(accumulator.incomeForYear(2026)).isEqualByComparingTo("0.00");
    assertThat(accumulator.expenseForYear(2026)).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldIgnoreOpeningBalanceProjections() {
    final List<MonthlyAggregateProjection> projections = List.of(
        projection(2026, 1, TransactionType.OPENING_BALANCE, "1000.00")
    );

    final MonthlyAggregateAccumulator accumulator = new MonthlyAggregateAccumulator(projections, 2026);

    assertThat(accumulator.incomeForYear(2026)).isEqualByComparingTo("0.00");
    assertThat(accumulator.expenseForYear(2026)).isEqualByComparingTo("0.00");
    assertThat(accumulator.transfersInForYear(2026)).isEqualByComparingTo("0.00");
    assertThat(accumulator.transfersOutForYear(2026)).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldReturnZeroForYearsAndMonthsWithNoRecordedActivity() {
    final MonthlyAggregateAccumulator accumulator = new MonthlyAggregateAccumulator(List.of(), 2026);

    assertThat(accumulator.incomeForYear(2026)).isEqualByComparingTo("0.00");
    assertThat(accumulator.expenseForMonth(5)).isEqualByComparingTo("0.00");
    assertThat(accumulator.transfersInForYear(2026)).isEqualByComparingTo("0.00");
    assertThat(accumulator.transfersOutForMonth(5)).isEqualByComparingTo("0.00");
  }

  private static MonthlyAggregateProjection projection(final int year, final int month,
                                                        final TransactionType type, final String amount) {
    return new MockProjection(year, month, type, new BigDecimal(amount));
  }

  private record MockProjection(int year, int month, TransactionType type,
                                BigDecimal amount) implements MonthlyAggregateProjection {
    @Override
    public int getYear() {
      return year;
    }

    @Override
    public int getMonth() {
      return month;
    }

    @Override
    public TransactionType getType() {
      return type;
    }

    @Override
    public BigDecimal getTotalAmount() {
      return amount;
    }
  }

}
