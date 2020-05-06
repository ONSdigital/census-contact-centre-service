package uk.gov.ons.ctp.integration.contactcentresvc.service;

import java.util.List;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.DeliveryChannel;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.FulfilmentDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ProductGroup;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.Region;

public interface FulfilmentsService {
  List<FulfilmentDTO> getFulfilments(
      CaseType caseType,
      Region region,
      DeliveryChannel deliveryChannel,
      Boolean individual,
      ProductGroup productGroup)
      throws CTPException;
}
