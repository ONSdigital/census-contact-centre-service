package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
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

  @Autowired private AddressServiceClientServiceImpl addressServiceClient;

  @Override
  public AddressQueryResponseDTO addressQuery(AddressQueryRequestDTO addressQueryRequest) {
    // Delegate the query to Address Index
    AddressIndexSearchResultsDTO addressIndexResponse =
        addressServiceClient.addressQuery(addressQueryRequest);

    // Summarise the returned addresses
    return convertAddressIndexResultsToSummarisedAdresses(addressIndexResponse);
  }

  @Override
  public AddressQueryResponseDTO postcodeQuery(PostcodeQueryRequestDTO postcodeQueryRequest) {
    // Delegate the query to Address Index
    AddressIndexSearchResultsDTO addressIndexResponse =
        addressServiceClient.postcodeQuery(postcodeQueryRequest);

    // Summarise the returned addresses
    return convertAddressIndexResultsToSummarisedAdresses(addressIndexResponse);
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
      addressSummary.setFormattedAddress(
          selectFirstUsableAddress(addressPaf, addressNag, formattedAddress));
      addressSummary.setWelshFormattedAddress(
          selectFirstUsableAddress(welshAddressPaf, welshAddressNag, formattedAddress));

      summarisedAddresses.add(addressSummary);
    }

    // Complete construction of response objects
    AddressQueryResponseDTO queryResponse = new AddressQueryResponseDTO();
    queryResponse.setDataVersion(addressIndexResponse.getDataVersion());
    queryResponse.setAddresses(summarisedAddresses);
    queryResponse.setTotal(addressIndexResponse.getResponse().getTotal());

    return queryResponse;
  }

  /**
   * This method takes multiple addresses and returns the first usable one.
   *
   * @param candidateAddresses, contains 1 or more addresses.
   * @return the first non-null and non-empty address, or an empty string if none of the supplied
   *     addresses are suitable.
   */
  private String selectFirstUsableAddress(String... candidateAddresses) {
    String preferredAddress = "";

    // Use the first non empty address
    for (String candidateAddress : candidateAddresses) {
      if (candidateAddress != null && !candidateAddress.trim().isEmpty()) {
        preferredAddress = candidateAddress.trim();
        break;
      }
    }

    return preferredAddress;
  }
}
