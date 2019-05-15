package uk.gov.ons.ctp.integration.contactcentresvc.message.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.message.GenericEvent;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FulfilmentRequestedEvent extends GenericEvent implements Event {

  private FulfilmentPayload payload = new FulfilmentPayload();
}
