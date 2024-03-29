package org.codeforamerica.shiba;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;
import java.util.Map;
import org.codeforamerica.shiba.testutilities.AbstractShibaMockMvcTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("ccap")
public class CCAPMockMvcTest extends AbstractShibaMockMvcTest {

  @BeforeEach
  protected void setUp() throws Exception {
    super.setUp();
    mockMvc.perform(get("/pages/identifyCountyBeforeApplying").session(session)); // start timer
    postExpectingSuccess("identifyCountyBeforeApplying", "county", "Hennepin");
    postExpectingSuccess("languagePreferences",
        Map.of("writtenLanguage", List.of("ENGLISH"), "spokenLanguage", List.of("ENGLISH"))
    );
  }

  @Test
  void verifyFlowWhenNoOneHasSelectedCCAPInHousehold() throws Exception {
    completeFlowFromLandingPageThroughReviewInfo("SNAP");
    postExpectingRedirect("addHouseholdMembers", "addHouseholdMembers", "true", "startHousehold");
    assertNavigationRedirectsToCorrectNextPage("startHousehold", "householdMemberInfo");
    fillOutHousemateInfo("EA");
    finishAddingHouseholdMembers("preparingMealsTogether");
    postExpectingNextPageTitle("preparingMealsTogether", "isPreparingMealsTogether", "false",
        "Going to school");
    postExpectingNextPageTitle("goingToSchool", "goingToSchool", "true", "Pregnant");
    completeFlowFromIsPregnantThroughTribalNations(true);
    assertNavigationRedirectsToCorrectNextPage("introIncome", "employmentStatus");
    postExpectingNextPageTitle("employmentStatus", "areYouWorking", "false", "Income Up Next");
    assertNavigationRedirectsToCorrectNextPage("incomeUpNext", "unearnedIncome");
    postExpectingRedirect("unearnedIncome", "unearnedIncome", "NO_UNEARNED_INCOME_SELECTED",
        "additionalIncomeInfo");
    fillAdditionalIncomeInfoToHaveVehicle();
    assertNavigationRedirectsToCorrectNextPage("vehicle", "investments");
    postExpectingRedirect("investments", "haveInvestments", "false", "savings");
    postExpectingRedirect("savings", "haveSavings", "true", "savingsAmount");
    postExpectingNextPageTitle("savingsAmount", "liquidAssets", "1234", "Sold assets");
    assertPageDoesNotHaveElementWithId("legalStuff", "ccap-legal");
  }

  @Test
  void verifyFlowWhenLiveAloneApplicantHasNotSelectedCCAP() throws Exception {
    completeFlowFromLandingPageThroughReviewInfo("SNAP");
    postExpectingRedirect("addHouseholdMembers", "addHouseholdMembers", "false",
        "introPersonalDetails");
    postExpectingRedirect("livingSituation", "goingToSchool");
    postExpectingRedirect("goingToSchool", "goingToSchool", "true", "pregnant");
    completeFlowFromIsPregnantThroughTribalNations(false);
    assertNavigationRedirectsToCorrectNextPage("introIncome", "employmentStatus");
    postExpectingNextPageTitle("employmentStatus", "areYouWorking", "false", "Income Up Next");
    assertNavigationRedirectsToCorrectNextPage("incomeUpNext", "unearnedIncome");
    postExpectingRedirect("unearnedIncome", "unearnedIncome", "NO_UNEARNED_INCOME_SELECTED",
        "additionalIncomeInfo");
    fillAdditionalIncomeInfoToHaveVehicle();
    assertNavigationRedirectsToCorrectNextPage("vehicle", "investments");
    postExpectingRedirect("investments", "haveInvestments", "true", "savings");
    postExpectingRedirect("savings", "haveSavings", "true", "savingsAmount");
    postExpectingNextPageTitle("savingsAmount", "liquidAssets", "1234", "Sold assets");
    assertPageDoesNotHaveElementWithId("legalStuff", "ccap-legal");
  }

  @Test
  void verifyFlowWhenLiveAloneApplicantSelectedCCAP() throws Exception {
    // Applicant lives alone and choose CCAP
    completeFlowFromLandingPageThroughReviewInfo("CCAP");
    postExpectingRedirect("addHouseholdMembers", "addHouseholdMembers", "false",
        "introPersonalDetails");
    postExpectingRedirect("livingSituation", "goingToSchool");
    postExpectingRedirect("goingToSchool", "goingToSchool", "true", "pregnant");
    postExpectingRedirect("pregnant", "isPregnant", "true", "migrantFarmWorker");
    postExpectingRedirect("migrantFarmWorker", "migrantOrSeasonalFarmWorker", "true", "usCitizen");
    postExpectingRedirect("usCitizen", "isUsCitizen", "true", "disability");
    postExpectingRedirect("disability", "hasDisability", "true", "workSituation");
    postExpectingRedirect("workSituation", "hasWorkSituation", "true", "introIncome");
    assertNavigationRedirectsToCorrectNextPage("introIncome", "employmentStatus");
    postExpectingRedirect("employmentStatus", "areYouWorking", "false", "jobSearch");
    postExpectingRedirect("jobSearch", "currentlyLookingForJob", "true", "incomeUpNext");
    fillUnearnedIncomeToLegalStuffCCAP();
  }

