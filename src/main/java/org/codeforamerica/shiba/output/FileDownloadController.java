package org.codeforamerica.shiba.output;

import static org.codeforamerica.shiba.output.Document.CAF;
import static org.codeforamerica.shiba.output.Document.CCAP;
import static org.codeforamerica.shiba.output.Document.CERTAIN_POPS;
import static org.codeforamerica.shiba.output.Recipient.CASEWORKER;
import static org.codeforamerica.shiba.output.Recipient.CLIENT;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.application.ApplicationRepository;
import org.codeforamerica.shiba.output.pdf.PdfGenerator;
import org.codeforamerica.shiba.output.xml.XmlGenerator;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.pages.data.UploadedDocument;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
@Slf4j
public class FileDownloadController {

  private static final String NOT_FOUND_MESSAGE = "Could not find any application with this ID for download";
  private static final String UNSUBMITTED_APPLICATION_MESSAGE = "Submitted time was not set for this application. It is either still in progress or the submitted time was cleared for some reason.";
  private static final String NON_APPLICABLE_DOCUMENT_TYPE = "Could not find a %s application with this ID for download";
  private final XmlGenerator xmlGenerator;
  private final PdfGenerator pdfGenerator;
  private final ApplicationData applicationData;
  private final ApplicationRepository applicationRepository;

  public FileDownloadController(
      XmlGenerator xmlGenerator,
      PdfGenerator pdfGenerator,
      ApplicationData applicationData,
      ApplicationRepository applicationRepository) {
    this.xmlGenerator = xmlGenerator;
    this.pdfGenerator = pdfGenerator;
    this.applicationData = applicationData;
    this.applicationRepository = applicationRepository;
  }

  @GetMapping("/download")
  ResponseEntity<byte[]> downloadPdf() {
    ApplicationFile applicationFile = pdfGenerator.generate(applicationData.getId(), CAF, CLIENT);
    return createResponse(applicationFile);
  }

  @GetMapping("/download-ccap")
  ResponseEntity<byte[]> downloadCcapPdf() {
    ApplicationFile applicationFile = pdfGenerator.generate(applicationData.getId(), CCAP, CLIENT);
    return createResponse(applicationFile);
  }

  @GetMapping("/download-ccap/{applicationId}")
  ResponseEntity<byte[]> downloadCcapPdfWithApplicationId(@PathVariable String applicationId) {
    return createResponse(applicationId, CCAP);
  }

  @GetMapping("/download-certain-pops/{applicationId}")
  ResponseEntity<byte[]> downloadCertainPopsWithApplicationId(@PathVariable String applicationId) {
    return createResponse(applicationId, CERTAIN_POPS);
  }

  @GetMapping("/download-xml")
  ResponseEntity<byte[]> downloadXml() {
    ApplicationFile applicationFile = xmlGenerator.generate(applicationData.getId(), CAF, CLIENT);
    return createResponse(applicationFile);
  }

  @GetMapping("/download-caf/{applicationId}")
  ResponseEntity<byte[]> downloadPdfWithAppJlicationId(@PathVariable String applicationId) {
    return createResponse(applicationId, CAF);
  }

  @GetMapping("/download-docs/{applicationId}")
  ResponseEntity<byte[]> downloadDocsWithApplicationId(@PathVariable String applicationId)
      throws IOException {
    Application application = applicationRepository.find(applicationId);
    List<UploadedDocument> uploadedDocs = application.getApplicationData().getUploadedDocs();

    List<ApplicationFile> applicationFiles = new ArrayList<>();
    byte[] coverPage = pdfGenerator.generateCoverPageForUploadedDocs(application);
    for (int i = 0; i < uploadedDocs.size(); i++) {
      UploadedDocument uploadedDocument = uploadedDocs.get(i);
      ApplicationFile fileToSend = pdfGenerator
          .generateForUploadedDocument(uploadedDocument, i, application, coverPage);

      if (null != fileToSend && fileToSend.getFileBytes().length > 0) {
        applicationFiles.add(fileToSend);
      }
    }

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos)) {

      applicationFiles.forEach(file -> {
        ZipEntry entry = new ZipEntry(file.getFileName());
        entry.setSize(file.getFileBytes().length);
        try {
          zos.putNextEntry(entry);
          zos.write(file.getFileBytes());
          zos.closeEntry();
        } catch (IOException e) {
          log.error("Unable to write file, " + file.getFileName(), e);
        }
      });

      zos.close();
      baos.close();

      // The minimum size of a .ZIP file is 22 bytes even when empty because of metadata
      if (baos.size() > 22) {
        return createResponse(baos.toByteArray(), applicationId + ".zip");
      } else {
        // Applicant should not have been able to "submit" documents without uploading any.
        log.warn("No documents to download");
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
      }
    }
  }

  private ResponseEntity<byte[]> createResponse(ApplicationFile applicationFile) {
    return createResponse(applicationFile.getFileBytes(), applicationFile.getFileName());
  }

  private ResponseEntity<byte[]> createResponse(byte[] fileBytes, String fileName) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(HttpHeaders.CONTENT_DISPOSITION, String.format("filename=\"%s\"", fileName))
        .body(fileBytes);
  }

  /**
   * Create a response without throwing an exception on missing result.
   * <p>
   * Should only be used for internal endpoints!
   */
  private ResponseEntity<byte[]> createResponse(String applicationId, Document document) {
    try {
      Application application = applicationRepository.find(applicationId);
      if (application.getCompletedAt() == null) {
        // The submitted time was not set - The application is still in progress or the time was
        // cleared somehow
        log.info(UNSUBMITTED_APPLICATION_MESSAGE);
        return ResponseEntity.ok().body(UNSUBMITTED_APPLICATION_MESSAGE.getBytes());
      }

      if (application.getDocumentStatuses() == null || application.getDocumentStatuses().stream()
          .noneMatch(documentStatus -> documentStatus.getDocumentType() == document)) {
        // The application exists, but not for this document type
        String msg = String.format(NON_APPLICABLE_DOCUMENT_TYPE, document);
        log.info(msg);
        return ResponseEntity.ok().body(msg.getBytes());
      }

      ApplicationFile applicationFile = pdfGenerator.generate(application, document, CASEWORKER);
      return createResponse(applicationFile);
    } catch (EmptyResultDataAccessException e) {
      log.info(NOT_FOUND_MESSAGE);
      return ResponseEntity.ok().body(NOT_FOUND_MESSAGE.getBytes());
    }
  }
}
