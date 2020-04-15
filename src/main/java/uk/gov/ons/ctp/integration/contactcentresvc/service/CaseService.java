package uk.gov.ons.ctp.integration.contactcentresvc.service;

import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.model.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.LaunchRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ModifyCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RefusalRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SMSFulfilmentRequestDTO;

public interface CaseService {

  CaseDTO getCaseById(final UUID caseId, CaseQueryRequestDTO requestParamsDTO);

  List<CaseDTO> getCaseByUPRN(
      final UniquePropertyReferenceNumber uprn, CaseQueryRequestDTO requestParamsDTO);

  CaseDTO getCaseByCaseReference(final long caseRef, CaseQueryRequestDTO requestParamsDTO);

  String getLaunchURLForCaseId(final UUID caseId, LaunchRequestDTO requestParamsDTO)
      throws CTPException;

  ResponseDTO modifyCase(ModifyCaseRequestDTO modifyRequestDTO) throws CTPException;

  ResponseDTO fulfilmentRequestByPost(PostalFulfilmentRequestDTO requestBodyDTO)
      throws CTPException;

  ResponseDTO fulfilmentRequestBySMS(SMSFulfilmentRequestDTO requestBodyDTO) throws CTPException;

  ResponseDTO reportRefusal(UUID caseId, @Valid RefusalRequestDTO requestBodyDTO)
      throws CTPException;
}
