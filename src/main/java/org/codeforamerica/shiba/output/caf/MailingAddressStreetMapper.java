package org.codeforamerica.shiba.output.caf;

import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field;
import org.codeforamerica.shiba.output.ApplicationInput;
import org.codeforamerica.shiba.output.ApplicationInputType;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.output.applicationinputsmappers.ApplicationInputsMapper;
import org.codeforamerica.shiba.output.applicationinputsmappers.SubworkflowIterationScopeTracker;
import org.codeforamerica.shiba.pages.config.FeatureFlagConfiguration;
import org.codeforamerica.shiba.pages.data.PagesData;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.*;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getFirstValue;

@Component
public class MailingAddressStreetMapper implements ApplicationInputsMapper {
    private final FeatureFlagConfiguration featureFlagConfiguration;
    private final static String GENERAL_DELIVERY = "General Delivery";

    public MailingAddressStreetMapper(FeatureFlagConfiguration featureFlagConfiguration) {
        this.featureFlagConfiguration = featureFlagConfiguration;
    }

    @Override
    public List<ApplicationInput> map(Application application, Document document, Recipient recipient, SubworkflowIterationScopeTracker scopeTracker) {
        PagesData pagesData = application.getApplicationData().getPagesData();

        // Use home address for mailing
        boolean sameAsHomeAddress = Boolean.parseBoolean(getFirstValue(pagesData, featureFlagConfiguration.get("apply-without-address").isOff() ? SAME_MAILING_ADDRESS : SAME_MAILING_ADDRESS2));
        if (sameAsHomeAddress) {
            return createAddressInputsFromHomeAddress(pagesData);
        }

        String usesEnriched = pagesData.getPageInputFirstValue("mailingAddressValidation", "useEnrichedAddress");
        if (usesEnriched == null) {
            // General delivery
            return createGeneralDeliveryAddressInputs(pagesData);
        }

        // Use mailing address application data
        return createAddressInputsFromMailingAddress(pagesData);
    }

    private List<ApplicationInput> createGeneralDeliveryAddressInputs(PagesData pagesData) {
        return List.of(new ApplicationInput(
                "mailingAddress",
                "selectedStreetAddress",
                List.of(GENERAL_DELIVERY),
                ApplicationInputType.SINGLE_VALUE
        ), new ApplicationInput(
                "mailingAddress",
                "selectedZipCode",
                List.of(getFirstValue(pagesData, GENERAL_DELIVERY_ZIPCODE)),
                ApplicationInputType.SINGLE_VALUE
        ), new ApplicationInput(
                "mailingAddress",
                "selectedCity",
                List.of(getFirstValue(pagesData, GENERAL_DELIVERY_CITY)),
                ApplicationInputType.SINGLE_VALUE
        ), new ApplicationInput(
                "mailingAddress",
                "selectedState",
                List.of("MN"),
                ApplicationInputType.SINGLE_VALUE
        ));
    }

    /**
     * Create mailing address inputs from the home address application data.
     *
     * @param pagesData application data to check
     * @return mailing address inputs
     */
    private List<ApplicationInput> createAddressInputsFromHomeAddress(PagesData pagesData) {
        boolean usesEnriched = Boolean.parseBoolean(pagesData.getPageInputFirstValue("homeAddressValidation", "useEnrichedAddress"));
        if (usesEnriched) {
            return createMailingInputs(pagesData,
                    ENRICHED_HOME_STREET,
                    ENRICHED_HOME_APARTMENT_NUMBER,
                    ENRICHED_HOME_ZIPCODE,
                    ENRICHED_HOME_CITY,
                    ENRICHED_HOME_STATE);
        } else {
            return createMailingInputs(pagesData,
                    HOME_STREET,
                    HOME_APARTMENT_NUMBER,
                    HOME_ZIPCODE,
                    HOME_CITY,
                    HOME_STATE);
        }
    }

    /**
     * Create the mailing address inputs from the mailing address application data.
     * It will be there if the home address isn't also being used for mailing address.
     *
     * @param pagesData application data to check
     * @return mailing address inputs
     */
    private List<ApplicationInput> createAddressInputsFromMailingAddress(PagesData pagesData) {
        boolean usesEnriched = Boolean.parseBoolean(pagesData.getPageInputFirstValue("mailingAddressValidation", "useEnrichedAddress"));
        if (usesEnriched) {
            return createMailingInputs(pagesData,
                    ENRICHED_MAILING_STREET,
                    ENRICHED_MAILING_APARTMENT_NUMBER,
                    ENRICHED_MAILING_ZIPCODE,
                    ENRICHED_MAILING_CITY,
                    ENRICHED_MAILING_STATE);
        } else {
            return createMailingInputs(pagesData,
                    MAILING_STREET,
                    MAILING_APARTMENT_NUMBER,
                    MAILING_ZIPCODE,
                    MAILING_CITY,
                    MAILING_STATE);
        }
    }

    private List<ApplicationInput> createMailingInputs(PagesData pagesData, Field street, Field apartment, Field zipcode, Field city, Field state) {
        return List.of(new ApplicationInput(
                "mailingAddress",
                "selectedStreetAddress",
                List.of(getFirstValue(pagesData, street)),
                ApplicationInputType.SINGLE_VALUE
        ), new ApplicationInput(
                "mailingAddress",
                "selectedApartmentNumber",
                List.of(getFirstValue(pagesData, apartment)),
                ApplicationInputType.SINGLE_VALUE
        ), new ApplicationInput(
                "mailingAddress",
                "selectedZipCode",
                List.of(getFirstValue(pagesData, zipcode)),
                ApplicationInputType.SINGLE_VALUE
        ), new ApplicationInput(
                "mailingAddress",
                "selectedCity",
                List.of(getFirstValue(pagesData, city)),
                ApplicationInputType.SINGLE_VALUE
        ), new ApplicationInput(
                "mailingAddress",
                "selectedState",
                List.of(getFirstValue(pagesData, state)),
                ApplicationInputType.SINGLE_VALUE
        ));
    }
}