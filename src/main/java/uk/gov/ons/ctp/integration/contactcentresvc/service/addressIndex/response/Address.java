package uk.gov.ons.ctp.integration.contactcentresvc.service.addressIndex.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Address {
	String uprn;
	String formattedAddress;
	String formattedAddressNag;
	String formattedAddressPaf;
	String welshFormattedAddressNag;
	String welshFormattedAddressPaf;
	String classificationCode;
    int confidenceScore;
}
