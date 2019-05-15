package uk.gov.ons.ctp.integration.contactcentresvc.message.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FulfilmentPayload {

  private FulfilmentRequest collectionCase = new FulfilmentRequest();
}
