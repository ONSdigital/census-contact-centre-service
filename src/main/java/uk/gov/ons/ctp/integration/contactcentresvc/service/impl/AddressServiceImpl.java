package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import java.util.ArrayList;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;

import uk.gov.ons.ctp.common.rest.RestClient;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostcodeQueryResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.AddressService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.addressIndex.response.AddressIndexAddressDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.addressIndex.response.PostcodeSearchDataDTO;

/**
 * A ContactCentreDataService implementation which encapsulates all business logic for getting
 * Addresses
 */
@Service
@Validated()
public class AddressServiceImpl implements AddressService {
  private static final Logger log = LoggerFactory.getLogger(AddressServiceImpl.class);

  @Autowired private AppConfig appConfig;

  @Inject
  @Qualifier("addressIndexClient")
  private RestClient addressIndexClient;

	@Override
	public PostcodeQueryResponseDTO postcodeQuery(uk.gov.ons.ctp.integration.contactcentresvc.representation.PostcodeQueryRequestDTO postcodeQueryRequest) {
		String postcode = postcodeQueryRequest.getPostcode();
		int offset = postcodeQueryRequest.getOffset();
		int limit = postcodeQueryRequest.getLimit();
		
		// Postcode query is delegated to Address Index. Build the query params
		log.debug("about to get to the AddressIndex service with {}", postcode, offset, limit);
	      MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
	      queryParams.add("offset", Integer.toString(offset));
	      queryParams.add("limit", Integer.toString(limit));
	      
	      // Ask Address Index to do postcode search
	      String path = appConfig.getAddressIndexSettings().getPostcodeLookupPath();
	      PostcodeSearchDataDTO addressIndexResponse = addressIndexClient.getResource(path, PostcodeSearchDataDTO.class, null, queryParams, postcode);
	      log.debug("PostcodeQuery. Address Index service "
	      		+ "response status: " + addressIndexResponse.getStatus().getCode() 
	      		+ " Found " + addressIndexResponse.getResponse().getAddresses().size() + " addresses");
		  
		  // Summarise the returned addresses
		  ArrayList<AddressDTO> summarisedAddresses = new ArrayList<>();
		  for (AddressIndexAddressDTO fullAddress : addressIndexResponse.getResponse().getAddresses()) {
			  String formattedAddress = fullAddress.getFormattedAddress();
			  String addressPaf = fullAddress.getFormattedAddressPaf();
			  String addressNag = fullAddress.getFormattedAddressNag();
			  String welshAddressPaf = fullAddress.getWelshFormattedAddressPaf();
			  String welshAddressNag = fullAddress.getWelshFormattedAddressNag();

			  AddressDTO addressSummary = new AddressDTO();
			  addressSummary.setUprn(fullAddress.getUprn());
			  addressSummary.setFormattedAddress(selectFirstUsableAddress(addressPaf, addressNag, formattedAddress));
			  addressSummary.setWelshFormattedAddress(selectFirstUsableAddress(welshAddressPaf, welshAddressNag, formattedAddress));
			  
			  summarisedAddresses.add(addressSummary);
		  }

		  // Complete construction of response objects
		  PostcodeQueryResponseDTO queryResponse = new PostcodeQueryResponseDTO();
		  queryResponse.setDataVersion(addressIndexResponse.getDataVersion());
		  queryResponse.setAddresses(summarisedAddresses);
		  queryResponse.setTotal(addressIndexResponse.getResponse().getTotal());
		  
		return queryResponse;
	}

	/**
	 * This method takes multiple addresses and returns the fi
	 * @param candidateAddresses contains 1 or more addresses.
	 * @return the first non-null and non-empty address, or an empty string if none of the supplied addresses are suitable.
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
