package org.codeforamerica.shiba.pages.enrichment;

import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.IDENTIFY_ZIPCODE;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getFirstValue;

import java.util.List;
import java.util.Map;
import org.codeforamerica.shiba.County;
import org.codeforamerica.shiba.pages.data.InputData;
import org.codeforamerica.shiba.pages.data.PageData;
import org.codeforamerica.shiba.pages.data.PagesData;
import org.springframework.stereotype.Component;

@Component
public class ZipcodeToCountyEnrichment implements Enrichment {

  private final Map<String, County> countyZipCodeMap;

  public ZipcodeToCountyEnrichment(Map<String, County> countyZipCodeMap) {
    this.countyZipCodeMap = countyZipCodeMap;
  }

  @Override
  public PageData process(PagesData pagesData) {
    String zipcode = getFirstValue(pagesData, IDENTIFY_ZIPCODE);
    County county = countyZipCodeMap.get(zipcode);
    if (county == null) {
      county = County.Other;
    }

    return new PageData(Map.of("mappedCounty", new InputData(List.of(county.name()))));
  }
}
