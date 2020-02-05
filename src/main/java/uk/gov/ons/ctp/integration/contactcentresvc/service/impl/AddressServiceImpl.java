package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import uk.gov.ons.ctp.common.util.StringUtils;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.AddressServiceClientServiceImpl;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexAddressDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexSearchResultsDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostcodeQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.AddressService;

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
    log.with("addressQueryRequest", addressQueryRequest).debug("Running search by address");

    // Delegate the query to Address Index
    AddressIndexSearchResultsDTO addressIndexResponse =
        addressServiceClient.searchByAddress(addressQueryRequest);

    // Summarise the returned addresses
    AddressQueryResponseDTO results =
        convertAddressIndexResultsToSummarisedAdresses(addressIndexResponse);

    log.with("addresses", results.getAddresses().size())
        .debug("Address search is returning addresses");
    return results;
  }

  @Override
  public AddressQueryResponseDTO postcodeQuery(PostcodeQueryRequestDTO postcodeQueryRequest) {
    log.with("postcodeQueryRequest", postcodeQueryRequest).debug("Running search by postcode");

    // Delegate the query to Address Index
    AddressIndexSearchResultsDTO addressIndexResponse =
        addressServiceClient.searchByPostcode(postcodeQueryRequest);

    // Summarise the returned addresses
    AddressQueryResponseDTO results =
        convertAddressIndexResultsToSummarisedAdresses(addressIndexResponse);

    log.with("addresses", results.getAddresses().size())
        .debug("Postcode search is returning addresses");
    return results;
  }

  @Override
  public AddressQueryResponseDTO uprnQuery(Long uprn) {
    log.with("uprnQueryRequest", uprn).debug("Running search by uprn");

    // Delegate the query to Address Index
    AddressIndexSearchResultsDTO addressIndexResponse = addressServiceClient.searchByUPRN(uprn);

    // Summarise the returned addresses
    AddressQueryResponseDTO results =
        convertAddressIndexResultsToSummarisedAdresses(addressIndexResponse);

    log.with("addresses", results.getAddresses().size())
        .debug("Postcode search is returning addresses");
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
      addressSummary.setRegion(fullAddress.getCountryCode());
      addressSummary.setAddressType(fullAddress.getCensusAddressType());
      addressSummary.setEstabType(fullAddress.getCensusEstabType());
      addressSummary.setFormattedAddress(
          StringUtils.selectFirstNonBlankString(addressPaf, addressNag, formattedAddress));
      addressSummary.setWelshFormattedAddress(
          StringUtils.selectFirstNonBlankString(
              welshAddressPaf, welshAddressNag, formattedAddress));

      summarisedAddresses.add(addressSummary);
    }

    // Complete construction of response objects
    AddressQueryResponseDTO queryResponse = new AddressQueryResponseDTO();
    queryResponse.setDataVersion(addressIndexResponse.getDataVersion());
    queryResponse.setAddresses(summarisedAddresses);

    int total = addressIndexResponse.getResponse().getTotal();
    int arraySize = summarisedAddresses.size();

    // UPRN search has no JSON total attribute as only one or zero
    if (total > 0) {
      queryResponse.setTotal(total);
    } else {
      queryResponse.setTotal(arraySize);
    }

    return queryResponse;
  }
}
