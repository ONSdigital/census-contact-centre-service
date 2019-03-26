package uk.gov.ons.ctp.integration.contactcentresvc.service;

import java.util.List;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.FulfilmentDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.FulfilmentsRequestDTO;

public interface FulfilmentsService {

  public default List<FulfilmentDTO> getFulfilments(FulfilmentsRequestDTO requestDTO) {
    return null;
  }
}
