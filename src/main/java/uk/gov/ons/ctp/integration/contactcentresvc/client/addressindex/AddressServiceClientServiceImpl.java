package uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import uk.gov.ons.ctp.common.rest.RestClient;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexSearchResultsCompositeDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexSearchResultsDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostcodeQueryRequestDTO;

/** This class is responsible for communications with the Address Index service. */
@Service
@Validated
public class AddressServiceClientServiceImpl {
  private static final Logger log = LoggerFactory.getLogger(AddressServiceClientServiceImpl.class);

  @Autowired private AppConfig appConfig;

  @Inject
  @Qualifier("addressIndexClient")
  private RestClient addressIndexClient;

  public AddressIndexSearchResultsDTO searchByAddress(AddressQueryRequestDTO addressQueryRequest) {
    log.debug("Delegating address search to AddressIndex service");

    String input = addressQueryRequest.getInput();
    int offset = addressQueryRequest.getOffset();
    int limit = addressQueryRequest.getLimit();

    // Address query is delegated to Address Index. Build the query params for the request
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("input", input);
    queryParams.add("offset", Integer.toString(offset));
    queryParams.add("limit", Integer.toString(limit));
    addEpoch(queryParams);

    // Ask Address Index to do an address search
    String path = appConfig.getAddressIndexSettings().getAddressQueryPath();
    AddressIndexSearchResultsDTO addressIndexResponse =
        addressIndexClient.getResource(
            path, AddressIndexSearchResultsDTO.class, null, queryParams, new Object[] {});
    log.with("status", addressIndexResponse.getStatus().getCode())
        .with("addresses", addressIndexResponse.getResponse().getAddresses().size())
        .debug("Address query response received");

    return addressIndexResponse;
  }

  public AddressIndexSearchResultsDTO searchByPostcode(
      PostcodeQueryRequestDTO postcodeQueryRequest) {
    log.debug("Delegating postcode search to the AddressIndex service");

    int offset = postcodeQueryRequest.getOffset();
    int limit = postcodeQueryRequest.getLimit();

    // Postcode query is delegated to Address Index. Build the query params
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("offset", Integer.toString(offset));
    queryParams.add("limit", Integer.toString(limit));
    addEpoch(queryParams);

    // Ask Address Index to do postcode search
    String postcode = postcodeQueryRequest.getPostcode();
    String path = appConfig.getAddressIndexSettings().getPostcodeLookupPath();
    AddressIndexSearchResultsDTO addressIndexResponse =
        addressIndexClient.getResource(
            path, AddressIndexSearchResultsDTO.class, null, queryParams, postcode);
    log.with("postcode", postcode)
        .with("status", addressIndexResponse.getStatus().getCode())
        .with("addresses", addressIndexResponse.getResponse().getAddresses().size())
        .debug("Postcode query response received");

    return addressIndexResponse;
  }

  public AddressIndexSearchResultsCompositeDTO searchByUPRN(Long uprn) {
    log.debug("Delegating UPRN search to AddressIndex service");

    // Build map for query params
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("addresstype", appConfig.getAddressIndexSettings().getAddressType());
    addEpoch(queryParams);

    // Ask Address Index to do uprn search
    String path = appConfig.getAddressIndexSettings().getUprnLookupPath();
    AddressIndexSearchResultsCompositeDTO addressIndexResponse =
        addressIndexClient.getResource(
            path, AddressIndexSearchResultsCompositeDTO.class, null, queryParams, uprn.toString());

    log.with("uprn", uprn)
        .with("status", addressIndexResponse.getStatus().getCode())
        .debug("UPRN query response received");

    return addressIndexResponse;
  }

  private MultiValueMap<String, String> addEpoch(MultiValueMap<String, String> queryParams) {
    String epoch = appConfig.getAddressIndexSettings().getEpoch();
    if (!StringUtils.isBlank(epoch)) {
      queryParams.add("epoch", epoch);
    }
    return queryParams;
  }
}
