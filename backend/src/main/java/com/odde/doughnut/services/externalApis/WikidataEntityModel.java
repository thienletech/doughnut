package com.odde.doughnut.services.externalApis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.odde.doughnut.services.WikidataService;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import org.thymeleaf.util.StringUtils;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WikidataEntityModel {
  private Map<String, WikidataEntityItemModel> entities;

  private Optional<WikidataEntityItemModel> getEntityItem(String wikidataId) {
    if (entities == null) {
      return Optional.empty();
    }

    return Optional.of(entities.get(wikidataId));
  }

  public Optional<WikidataValue> getFirstClaimOfProperty(
      String wikidataId, WikidataFields propertyId) {
    var entityItem = getEntityItem(wikidataId);
    if (entityItem.isPresent()) {
      return entityItem.get().getFirstClaimOfProperty(propertyId.label);
    }
    return Optional.empty();
  }

  public boolean isBook(String wikidataId) {
    return getWikiClass(wikidataId).equals(Optional.of(WikidataItems.BOOK.label));
  }

  public Optional<String> getDescription(WikidataService service, String wikidataId) {

    Optional<String> description;

    if (getWikiClass(wikidataId).equals(Optional.of(WikidataItems.HUMAN.label))) {
      Optional<String> country =
          getFirstClaimOfProperty(wikidataId, WikidataFields.COUNTRY_OF_CITIZENSHIP)
              .map(wikiId -> service.getCountry(wikiId));
      Optional<String> birthday =
          getFirstClaimOfProperty(wikidataId, WikidataFields.BIRTHDAY)
              .map(WikidataValue::toDateDescription);
      // Add spacing between birthday and country only if country is not empty
      Optional<String> countryString =
          StringUtils.isEmpty(country.get()) ? Optional.of("") : Optional.of(country.get() + ", ");
      description = Optional.of(countryString.get() + birthday.get());

    } else {
      description =
          getFirstClaimOfProperty(wikidataId, WikidataFields.COORDINATE_LOCATION)
              .map(WikidataValue::toLocationDescription);
    }

    return description;
  }

  private Optional<String> getWikiClass(String wikidataId) {
    return getFirstClaimOfProperty(wikidataId, WikidataFields.INSTANCE_OF)
        .map(WikidataValue::toWikiClass);
  }
}
