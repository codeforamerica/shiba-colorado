package org.codeforamerica.shiba.output;

import lombok.extern.slf4j.Slf4j;
import org.codeforamerica.shiba.MonitoringService;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.application.parsers.ApplicationDataParser;
import org.codeforamerica.shiba.mnit.MnitEsbWebServiceClient;
import org.codeforamerica.shiba.output.pdf.PdfGenerator;
import org.codeforamerica.shiba.output.xml.XmlGenerator;
import org.codeforamerica.shiba.pages.data.UploadedDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.codeforamerica.shiba.output.Document.UPLOADED_DOC;
import static org.codeforamerica.shiba.output.Recipient.CASEWORKER;

@Component
@Slf4j
public class MnitDocumentConsumer {
    private final MnitEsbWebServiceClient mnitClient;
    private final XmlGenerator xmlGenerator;
    private final PdfGenerator pdfGenerator;
    private final ApplicationDataParser<List<Document>> documentListParser;
    private final MonitoringService monitoringService;
    private final String activeProfile;

    public MnitDocumentConsumer(MnitEsbWebServiceClient mnitClient,
                                XmlGenerator xmlGenerator,
                                PdfGenerator pdfGenerator,
                                ApplicationDataParser<List<Document>> documentListParser,
                                MonitoringService monitoringService,
                                @Value("${spring.profiles.active:dev}") String activeProfile) {
        this.mnitClient = mnitClient;
        this.xmlGenerator = xmlGenerator;
        this.pdfGenerator = pdfGenerator;
        this.documentListParser = documentListParser;
        this.monitoringService = monitoringService;
        this.activeProfile = activeProfile;
    }

    public void process(Application application) {
        monitoringService.setApplicationId(application.getId());
        // Send the CAF and CCAP as PDFs
        documentListParser.parse(application.getApplicationData()).forEach(documentType -> mnitClient.send(
                pdfGenerator.generate(application.getId(), documentType, CASEWORKER), application.getCounty(),
                application.getId(), documentType)
        );
        mnitClient.send(xmlGenerator.generate(application.getId(), Document.CAF, CASEWORKER), application.getCounty(), application.getId(), Document.CAF);
    }

    public void processUploadedDocuments(Application application) {
        List<UploadedDocument> uploadedDocs = application.getApplicationData().getUploadedDocs();
        byte[] coverPage = pdfGenerator.generate(application, UPLOADED_DOC, CASEWORKER).getFileBytes();
        for (int i = 0; i < uploadedDocs.size(); i++) {
            UploadedDocument uploadedDocument = uploadedDocs.get(i);
            ApplicationFile fileToSend = pdfGenerator.generateForUploadedDocument(uploadedDocument, i, application, coverPage);

            if (fileToSend.getFileBytes().length > 0) {
                log.info("Now sending: " + fileToSend.getFileName() + " original filename: " + uploadedDocument.getFilename());
                mnitClient.send(fileToSend, application.getCounty(), application.getId(), UPLOADED_DOC);
                log.info("Finished sending document " + fileToSend.getFileName());
            } else if (activeProfile.equals("demo") || activeProfile.equals("staging") || activeProfile.equals("production")) {
                log.error("Skipped uploading file " + uploadedDocument.getFilename() + " because it was empty. This should only happen in a dev environment.");
            } else {
                log.info("Pretending to send file " + uploadedDocument.getFilename() + ".");
            }
        }
    }
}