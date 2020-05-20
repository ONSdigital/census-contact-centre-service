package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.util.StringUtils;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.AddressServiceClientServiceImpl;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexAddressCompositeDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexAddressDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexSearchResultsCompositeDTO;
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
  public AddressIndexAddressCompositeDTO uprnQuery(long uprn) throws CTPException {
    log.with("uprnQueryRequest", uprn).debug("Running search by uprn");

    // Delegate the query to Address Index
    try {
      AddressIndexSearchResultsCompositeDTO addressResult = addressServiceClient.searchByUPRN(uprn);
      // No result for UPRN from Address Index search
      if (addressResult.getStatus().getCode() != 200) {
        log.with("uprn", uprn)
            .with("status", addressResult.getStatus().getCode())
            .with("message", addressResult.getStatus().getMessage())
            .warn("UPRN not found calling Address Index");
        throw new CTPException(
            CTPException.Fault.RESOURCE_NOT_FOUND,
            "UPRN: %s, status: %s, message: %s",
            uprn,
            addressResult.getStatus().getCode(),
            addressResult.getStatus().getMessage());
      }
      AddressIndexAddressCompositeDTO address = addressResult.getResponse().getAddress();
      log.with("uprn", uprn).debug("UPRN search is returning address");
      return address;
    } catch (ResponseStatusException ex) {
      log.with("uprn", uprn)
          .with("status", ex.getStatus())
          .with("message", ex.getMessage())
          .warn("UPRN not found calling Address Index");
      throw ex;
    }
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
      String estabDescription = fullAddress.getCensusEstabType();

      AddressDTO addressSummary = new AddressDTO();
      addressSummary.setUprn(fullAddress.getUprn());
      addressSummary.setRegion(fullAddress.getCountryCode());
      addressSummary.setAddressType(fullAddress.getCensusAddressType());
      addressSummary.setEstabType(EstabType.forCode(estabDescription).name());
      addressSummary.setEstabDescription(estabDescription);
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
