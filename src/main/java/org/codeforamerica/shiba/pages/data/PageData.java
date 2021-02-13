package org.codeforamerica.shiba.pages.data;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.codeforamerica.shiba.pages.config.FormInput;
import org.codeforamerica.shiba.pages.config.PageConfiguration;
import org.codeforamerica.shiba.pages.config.Validator;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static org.codeforamerica.shiba.pages.PageUtils.getFormInputName;

@EqualsAndHashCode(callSuper = true)
@Value
@NoArgsConstructor
public class PageData extends HashMap<String, InputData> {
    public PageData(Map<String, InputData> inputDataMap) {
        super(inputDataMap);
    }

    public static PageData fillOut(PageConfiguration page, MultiValueMap<String, String> model) {
        Map<String, InputData> inputDataMap = page.getFlattenedInputs().stream()
                .map(formInput -> {
                    List<String> value = Optional.ofNullable(model)
                            .map(modelMap -> modelMap.get(getFormInputName(formInput.getName())))
                            .orElse(null);
                    InputData inputData = new InputData(value, validatorsFor(formInput, model));
                    return Map.entry(formInput.getName(), inputData);
                })
                .collect(toMap(Entry::getKey, Entry::getValue));
        return new PageData(inputDataMap);
    }

    public static PageData initialize(PageConfiguration pageConfiguration) {
        return new PageData(
                pageConfiguration.getFlattenedInputs().stream()
                        .collect(toMap(
                                FormInput::getName,
                                input -> Optional.ofNullable(input.getDefaultValue())
                                        .map(defaultValue -> new InputData(List.of(defaultValue)))
                                        .orElse(new InputData())
                        )));
    }

    public Boolean isValid() {
        return values().stream().allMatch(InputData::valid);
    }

    private static List<Validator> validatorsFor(FormInput formInput, MultiValueMap<String, String> model) {
        return formInput.getValidators()
                .stream()
                .filter(validator -> validator.shouldValidate(model))
                .collect(Collectors.toList());
    }

    /**
     * Merges the InputData values of otherPage into this PageData.
     *
     * @param otherPage PageData containing values to merge.
     */
    public void mergeInputDataValues(PageData otherPage) {
        if (otherPage != null) {
            otherPage.forEach((key, value) -> {
                putIfAbsent(key, new InputData(new ArrayList<>()));
                get(key).getValue().addAll(value.getValue());
            });
        }
    }

    /**
     * Convenience method for checking if the input data values contain any of the given values.
     *
     * @param inputDataKey input data key to check
     * @param values values to check for
     * @return True - at least one of the input data's values is in the given values list;
     *         False - input data doesn't exist or none of the input data values are in the given values list
     */
    public boolean inputDataValueContainsAny(String inputDataKey, List<String> values) {
        InputData inputData = get(inputDataKey);
        return inputData != null && inputData.getValue().stream().anyMatch(values::contains);
    }
}
