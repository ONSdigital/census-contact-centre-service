package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import uk.gov.ons.ctp.common.util.StringUtils;
import uk.gov.ons.ctp.integration.contactcentresvc.client.AddressServiceClientServiceImpl;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostcodeQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.AddressService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.addressindex.model.AddressIndexAddressDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.addressindex.model.AddressIndexSearchResultsDTO;

/**
 * A ContactCentreDataService implementation which encapsulates all business logic for getting
 * Addresses
 */
@Service
@Validated()
public class AddressServiceImpl implements AddressService {
  private static final Logger log = LoggerFactory.getLogger(AddressServiceImpl.class);

  @Autowired private AddressServiceClientServiceImpl addressServiceClient;

  @Override
  public AddressQueryResponseDTO addressQuery(AddressQueryRequestDTO addressQueryRequest) {
    log.debug("Running search by address. {}", addressQueryRequest);

    // Delegate the query to Address Index
    AddressIndexSearchResultsDTO addressIndexResponse =
        addressServiceClient.searchByAddress(addressQueryRequest);

    // Summarise the returned addresses
    AddressQueryResponseDTO results =
        convertAddressIndexResultsToSummarisedAdresses(addressIndexResponse);

    log.debug("Postcode search is returning {} addresses", results.getAddresses().size());
    return results;
  }

  @Override
  public AddressQueryResponseDTO postcodeQuery(PostcodeQueryRequestDTO postcodeQueryRequest) {
    log.debug("Running search by postcode. {}", postcodeQueryRequest);

    // Delegate the query to Address Index
    AddressIndexSearchResultsDTO addressIndexResponse =
        addressServiceClient.searchByPostcode(postcodeQueryRequest);

    // Summarise the returned addresses
    AddressQueryResponseDTO results =
        convertAddressIndexResultsToSummarisedAdresses(addressIndexResponse);

    log.debug("Postcode search is returning {} addresses", results.getAddresses().size());
    return results;
  }

  private AddressQueryResponseDTO convertAddressIndexResultsToSummarisedAdresses(
      AddressIndexSearchResultsDTO addressIndexResponse) {
    ArrayList<AddressDTO> summarisedAddresses = new ArrayList<>();
    for (AddressIndexAddressDTO fullAddress : addressIndexResponse.getResponse().getAddresses()) {
      String formattedAddress = fullAddress.getFormattedAddress();
      String addressPaf = fullAddress.getFormattedAddressPaf();
      String addressNag = fullAddress.getFormattedAddressNag();
      String welshAddressPaf = fullAddress.getWelshFormattedAddressPaf();
      String welshAddressNag = fullAddress.getWelshFormattedAddressNag();

      AddressDTO addressSummary = new AddressDTO();
      addressSummary.setUprn(fullAddress.getUprn());
      addressSummary.setFormattedAddress(StringUtils.selectFirstNonBlankString(addressPaf, addressNag, formattedAddress));
      addressSummary.setWelshFormattedAddress(StringUtils.selectFirstNonBlankString(welshAddressPaf, welshAddressNag, formattedAddress));
      
      summarisedAddresses.add(addressSummary);
    }

    // Complete construction of response objects
    AddressQueryResponseDTO queryResponse = new AddressQueryResponseDTO();
    queryResponse.setDataVersion(addressIndexResponse.getDataVersion());
    queryResponse.setAddresses(summarisedAddresses);
    queryResponse.setTotal(summarisedAddresses.size());

    return queryResponse;
  }
}
