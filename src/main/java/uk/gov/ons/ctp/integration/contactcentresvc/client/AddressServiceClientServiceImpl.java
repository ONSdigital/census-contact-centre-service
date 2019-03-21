package uk.gov.ons.ctp.integration.contactcentresvc.client;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import javax.inject.Inject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import uk.gov.ons.ctp.common.rest.RestClient;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostcodeQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.addressindex.model.AddressIndexSearchResultsDTO;

/** This class is responsible for communications with the Addresse Index service. */
@Service
@Validated()
public class AddressServiceClientServiceImpl {
  private static final Logger log = LoggerFactory.getLogger(AddressServiceClientServiceImpl.class);

  @Autowired private AppConfig appConfig;

  @Inject
  @Qualifier("addressIndexClient")
  private RestClient addressIndexClient;

  public AddressIndexSearchResultsDTO addressQuery(AddressQueryRequestDTO addressQueryRequest) {
    String input = addressQueryRequest.getInput();
    int offset = addressQueryRequest.getOffset();
    int limit = addressQueryRequest.getLimit();

    // Postcode query is delegated to Address Index. Build the query params for the request
    log.debug("about to get to the AddressIndex service with query {}", input, offset, limit);
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("input", input);
    queryParams.add("offset", Integer.toString(offset));
    queryParams.add("limit", Integer.toString(limit));

    // Ask Address Index to do an address search
    String path = appConfig.getAddressIndexSettings().getAddressQueryPath();
    AddressIndexSearchResultsDTO addressIndexResponse =
        addressIndexClient.getResource(
            path, AddressIndexSearchResultsDTO.class, null, queryParams, new Object[] {});
    log.info(
        "AddressQuery. Response status: "
            + addressIndexResponse.getStatus().getCode()
            + " Found: "
            + addressIndexResponse.getResponse().getAddresses().size()
            + " addresses");

    return addressIndexResponse;
  }

  public AddressIndexSearchResultsDTO postcodeQuery(PostcodeQueryRequestDTO postcodeQueryRequest) {

    String postcode = postcodeQueryRequest.getPostcode();
    int offset = postcodeQueryRequest.getOffset();
    int limit = postcodeQueryRequest.getLimit();

    // Postcode query is delegated to Address Index. Build the query params
    log.debug("about to get to the AddressIndex service with postcode {}", postcode, offset, limit);
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("offset", Integer.toString(offset));
    queryParams.add("limit", Integer.toString(limit));

    // Ask Address Index to do postcode search
    String path = appConfig.getAddressIndexSettings().getPostcodeLookupPath();
    AddressIndexSearchResultsDTO addressIndexResponse =
        addressIndexClient.getResource(
            path, AddressIndexSearchResultsDTO.class, null, queryParams, postcode);
    log.info(
        "PostcodeQuery. Response status: "
            + addressIndexResponse.getStatus().getCode()
            + " Found: "
            + addressIndexResponse.getResponse().getAddresses().size()
            + " addresses");

    return addressIndexResponse;
  }
}
