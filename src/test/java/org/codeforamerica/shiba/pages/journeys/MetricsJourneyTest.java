package org.codeforamerica.shiba.pages.journeys;

import org.codeforamerica.shiba.pages.MetricsPage;
import org.codeforamerica.shiba.pages.Sentiment;
import org.codeforamerica.shiba.pages.SuccessPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.test.context.jdbc.Sql;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

@Tag("ccap")
@Sql(statements = "TRUNCATE TABLE applications;")
public class MetricsJourneyTest extends JourneyTest {

    @Override
    @BeforeEach
    protected void setUp() throws IOException { super.setUp(); }

    @Test
    void userCanCompleteTheNonExpeditedFlowAndCanDownloadPdfsAndShibaShouldCaptureMetricsAfterApplicationIsCompleted() {
        when(clock.instant()).thenReturn(
                LocalDateTime.of(2020, 1, 1, 10, 10).atOffset(ZoneOffset.UTC).toInstant(),
                LocalDateTime.of(2020, 1, 1, 10, 15, 30).atOffset(ZoneOffset.UTC).toInstant()
        );

        SuccessPage successPage = nonExpeditedFlowToSuccessPage(false, true, smartyStreetClient);

        // Downloading PDfs
        successPage.downloadPdfs();
        await().until(this::allPdfsHaveBeenDownloaded);

        // Submitting Feedback
        successPage.chooseSentiment(Sentiment.HAPPY);
        successPage.submitFeedback();
        driver.navigate().to(baseUrlWithAuth + "/metrics");
        MetricsPage metricsPage = new MetricsPage(driver);
        assertThat(metricsPage.getCardValue("Applications Submitted")).isEqualTo("1");
        assertThat(metricsPage.getCardValue("Median All Time")).contains("05m 30s");
        assertThat(metricsPage.getCardValue("Median Week to Date")).contains("05m 30s");
        assertThat(metricsPage.getCardValue("Average Week to Date")).contains("05m 30s");
        // When adding new counties, this TD will be equal to the first county in the list
        assertThat(driver.findElements(By.tagName("td")).get(0).getText()).isEqualTo("Anoka");
        assertThat(driver.findElements(By.tagName("td")).get(1).getText()).isEqualTo("0");
        assertThat(driver.findElements(By.tagName("td")).get(2).getText()).isEqualTo("0");
        assertThat(metricsPage.getCardValue("Happy")).contains("100%");
    }
}
