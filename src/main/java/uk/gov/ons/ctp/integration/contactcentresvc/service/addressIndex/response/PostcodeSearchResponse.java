package uk.gov.ons.ctp.integration.contactcentresvc.service.addressIndex.response;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostcodeSearchResponse {
	String postcode;
	
	ArrayList<Address> addresses;
	
	int limit;
	int offset;
	int total;
}
