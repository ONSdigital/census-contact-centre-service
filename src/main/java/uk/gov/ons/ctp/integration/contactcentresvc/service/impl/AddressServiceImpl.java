package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static java.util.stream.Collectors.toList;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
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
  private static final String HISTORICAL_ADDRESS_STATUS = "8";

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

  private AddressDTO convertToSummarised(AddressIndexAddressDTO fullAddress) {
    String formattedAddress = fullAddress.getFormattedAddress();
    String addressPaf = fullAddress.getFormattedAddressPaf();
    String addressNag = fullAddress.getFormattedAddressNag();
    String welshAddressPaf = fullAddress.getWelshFormattedAddressPaf();
    String welshAddressNag = fullAddress.getWelshFormattedAddressNag();
    String estabDescription = fullAddress.getCensus().getCensusEstabType();

    AddressDTO addressSummary = new AddressDTO();
    addressSummary.setUprn(fullAddress.getUprn());
    addressSummary.setRegion(fullAddress.getCensus().getCountryCode());
    addressSummary.setAddressType(fullAddress.getCensus().getCensusAddressType());
    addressSummary.setEstabType(EstabType.forCode(estabDescription).name());
    addressSummary.setEstabDescription(estabDescription);
    addressSummary.setFormattedAddress(
        StringUtils.selectFirstNonBlankString(addressPaf, addressNag, formattedAddress));
    addressSummary.setWelshFormattedAddress(
        StringUtils.selectFirstNonBlankString(welshAddressPaf, welshAddressNag, formattedAddress));
    return addressSummary;
  }

  /**
   * Determine whether an address returned from AIMS is historical.
   *
   * <p>In reality, we should never get historical addresses from AIMS. However since it is so
   * important not to return historical addresses, we accept the pagination breakage to filter out
   * any that we find. The theory is that logging errors will notify operations to fix AIMS if it is
   * not honouring the historical=false query parameter, and the service will be rectified as a
   * result.
   *
   * <p>See CR-976.
   *
   * @param dto the address from AIMS
   * @return true if historical; false otherwise.
   */
  private boolean isHistorical(AddressIndexAddressDTO dto) {
    boolean historical = HISTORICAL_ADDRESS_STATUS.equals(dto.getLpiLogicalStatus());
    if (historical) {
      log.with("uprn", dto.getUprn())
          .with("formattedAddress", dto.getFormattedAddress())
          .error("Unexpected historical address returned from AIMS");
    }
    return historical;
  }

  private AddressQueryResponseDTO convertAddressIndexResultsToSummarisedAdresses(
      AddressIndexSearchResultsDTO addressIndexResponse) {
    List<AddressDTO> summarisedAddresses =
        addressIndexResponse
            .getResponse()
            .getAddresses()
            .stream()
            .filter(a -> !isHistorical(a))
            .map(this::convertToSummarised)
            .collect(toList());

    // Complete construction of response objects
    AddressQueryResponseDTO queryResponse = new AddressQueryResponseDTO();
    queryResponse.setDataVersion(addressIndexResponse.getDataVersion());
    queryResponse.setAddresses(new ArrayList<>(summarisedAddresses));

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
