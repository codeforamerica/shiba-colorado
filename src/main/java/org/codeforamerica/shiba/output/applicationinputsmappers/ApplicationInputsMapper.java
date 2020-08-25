package org.codeforamerica.shiba.output.applicationinputsmappers;

import org.codeforamerica.shiba.Application;
import org.codeforamerica.shiba.output.ApplicationInput;
import org.codeforamerica.shiba.output.ApplicationInputType;
import org.codeforamerica.shiba.pages.config.FormInputType;

import java.util.List;

public interface ApplicationInputsMapper {
    List<ApplicationInput> map(Application application);

    static ApplicationInputType formInputTypeToApplicationInputType(FormInputType type) {
        return switch (type) {
            case CHECKBOX -> ApplicationInputType.ENUMERATED_MULTI_VALUE;
            case RADIO, SELECT -> ApplicationInputType.ENUMERATED_SINGLE_VALUE;
            case DATE -> ApplicationInputType.DATE_VALUE;
            case TEXT, HOURLY_WAGE, NUMBER, YES_NO, MONEY, INCREMENTER, TEXTAREA, CUSTOM -> ApplicationInputType.SINGLE_VALUE;
        };
    }
}