  @Test
  void verifyFlowWhenApplicantSelectedCCAPAndHouseholdMemberDidNot() throws Exception {
    // Applicant selected CCAP for themselves and did not choose CCAP for household member
    completeFlowFromLandingPageThroughReviewInfo("CCAP");
    postExpectingRedirect("addHouseholdMembers", "addHouseholdMembers", "true", "startHousehold");
    assertNavigationRedirectsToCorrectNextPage("startHousehold", "householdMemberInfo");
    fillOutHousemateInfo("EA");
    finishAddingHouseholdMembers("childrenInNeedOfCare");
    assertCorrectPageTitle("childrenInNeedOfCare", "Who are the children in need of care?");
    postExpectingRedirect("childrenInNeedOfCare", "livingSituation");
    postExpectingRedirect("livingSituation", "goingToSchool");
    postExpectingNextPageTitle("goingToSchool", "goingToSchool", "true", "Who is going to school?");
    completeFlowFromIsPregnantThroughTribalNations(true);
    assertNavigationRedirectsToCorrectNextPage("introIncome", "employmentStatus");
    postExpectingNextPageTitle("employmentStatus", "areYouWorking", "false", "Job Search");
    postExpectingNextPageTitle("jobSearch", "currentlyLookingForJob", "true",
        "Who is looking for a job");
    fillUnearnedIncomeToLegalStuffCCAP();
  }

  @Test
  void verifyFlowWhenOnlyHouseholdMemberSelectedCCAP() throws Exception {
    selectPrograms("NONE");
    assertPageHasWarningMessage("choosePrograms",
        "You will be asked to share some information about yourself even though you’re only applying for others.");

    completeFlowFromLandingPageThroughReviewInfo("NONE");
    postExpectingRedirect("addHouseholdMembers", "addHouseholdMembers", "true", "startHousehold");
    assertNavigationRedirectsToCorrectNextPage("startHousehold", "householdMemberInfo");
    fillOutHousemateInfo("CCAP");

    // Don't select any children in need of care, should get redirected to preparing meals together
    assertCorrectPageTitle("childrenInNeedOfCare", "Who are the children in need of care?");
    postExpectingNextPageTitle("childrenInNeedOfCare", "Living situation");

    // Go back to childrenInNeedOfCare and select someone this time, but don't select anyone having a parent not at home
    String householdMemberId = getFirstHouseholdMemberId();
    postExpectingNextPageTitle("childrenInNeedOfCare",
        "whoNeedsChildCare",
        List.of("defaultFirstName defaultLastName applicant",
            "householdMemberFirstName householdMemberLastName" + householdMemberId),
        "Who are the children that have a parent not living in the home?"
    );
    postExpectingNextPageTitle("whoHasParentNotAtHome",
        "whoHasAParentNotLivingAtHome",
        List.of("NONE_OF_THE_ABOVE"),
        "Living situation");

    // Go back and select someone having a parent not at home
    postExpectingNextPageTitle("whoHasParentNotAtHome",
        "whoHasAParentNotLivingAtHome",
        List.of("defaultFirstName defaultLastName applicant"),
        "Name of parent outside home");
    postExpectingNextPageTitle("parentNotAtHomeNames",
        Map.of("whatAreTheParentsNames", List.of("My Parent", "Default's Parent"),
            "childIdMap", List.of("applicant", householdMemberId)
        ),
        "Living situation");

    postExpectingRedirect("preparingMealsTogether", "isPreparingMealsTogether", "false",
        "livingSituation");
    postExpectingRedirect("livingSituation", "livingSituation", "UNKNOWN", "goingToSchool");
    postExpectingNextPageTitle("goingToSchool", "goingToSchool", "true", "Who is going to school?");
    postExpectingRedirect("whoIsGoingToSchool", "pregnant"); // no one is going to school
    completeFlowFromIsPregnantThroughTribalNations(true);
    assertNavigationRedirectsToCorrectNextPage("introIncome", "employmentStatus");
    postExpectingNextPageTitle("employmentStatus", "areYouWorking", "false", "Job Search");
    postExpectingNextPageTitle("jobSearch", "currentlyLookingForJob", "true",
        "Who is looking for a job");
    fillUnearnedIncomeToLegalStuffCCAP();
  }

  private void fillUnearnedIncomeToLegalStuffCCAP() throws Exception {
    assertNavigationRedirectsToCorrectNextPage("incomeUpNext", "unearnedIncome");
    postExpectingRedirect("unearnedIncome", "unearnedIncome", "NO_UNEARNED_INCOME_SELECTED",
        "unearnedIncomeCcap");
    postExpectingRedirect("unearnedIncomeCcap",
        "unearnedIncomeCcap",
        "NO_UNEARNED_INCOME_CCAP_SELECTED",
        "additionalIncomeInfo");
    fillAdditionalIncomeInfoToHaveVehicle();
    assertNavigationRedirectsToCorrectNextPage("vehicle", "realEstate");
    postExpectingRedirect("realEstate", "ownRealEstate", "false", "investments");
    postExpectingRedirect("investments", "haveInvestments", "false", "savings");
    postExpectingRedirect("savings", "haveSavings", "false", "soldAssets");
    // Go back and enter true for savings
    postExpectingRedirect("savings", "haveSavings", "true", "savingsAmount");
    postExpectingRedirect("savingsAmount", "liquidAssets", "1234", "millionDollar");
    postExpectingRedirect("millionDollar", "haveMillionDollars", "false", "soldAssets");
    assertPageHasElementWithId("legalStuff", "ccap-legal");
  }
}
