package org.codeforamerica.shiba.output;

import de.redsix.pdfcompare.PdfComparator;
import org.codeforamerica.shiba.*;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.application.ApplicationRepository;
import org.codeforamerica.shiba.documents.DocumentRepositoryService;
import org.codeforamerica.shiba.mnit.MnitEsbWebServiceClient;
import org.codeforamerica.shiba.output.caf.FileNameGenerator;
import org.codeforamerica.shiba.output.pdf.PdfGenerator;
import org.codeforamerica.shiba.output.xml.XmlGenerator;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.pages.data.InputData;
import org.codeforamerica.shiba.pages.data.PageData;
import org.codeforamerica.shiba.pages.data.PagesData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.codeforamerica.shiba.TestUtils.getAbsoluteFilepath;
import static org.codeforamerica.shiba.application.Status.DELIVERY_FAILED;
import static org.codeforamerica.shiba.application.Status.SENDING;
import static org.codeforamerica.shiba.output.Document.*;
import static org.codeforamerica.shiba.output.Recipient.CASEWORKER;
import static org.mockito.Mockito.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = NONE)
@ContextConfiguration(classes = {NonSessionScopedApplicationData.class})
@Tag("db")
class MnitDocumentConsumerTest {
    @MockBean
    private MnitEsbWebServiceClient mnitClient;
    @MockBean
    private XmlGenerator xmlGenerator;

    @MockBean
    private MonitoringService monitoringService;
    @MockBean
    private DocumentRepositoryService documentRepositoryService;
    @MockBean
    private FileNameGenerator fileNameGenerator;
    @MockBean
    private ApplicationRepository applicationRepository;

    @SpyBean
    private PdfGenerator pdfGenerator;

    @MockBean
    private MessageSource messageSource;

    @Autowired
    private ApplicationData applicationData;
    @Autowired
    private MnitDocumentConsumer documentConsumer;

    private Application application;

    @MockBean
    private Clock clock;

    @MockBean
    private ApplicationStatusUpdater applicationStatusUpdater;


    @BeforeEach
    void setUp() throws ParseException {
        PagesData pagesData = new PagesDataBuilder().build(List.of(
                new PageDataBuilder("personalInfo", Map.of(
                        "firstName", List.of("Jane"),
                        "lastName", List.of("Doe"),
                        "otherName", List.of(""),
                        "dateOfBirth", List.of("10", "04", "2020"),
                        "ssn", List.of("123-45-6789"),
                        "sex", List.of("FEMALE"),
                        "maritalStatus", List.of("NEVER_MARRIED"),
                        "livedInMnWholeLife", List.of("false"),
                        "moveToMnDate", List.of("11", "03", "2020"),
                        "moveToMnPreviousCity", List.of("")
                )),
                new PageDataBuilder("contactInfo", Map.of(
                        "phoneNumber", List.of("(603) 879-1111"),
                        "email", List.of("jane@example.com"),
                        "phoneOrEmail", List.of("PHONE")
                )),
                new PageDataBuilder("choosePrograms", Map.of(
                        "programs", List.of("SNAP")
                ))
        ));

        DateFormat format = new SimpleDateFormat("M/dd/yyy 'at' HH:mm aaa");
        Date date = format.parse("06/10/2021 at 01:28 PM");
        ZonedDateTime completedAt = ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);

