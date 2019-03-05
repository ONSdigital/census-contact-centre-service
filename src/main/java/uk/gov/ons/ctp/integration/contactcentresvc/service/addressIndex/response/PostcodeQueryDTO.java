package uk.gov.ons.ctp.integration.contactcentresvc.service.addressIndex.response;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties({ "apiVersion" })
public class PostcodeQueryDTO {
	String dataVersion;
    PostcodeSearchResponse response;
    Status status;
    ArrayList<String> errors;
}
