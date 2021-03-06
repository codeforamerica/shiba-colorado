package org.codeforamerica.shiba.output.caf;

import static org.assertj.core.api.Assertions.assertThat;

import org.codeforamerica.shiba.Money;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NonHourlyJobIncomeInformationTest {

  @ParameterizedTest
  @CsvSource(value = {
      "EVERY_WEEK,4",
      "EVERY_TWO_WEEKS,2",
      "TWICE_A_MONTH,2",
      "EVERY_MONTH,1",
      "IT_VARIES,1"
  })
  void shouldCalculateGrossMonthlyIncome(String payPeriod, String income) {
    assertThat(new NonHourlyJobIncomeInformation(payPeriod, "1", 0, null).grossMonthlyIncome())
        .isEqualTo(Money.parse(income));
  }
}