        applicationData.setPagesData(pagesData);
        application = Application.builder()
                .id("someId")
                .completedAt(completedAt)
                .applicationData(applicationData)
                .county(County.Olmsted)
                .timeToComplete(null)
                .build();
        when(messageSource.getMessage(any(), any(), any())).thenReturn("default success message");
        when(fileNameGenerator.generatePdfFileName(any(), any())).thenReturn("some-file.pdf");
        when(clock.instant()).thenReturn(Instant.from(completedAt));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        doReturn(application).when(applicationRepository).find(any());
    }

    @AfterEach
    void afterEach() {
        applicationData.setUploadedDocs(new ArrayList<>());
    }

    @Test
    void generatesThePDFFromTheApplicationData() {
        documentConsumer.process(application);
        verify(pdfGenerator).generate(application.getId(), CAF, CASEWORKER);
    }

    @Test
    void generatesTheXmlFromTheApplicationData() {
        documentConsumer.process(application);
        verify(xmlGenerator).generate(application.getId(), CAF, CASEWORKER);
    }

    @Test
    void sendsTheGeneratedXmlAndPdf() {
        ApplicationFile pdfApplicationFile = new ApplicationFile("my pdf".getBytes(), "someFile.pdf");
        doReturn(pdfApplicationFile).when(pdfGenerator).generate(anyString(), any(), any());
        ApplicationFile xmlApplicationFile = new ApplicationFile("my xml".getBytes(), "someFile.xml");
        when(xmlGenerator.generate(any(), any(), any())).thenReturn(xmlApplicationFile);
        documentConsumer.process(application);
        verify(mnitClient).send(pdfApplicationFile, County.Olmsted, application.getId(), Document.CAF);
        verify(mnitClient).send(xmlApplicationFile, County.Olmsted, application.getId(), Document.CAF);
    }

    @Test
    void sendsTheCcapPdfIfTheApplicationHasCCAP() {
        ApplicationFile pdfApplicationFile = new ApplicationFile("my pdf".getBytes(), "someFile.pdf");
        doReturn(pdfApplicationFile).when(pdfGenerator).generate(anyString(), eq(CCAP), any());

        ApplicationFile xmlApplicationFile = new ApplicationFile("my xml".getBytes(), "someFile.xml");
        when(xmlGenerator.generate(any(), any(), any())).thenReturn(xmlApplicationFile);
        PagesData pagesData = new PagesData();
        PageData chooseProgramsPage = new PageData();
        chooseProgramsPage.put("programs", InputData.builder().value(List.of("CCAP")).build());
        pagesData.put("choosePrograms", chooseProgramsPage);
        applicationData.setPagesData(pagesData);
        documentConsumer.process(application);
        verify(mnitClient).send(pdfApplicationFile, County.Olmsted, application.getId(), Document.CCAP);
    }

    @Test
    void updatesStatusToSendingForCafAndCcapDocuments() {
        ApplicationFile pdfApplicationFile = new ApplicationFile("my pdf".getBytes(), "someFile.pdf");
        doReturn(pdfApplicationFile).when(pdfGenerator).generate(anyString(), eq(CCAP), any());

        PagesData pagesData = new PagesData();
        PageData chooseProgramsPage = new PageData();
        chooseProgramsPage.put("programs", InputData.builder().value(List.of("CCAP", "SNAP")).build());
        pagesData.put("choosePrograms", chooseProgramsPage);
        applicationData.setPagesData(pagesData);
        documentConsumer.process(application);
        verify(applicationStatusUpdater).updateCafApplicationStatus(application.getId(), CAF, SENDING);
        verify(applicationStatusUpdater).updateCcapApplicationStatus(application.getId(), CCAP, SENDING);
    }

    @Test
    void updatesStatusToDeliveryFailedForDocuments() {
        ApplicationFile pdfApplicationFile = new ApplicationFile("my pdf".getBytes(), "someFile.pdf");
        doReturn(pdfApplicationFile).when(pdfGenerator).generate(anyString(), eq(CCAP), any());

        doThrow(new RuntimeException()).doNothing().when(mnitClient).send(any(),any(),any(),any());
        PagesData pagesData = new PagesData();
        PageData chooseProgramsPage = new PageData();
        chooseProgramsPage.put("programs", InputData.builder().value(List.of("CCAP", "SNAP")).build());
        pagesData.put("choosePrograms", chooseProgramsPage);
        applicationData.setPagesData(pagesData);
        documentConsumer.process(application);
        verify(applicationStatusUpdater).updateCcapApplicationStatus(application.getId(), CCAP, DELIVERY_FAILED);
    }

    @Test
    void sendsApplicationIdToMonitoringService() {
        documentConsumer.process(application);
        verify(monitoringService).setApplicationId(application.getId());
    }

    @Test
    void sendsBothImageAndDocumentUploadsSuccessfully() throws IOException {
        mockDocUpload("shiba+file.jpg", "someS3FilePath", MediaType.IMAGE_JPEG_VALUE, "jpg");

        mockDocUpload("test-uploaded-pdf.pdf", "pdfS3FilePath", MediaType.APPLICATION_PDF_VALUE, "pdf");
        when(fileNameGenerator.generateUploadedDocumentName(application, 0, "pdf")).thenReturn("pdf1of2.pdf");
        when(fileNameGenerator.generateUploadedDocumentName(application, 1, "pdf")).thenReturn("pdf2of2.pdf");

        documentConsumer.processUploadedDocuments(application);

        ArgumentCaptor<ApplicationFile> captor = ArgumentCaptor.forClass(ApplicationFile.class);
        verify(mnitClient, times(2)).send(captor.capture(), eq(County.Olmsted), eq(application.getId()), eq(UPLOADED_DOC));

        // Uncomment the following line to regenereate the test fixtures
        // writeByteArrayToFile(captor.getAllValues().get(0).getFileBytes(), "src/test/resources/shiba+file.pdf");
        // writeByteArrayToFile(captor.getAllValues().get(1).getFileBytes(), "src/test/resources/test-uploaded-pdf-with-coverpage.pdf");
        // Assert that converted file contents are as expected
        verifyGeneratedPdf(captor.getAllValues().get(0).getFileBytes(), "shiba+file.pdf");
        verifyGeneratedPdf(captor.getAllValues().get(1).getFileBytes(), "test-uploaded-pdf-with-coverpage.pdf");
    }

    @Test
    void setsUploadedDocumentStatusToSendingWhenProcessUploadedDocumentsIsCalled() throws IOException {
        mockDocUpload("shiba+file.jpg", "someS3FilePath", MediaType.IMAGE_JPEG_VALUE, "jpg");

        mockDocUpload("test-uploaded-pdf.pdf", "pdfS3FilePath", MediaType.APPLICATION_PDF_VALUE, "pdf");

        when(fileNameGenerator.generateUploadedDocumentName(application, 0, "pdf")).thenReturn("pdf1of2.pdf");
        when(fileNameGenerator.generateUploadedDocumentName(application, 1, "pdf")).thenReturn("pdf2of2.pdf");

        documentConsumer.processUploadedDocuments(application);

        verify(applicationStatusUpdater).updateUploadedDocumentsStatus(application.getId(), UPLOADED_DOC, SENDING);
    }

    private void mockDocUpload(String uploadedDocFilename, String s3filepath, String contentType, String extension) throws IOException {
        var fileBytes = Files.readAllBytes(getAbsoluteFilepath(uploadedDocFilename));
        when(documentRepositoryService.get(s3filepath)).thenReturn(fileBytes);
        applicationData.addUploadedDoc(
                new MockMultipartFile("someName", "originalName." + extension, contentType, fileBytes),
                s3filepath,
                "someDataUrl",
                contentType);
    }

    private void verifyGeneratedPdf(byte[] actualFileBytes, String expectedFile) throws IOException {
        try (var actual = new ByteArrayInputStream(actualFileBytes);
             var expected = Files.newInputStream(getAbsoluteFilepath(expectedFile))) {
            var compareResult = new PdfComparator<>(expected, actual).compare();
//            compareResult.writeTo("diffOutput"); // uncomment this line to print the diff between the two pdfs
            assertThat(compareResult.isEqual()).isTrue();
        }
    }
}