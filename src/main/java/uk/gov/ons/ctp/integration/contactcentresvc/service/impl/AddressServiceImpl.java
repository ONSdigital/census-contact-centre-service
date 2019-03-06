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
import uk.gov.ons.ctp.integration.contactcentresvc.service.AddressService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.addressIndex.response.Address;
import uk.gov.ons.ctp.integration.contactcentresvc.service.addressIndex.response.PostcodeQueryDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.request.PostcodeQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.response.AddressSummaryDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.response.PostcodeQueryResponseDTO;

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
	public PostcodeQueryResponseDTO postcodeQuery(PostcodeQueryRequestDTO postcodeQueryRequest) {
		String postcode = postcodeQueryRequest.getPostcode();
		int offset = postcodeQueryRequest.getOffset();
		int limit = postcodeQueryRequest.getLimit();
		
		// Delegate postcode query to Address Index
		log.debug("about to get to the AddressIndex service with {}", postcode, offset, limit);
	      MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
	      queryParams.add("offset", Integer.toString(offset));
	      queryParams.add("limit", Integer.toString(limit));
	      
	      String path = appConfig.getAddressIndexSettings().getPostcodeLookupPath();
	      PostcodeQueryDTO response = addressIndexClient.getResource(path, PostcodeQueryDTO.class, null, queryParams, postcode);
	      log.debug("Address Service found " + response.getResponse().getAddresses().size() + " addresses");

	      StringBuilder results = new StringBuilder();
		  results.append("For: " + postcode + "\n");
		  for (Address a : response.getResponse().getAddresses()) {
			  results.append(a.getWelshFormattedAddressNag() + "\n");
		  }
		  
		  // Summarise key information from AddressIndex response
		  ArrayList<AddressSummaryDTO> addresses = new ArrayList<>();
		  for (Address address : response.getResponse().getAddresses()) {
			  String formattedAddress = address.getFormattedAddress();
			  String addressPaf = address.getFormattedAddressPaf();
			  String addressNag = address.getFormattedAddressNag();
			  String welshAddressPaf = address.getWelshFormattedAddressPaf();
			  String welshAddressNag = address.getWelshFormattedAddressNag();

			  AddressSummaryDTO addressSummary = new AddressSummaryDTO();
			  addressSummary.setUprn(address.getUprn());
			  addressSummary.setFormattedAddress(selectFirstUsableAddress(addressPaf, addressNag, formattedAddress));
			  addressSummary.setWelshFormattedAddress(selectFirstUsableAddress(welshAddressPaf, welshAddressNag, formattedAddress));
			  
			  addresses.add(addressSummary);
		  }

		  // Complete construction of response objects
		  PostcodeQueryResponseDTO queryResponse = new PostcodeQueryResponseDTO();
		  queryResponse.setDataVersion(response.getDataVersion());
		  queryResponse.setAddresses(addresses);
		  queryResponse.setTotal(response.getResponse().getTotal());
		  
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
