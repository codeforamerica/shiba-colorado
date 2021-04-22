package org.codeforamerica.shiba.pages.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.*;
import org.apache.pdfbox.pdmodel.graphics.image.*;
import org.codeforamerica.shiba.*;
import org.codeforamerica.shiba.application.*;
import org.codeforamerica.shiba.documents.*;
import org.codeforamerica.shiba.output.*;
import org.codeforamerica.shiba.output.caf.*;

import java.io.*;
import java.util.*;

@AllArgsConstructor
@Data
@Slf4j
@ToString(exclude = {"dataURL"})
public class UploadedDocument implements Serializable {
    private String filename;
    private String s3Filepath;
    private String dataURL; // thumbnail image as a string, generated by dropzone
    private String type;
    private long size;
    private final List<String> IMAGE_TYPES_TO_CONVERT_TO_PDF = List.of("jpg", "jpeg", "png", "gif");

    public ApplicationFile fileToSend(Application application, Integer index, DocumentRepositoryService documentRepositoryService, FileNameGenerator fileNameGenerator) {

        var fileBytes = documentRepositoryService.get(this.getS3Filepath());
        var extension = Utils.getFileType(this.getFilename());
        if (IMAGE_TYPES_TO_CONVERT_TO_PDF.contains(extension)) {
            try {
                fileBytes = convertImageToPdf(fileBytes);
                extension = "pdf";
            } catch (IOException e) {
                log.error("failed to convert document " + this.getFilename() + " to pdf. Maintaining original type");
            }
        }
        String filename = fileNameGenerator.generateUploadedDocumentName(application, index, extension);
        return new ApplicationFile(fileBytes, filename);
    }

    private byte[] convertImageToPdf(byte[] imageFileBytes) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var image = PDImageXObject.createFromByteArray(doc, imageFileBytes, this.getFilename());
            var pageSize = PDRectangle.LETTER;
            var originalWidth = image.getWidth();
            var originalHeight = image.getHeight();
            var pageWidth = pageSize.getWidth();
            var pageHeight = pageSize.getHeight();
            var ratio = Math.min(pageWidth / originalWidth, pageHeight / originalHeight);
            var scaledWidth = originalWidth * ratio;
            var scaledHeight = originalHeight * ratio;
            var x = (pageWidth - scaledWidth) / 2;
            var y = (pageHeight - scaledHeight) / 2;
            var page = new PDPage(pageSize);
            doc.addPage(page);

            try (PDPageContentStream pdfContents = new PDPageContentStream(doc, page)) {
                pdfContents.drawImage(image, x, y, scaledWidth, scaledHeight);
            }

            doc.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}
