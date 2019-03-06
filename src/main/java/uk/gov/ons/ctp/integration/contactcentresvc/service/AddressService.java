package uk.gov.ons.ctp.integration.contactcentresvc.service;

import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostcodeQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostcodeQueryResponseDTO;

public interface AddressService {
  public PostcodeQueryResponseDTO postcodeQuery(PostcodeQueryRequestDTO postcodeQueryRequest);
}
