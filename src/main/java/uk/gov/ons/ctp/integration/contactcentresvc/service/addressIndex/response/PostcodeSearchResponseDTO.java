package uk.gov.ons.ctp.integration.contactcentresvc.service.addressIndex.response;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostcodeSearchResponseDTO {
	
	private String postcode;
	
	private ArrayList<AddressIndexAddressDTO> addresses;
	
	private int limit;
	
	private int offset;
	
	private int total;
}
