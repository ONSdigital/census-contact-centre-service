package uk.gov.ons.ctp.integration.contactcentresvc.service;

import java.util.Optional;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexAddressSplitDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostcodeQueryRequestDTO;

/** Service responsible for dealing with address related functionality */
public interface AddressService {

  /**
   * Search for an address by search string
   *
   * @param addressQueryRequest with search string, offset and limit for pagination
   * @return result object containing list of addresses
   */
  public AddressQueryResponseDTO addressQuery(AddressQueryRequestDTO addressQueryRequest);

  /**
   * Search for an address by postcode
   *
   * @param postcodeQueryRequest with postcode, offset and limit for pagination
   * @return result object containing list of addresses
   */
  public AddressQueryResponseDTO postcodeQuery(PostcodeQueryRequestDTO postcodeQueryRequest);

  /**
   * Search for an address by uprn
   *
   * @param uprn Unique Property Reference No.
   * @return Optional with result object splitting address into census component fields
   */
  public Optional<AddressIndexAddressSplitDTO> uprnQuery(long uprn) throws CTPException;
}
