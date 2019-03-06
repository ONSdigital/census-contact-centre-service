package uk.gov.ons.ctp.integration.contactcentresvc.service.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

@Data
@JsonPropertyOrder({ "uprn", "formatted-address", "welsh-formatted-address", "household" })
public class AddressSummaryDTO {
	String uprn;
    
	@JsonProperty("formatted-address")
	String formattedAddress;
	
	@JsonProperty("welsh-formatted-address")
	String welshFormattedAddress;
}
