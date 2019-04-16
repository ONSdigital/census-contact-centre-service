package uk.gov.ons.ctp.integration.contactcentresvc.service;

import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressUpdateRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostcodeQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;

public interface AddressService {

  public AddressQueryResponseDTO addressQuery(AddressQueryRequestDTO addressQueryRequest);

  public AddressQueryResponseDTO postcodeQuery(PostcodeQueryRequestDTO postcodeQueryRequest);

  public default ResponseDTO addressChange(AddressUpdateRequestDTO addressUpdateRequestDTO) {
    ResponseDTO fakeResponse = new ResponseDTO();
    fakeResponse.setId("8437625585067");
    fakeResponse.setDateTime("2019-04-01T01:01:01.011");
    return fakeResponse;
  }
}
