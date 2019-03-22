package uk.gov.ons.ctp.integration.contactcentresvc.service;

import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostcodeQueryRequestDTO;

public interface AddressService {

  public AddressQueryResponseDTO addressQuery(AddressQueryRequestDTO addressQueryRequest);

  public AddressQueryResponseDTO postcodeQuery(PostcodeQueryRequestDTO postcodeQueryRequest);
}
