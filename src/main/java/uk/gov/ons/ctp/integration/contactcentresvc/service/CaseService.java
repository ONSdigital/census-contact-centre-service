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

/** Service responsible for dealing with Cases */
public interface CaseService {

  CaseDTO getCaseById(final UUID caseId, CaseQueryRequestDTO requestParamsDTO);

  /**
   * Return HH, CE and SPG cases but filter out any HI cases at address
   *
   * @param uprn Unique Property Reference No for which to return cases
   * @param requestParamsDTO request details
   * @return List of Cases at address, excluding HI cases
   * @throws CTPException error querying for case
   */
  List<CaseDTO> getCaseByUPRN(
      final UniquePropertyReferenceNumber uprn, CaseQueryRequestDTO requestParamsDTO)
      throws CTPException;

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
