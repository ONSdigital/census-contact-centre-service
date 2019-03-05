package uk.gov.ons.ctp.integration.contactcentresvc.service.response;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

@Data
@JsonPropertyOrder({ "data-version", "addresses", "total" })
public class PostcodeQueryResponseDTO {
	@JsonProperty("data-version")
	private String dataVersion;
	
    private ArrayList<AddressSummaryDTO> addresses;
	
    private int total;
}
