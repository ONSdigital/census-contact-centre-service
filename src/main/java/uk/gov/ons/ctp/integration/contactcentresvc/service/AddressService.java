package uk.gov.ons.ctp.integration.contactcentresvc.service;

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
   * @return result object as for other searches but with only ever 0 or 1 result
   */
  public AddressQueryResponseDTO uprnQuery(Long uprn);
}
