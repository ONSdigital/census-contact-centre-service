package uk.gov.ons.ctp.integration.contactcentresvc.service;

import uk.gov.ons.ctp.integration.contactcentresvc.service.request.PostcodeQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.response.PostcodeQueryResponseDTO;

public interface AddressService {
	public PostcodeQueryResponseDTO postcodeQuery(PostcodeQueryRequestDTO postcodeQueryRequest);
}
