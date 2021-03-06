package org.codeforamerica.shiba.output.caf;

import lombok.Value;
import org.codeforamerica.shiba.Money;
import org.codeforamerica.shiba.pages.data.Iteration;

@Value
public class LastThirtyDaysJobIncomeInformation implements JobIncomeInformation {

  Money lastThirtyDaysIncome;
  int indexInJobsSubworkflow;
  Iteration iteration;

  public LastThirtyDaysJobIncomeInformation(String lastThirtyDaysIncome, int indexInJobsSubworkflow,
      Iteration iteration) {
    this.lastThirtyDaysIncome =
        lastThirtyDaysIncome.isBlank() ? Money.ZERO : Money.parse(lastThirtyDaysIncome);
    this.indexInJobsSubworkflow = indexInJobsSubworkflow;
    this.iteration = iteration;
  }

  @Override
  public boolean isComplete() {
    return lastThirtyDaysIncome != null;
  }

  @Override
  public Money grossMonthlyIncome() {
    return lastThirtyDaysIncome;
  }
}
