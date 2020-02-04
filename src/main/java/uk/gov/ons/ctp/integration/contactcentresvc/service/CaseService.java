package uk.gov.ons.ctp.integration.contactcentresvc.service;

import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.model.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.LaunchRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RefusalRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SMSFulfilmentRequestDTO;

public interface CaseService {

  public CaseDTO getCaseById(final UUID caseId, CaseRequestDTO requestParamsDTO);

  public List<CaseDTO> getCaseByUPRN(
      final UniquePropertyReferenceNumber uprn, CaseRequestDTO requestParamsDTO);

  public CaseDTO getCaseByCaseReference(final long caseRef, CaseRequestDTO requestParamsDTO);

  public String getLaunchURLForCaseId(final UUID caseId, LaunchRequestDTO requestParamsDTO)
      throws CTPException;

  public ResponseDTO fulfilmentRequestByPost(PostalFulfilmentRequestDTO requestBodyDTO)
      throws CTPException;

  public ResponseDTO fulfilmentRequestBySMS(SMSFulfilmentRequestDTO requestBodyDTO)
      throws CTPException;

  public ResponseDTO reportRefusal(UUID caseId, @Valid RefusalRequestDTO requestBodyDTO)
      throws CTPException;

  private UUID createSemiRandomFakeUUID() {
    String randomUUID = UUID.randomUUID().toString();
    String firstPart = randomUUID.substring(0, 9);
    String lastPart = randomUUID.substring(23);
    String semiRandomUUID = firstPart + "aaaa-bbbb-cccc" + lastPart;

    return UUID.fromString(semiRandomUUID);
  }
}
