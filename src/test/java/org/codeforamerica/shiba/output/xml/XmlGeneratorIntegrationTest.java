package org.codeforamerica.shiba.output.xml;

import org.codeforamerica.shiba.Application;
import org.codeforamerica.shiba.ApplicationRepository;
import org.codeforamerica.shiba.County;
import org.codeforamerica.shiba.output.ApplicationFile;
import org.codeforamerica.shiba.output.ApplicationInput;
import org.codeforamerica.shiba.output.applicationinputsmappers.ApplicationInputsMappers;
import org.codeforamerica.shiba.pages.config.ApplicationConfiguration;
import org.codeforamerica.shiba.pages.config.FormInput;
import org.codeforamerica.shiba.pages.config.Option;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.pages.data.InputData;
import org.codeforamerica.shiba.pages.data.PageData;
import org.codeforamerica.shiba.pages.data.PagesData;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
public class XmlGeneratorIntegrationTest {
    @Autowired
    private FileGenerator xmlGenerator;

    @Autowired
    private ApplicationInputsMappers applicationInputsMappers;

    @Value("classpath:OnlineApplication.xsd")
    private Resource onlineApplicationSchema;

    @Autowired
    private ApplicationConfiguration applicationConfiguration;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private Clock clock;

    @Test
    void shouldProduceAValidDocument() throws IOException, SAXException, ParserConfigurationException {
        String applicationId = applicationRepository.getNextId();
        Map<String, PageData> data = applicationConfiguration.getPageDefinitions().stream()
                .map(pageConfiguration -> {
                    Map<String, InputData> inputDataMap = pageConfiguration.getFlattenedInputs().stream()
                            .collect(toMap(
                                    FormInput::getName,
                                    input -> {
                                        if (input.getReadOnly() && input.getDefaultValue() != null) {
                                            return InputData.builder().value(List.of(input.getDefaultValue())).build();
                                        }
                                        @NotNull List<String> value = switch (input.getType()) {
                                            case RADIO, SELECT -> List.of(input.getOptions().get(new Random().nextInt(input.getOptions().size())).getValue());
                                            case CHECKBOX -> input.getOptions().subList(0, new Random().nextInt(input.getOptions().size()) + 1).stream()
                                                    .map(Option::getValue)
                                                    .collect(Collectors.toList());
                                            case DATE -> List.of(LocalDate.ofEpochDay(0).plusDays(new Random().nextInt()).format(DateTimeFormatter.ofPattern("MM/dd/yyyy")).split("/"));
                                            case YES_NO -> List.of(String.valueOf(new Random().nextBoolean()));
                                            default -> Optional.ofNullable(input.getValidators())
                                                    .filter(validators -> validators.size() == 1)
                                                    .map(validators -> switch (validators.get(0).getValidation()) {
                                                        case SSN -> List.of("123456789");
                                                        case ZIPCODE -> List.of("12345");
                                                        case STATE -> List.of("MN");
                                                        default -> List.of("some-value");
                                                    })
                                                    .orElse(List.of("some-value"));
                                        };
                                        return InputData.builder().value(value).build();
                                    }
                            ));
                    return Map.entry(pageConfiguration.getName(), new PageData(inputDataMap));
                })
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        PagesData pagesData = new PagesData();
        pagesData.putAll(data);
        ApplicationData applicationData = new ApplicationData();
        applicationData.setPagesData(pagesData);
        Application application = new Application(applicationId, ZonedDateTime.now(clock), applicationData, County.OTHER);
        List<ApplicationInput> applicationInputs = applicationInputsMappers.map(application);
        ApplicationFile applicationFile = xmlGenerator.generate(applicationInputs, "");

        Document document = byteArrayToDocument(applicationFile.getFileBytes());

        Validator schemaValidator = SchemaFactory.newDefaultInstance()
                .newSchema(onlineApplicationSchema.getFile())
                .newValidator();
        assertThatCode(() -> schemaValidator.validate(new DOMSource(document))).doesNotThrowAnyException();
    }

    private Document byteArrayToDocument(byte[] bytes) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(new ByteArrayInputStream(bytes));
    }
}
