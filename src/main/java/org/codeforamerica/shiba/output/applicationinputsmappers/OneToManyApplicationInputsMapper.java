package org.codeforamerica.shiba.output.applicationinputsmappers;

import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.HOME_EXPENSES;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.UNEARNED_INCOME;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.UNEARNED_INCOME_CCAP;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.UTILITY_PAYMENTS;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getValues;
import static org.codeforamerica.shiba.output.ApplicationInputType.ENUMERATED_SINGLE_VALUE;

import java.util.ArrayList;
import java.util.List;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field;
import org.codeforamerica.shiba.output.ApplicationInput;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.pages.data.PagesData;
import org.springframework.stereotype.Component;

/**
 * Handle checkbox selections that correspond to individual inputs on the presented application.
 */
@Component
public class OneToManyApplicationInputsMapper implements ApplicationInputsMapper {

  private static final List<String> UNEARNED_INCOME_OPTIONS = List.of("SOCIAL_SECURITY", "SSI",
      "VETERANS_BENEFITS",
      "UNEMPLOYMENT", "WORKERS_COMPENSATION", "RETIREMENT", "CHILD_OR_SPOUSAL_SUPPORT",
      "TRIBAL_PAYMENTS");

  private static final List<String> UNEARNED_INCOME_CCAP_OPTIONS = List.of("BENEFITS",
      "INSURANCE_PAYMENTS", "CONTRACT_FOR_DEED", "TRUST_MONEY", "HEALTH_CARE_REIMBURSEMENT",
      "INTEREST_DIVIDENDS", "OTHER_SOURCES");

  private static final List<String> HOME_EXPENSES_OPTIONS = List.of("RENT", "MORTGAGE",
      "HOMEOWNERS_INSURANCE", "REAL_ESTATE_TAXES", "ASSOCIATION_FEES", "ROOM_AND_BOARD");

  private static final List<String> UTILITY_PAYMENTS_OPTIONS = List.of("ELECTRICITY", "PHONE",
      "GARBAGE_REMOVAL", "COOKING_FUEL");

  @Override

  public List<ApplicationInput> map(Application application, Document document, Recipient recipient,
      SubworkflowIterationScopeTracker scopeTracker) {
    return map(application.getApplicationData().getPagesData());
  }

  private List<ApplicationInput> map(PagesData pagesData) {
    List<ApplicationInput> results = new ArrayList<>();

    addApplicationInputs(pagesData, "unearnedIncome", results, UNEARNED_INCOME,
        UNEARNED_INCOME_OPTIONS);

    addApplicationInputs(pagesData, "unearnedIncomeCcap", results, UNEARNED_INCOME_CCAP,
        UNEARNED_INCOME_CCAP_OPTIONS);

    addApplicationInputs(pagesData, "homeExpenses", results, HOME_EXPENSES,
        HOME_EXPENSES_OPTIONS);

    addApplicationInputs(pagesData, "utilityPayments", results, UTILITY_PAYMENTS,
        UTILITY_PAYMENTS_OPTIONS);

    return results;
  }

  private void addApplicationInputs(PagesData pagesData, String pageName,
      List<ApplicationInput> results, Field field, List<String> options) {
    if (pagesData.containsKey(pageName)) {
      List<String> selectedValues = getValues(pagesData, field);
      options.stream()
          .map(option -> new ApplicationInput(pageName,
              option,
              String.valueOf(selectedValues.contains(option)),
              ENUMERATED_SINGLE_VALUE))
          .forEach(results::add);
    }
  }
}