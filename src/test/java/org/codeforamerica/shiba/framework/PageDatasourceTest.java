package org.codeforamerica.shiba.framework;

import static java.util.Locale.ENGLISH;
import static org.assertj.core.api.Assertions.assertThat;

import org.codeforamerica.shiba.testutilities.AbstractFrameworkTest;
import org.codeforamerica.shiba.testutilities.YesNoAnswer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"pagesConfig=pages-config/test-page-datasources.yaml"})
public class PageDatasourceTest extends AbstractFrameworkTest {

  private final String yesHeaderText = "yes header text";
  private final String noHeaderText = "no header text";
  private final String noAnswerTitle = "no answer title";
  private final String yesAnswerTitle = "yes answer title";

  @Override
  @BeforeEach
  protected void setUp() throws Exception {
    super.setUp();
    staticMessageSource.addMessage("first-page-title", ENGLISH, "firstPageTitle");
    staticMessageSource.addMessage("static-page-with-datasource-inputs-title", ENGLISH,
        "staticPageWithDatasourceInputsTitle");
    staticMessageSource.addMessage("yes-header-text", ENGLISH, yesHeaderText);
    staticMessageSource.addMessage("no-header-text", ENGLISH, noHeaderText);
    staticMessageSource
        .addMessage("general.inputs.yes", ENGLISH, YesNoAnswer.YES.getDisplayValue());
    staticMessageSource.addMessage("general.inputs.no", ENGLISH, YesNoAnswer.NO.getDisplayValue());
    staticMessageSource.addMessage("some-other-header", ENGLISH, "some other header");
    staticMessageSource.addMessage("some-header", ENGLISH, "some other header");
    staticMessageSource.addMessage("no-answer-title", ENGLISH, noAnswerTitle);
    staticMessageSource.addMessage("radio-value-key-1", ENGLISH, "radio value 1");
    staticMessageSource.addMessage("radio-value-key-2", ENGLISH, "radio value 2");
    staticMessageSource.addMessage("foo", ENGLISH, "wrong title");
    staticMessageSource.addMessage("no-answer-title", ENGLISH, noAnswerTitle);
    staticMessageSource.addMessage("yes-answer-title", ENGLISH, yesAnswerTitle);
  }

  @Test
  void shouldDisplayDataEnteredFromAPreviousPage() throws Exception {
    var inputText = "some input";
    var nextPage = postAndFollowRedirect("employersName", "employersName", inputText);
    assertThat(nextPage.getElementTextById("employersName")).isEqualTo(inputText);
  }

  @Test
  void shouldDisplayPageTitleBasedOnCondition() throws Exception {
    postExpectingNextPageTitle("yesNoQuestionPage", "yesNoQuestion", "false", noAnswerTitle);
  }

  @Test
  void shouldDisplayPageHeaderBasedOnCondition() throws Exception {
    var page = postAndFollowRedirect("yesNoQuestionPage", "yesNoQuestion", "true");
    assertThat(page.getTitle()).isEqualTo(yesAnswerTitle);
    assertThat(page.getElementByCssSelector("h1").text()).isEqualTo(yesHeaderText);
  }

  @Test
  void shouldDisplayPageHeaderBasedOnCompositeCondition() throws Exception {
    postExpectingRedirect("yesNoQuestionPage2", "yesNoQuestion2", "true", "yesNoQuestionPage3");
    var page = postAndFollowRedirect("yesNoQuestionPage3", "yesNoQuestion3", "false");
    assertThat(page.getElementByCssSelector("h1").text()).isEqualTo(yesHeaderText);
  }

  @Test
  void shouldDisplayPageHeaderBasedOnCompositeConditionOtherPath() throws Exception {
    postExpectingRedirect("yesNoQuestionPage2", "yesNoQuestion2", "false", "yesNoQuestionPage3");
    var page = postAndFollowRedirect("yesNoQuestionPage3", "yesNoQuestion3", "true");
    assertThat(page.getElementByCssSelector("h1").text()).isEqualTo(noHeaderText);
  }

  @Test
  void shouldDisplayDatasourceForFormPages() throws Exception {
    var value = "some input value";
    postExpectingSuccess("employersName", "employersName", value);
    var page = getFormPage("testFormPage");
    assertThat(page.getElementsByClassName("job-context-employer-name").get(0).text())
        .isEqualTo(value);
  }

  @Test
  void shouldGetDataFromDatasourcesOutsideOfSubworkflow() throws Exception {
    var page = postAndFollowRedirect("outsideSubworkflowPage", "outside-subworkflow-input", "true");
    assertThat(page.getElementByCssSelector("h1").text()).isEqualTo(yesHeaderText);
  }

  @Test
  void shouldHandleMissingDatasourcePagesWhenDatasourcePageWasSkipped() throws Exception {
    var page = postAndFollowRedirect("outsideSubworkflowPage", "outside-subworkflow-input", "true");
    assertThat(page.getElementByCssSelector("h1").text()).isEqualTo(yesHeaderText);
    assertThat(page.getTitle()).isEqualTo(noAnswerTitle);
  }
}
