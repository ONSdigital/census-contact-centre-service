package uk.gov.ons.ctp.integration.contactcentresvc.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequestedEvent;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AppointmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseEventDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.LaunchRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalUnresolvedFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RefusalRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SMSUnresolvedFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.model.UniquePropertyReferenceNumber;

public interface CaseService {

  public CaseDTO getCaseById(final UUID caseId, CaseRequestDTO requestParamsDTO);

  public default List<CaseDTO> getCaseByUPRN(
      final UniquePropertyReferenceNumber uprn, CaseRequestDTO requestParamsDTO) {
    List<CaseDTO> cases = new ArrayList<>();
    cases.add(createFakeCaseDTO());
    cases.add(createFakeCaseDTO());
    cases.add(createFakeCaseDTO());
    return cases;
  }

  public CaseDTO getCaseByCaseReference(final long caseRef, CaseRequestDTO requestParamsDTO);

  public default String getLaunchURLForCaseId(
      final UUID caseId, LaunchRequestDTO requestParamsDTO) {
    return "{\"url\": \"https://www.google.co.uk/search?q=FAKE+"
        + (caseId.hashCode() & 0xFF)
        + (requestParamsDTO.getAgentId().hashCode() & 0xFF)
        + "\"}\n";
  }

  public default ResponseDTO fulfilmentRequestByPost(
      UUID caseId, PostalFulfilmentRequestDTO requestBodyDTO) throws CTPException {
    ResponseDTO fakeResponse =
        ResponseDTO.builder().id(caseId.toString()).dateTime(DateTimeUtil.nowUTC()).build();

    return fakeResponse;
  }

  public FulfilmentRequestedEvent searchProductsAndConstructEvent(
      String fulfilmentCode, Product.DeliveryChannel deliveryChannel) throws CTPException;

  public default ResponseDTO fulfilmentRequestBySMS(
      UUID caseId, SMSFulfilmentRequestDTO requestBodyDTO) {
    ResponseDTO fakeResponse =
        ResponseDTO.builder().id(caseId.toString()).dateTime(DateTimeUtil.nowUTC()).build();

    return fakeResponse;
  }

  public default ResponseDTO fulfilmentUnresolvedRequestByPost(
      PostalUnresolvedFulfilmentRequestDTO requestBodyDTO) {
    ResponseDTO fakeResponse =
        ResponseDTO.builder()
            .id(createSemiRandomFakeUUID().toString())
            .dateTime(DateTimeUtil.nowUTC())
            .build();

    return fakeResponse;
  }

  public default ResponseDTO fulfilmentUnresolvedRequestBySMS(
      SMSUnresolvedFulfilmentRequestDTO requestBodyDTO) {
    ResponseDTO fakeResponse =
        ResponseDTO.builder()
            .id(createSemiRandomFakeUUID().toString())
            .dateTime(DateTimeUtil.nowUTC())
            .build();

    return fakeResponse;
  }

  public default ResponseDTO makeAppointment(UUID caseId, AppointmentRequestDTO requestBodyDTO) {
    ResponseDTO fakeResponse =
        ResponseDTO.builder().id(caseId.toString()).dateTime(DateTimeUtil.nowUTC()).build();

    return fakeResponse;
  }

  public default ResponseDTO reportRefusal(UUID caseId, @Valid RefusalRequestDTO requestBodyDTO) {
    ResponseDTO fakeResponse =
        ResponseDTO.builder().id(caseId.toString()).dateTime(DateTimeUtil.nowUTC()).build();

    return fakeResponse;
  }

  private CaseDTO createFakeCaseDTO() {
    return createFakeCaseDTO(createSemiRandomFakeUUID());
  }

  private CaseDTO createFakeCaseDTO(UUID caseId) {
    CaseEventDTO caseEventDTO1 =
        CaseEventDTO.builder()
            .description("Made up case event DTO")
            .category("create")
            .createdDateTime(DateTimeUtil.nowUTC())
            .build();
    CaseEventDTO caseEventDTO2 =
        CaseEventDTO.builder()
            .description("Another fake case event DTO")
            .category("update")
            .createdDateTime(DateTimeUtil.nowUTC())
            .build();
    CaseEventDTO caseEventDTO3 =
        CaseEventDTO.builder()
            .description("Yet another fake case event DTO")
            .category("update")
            .createdDateTime(DateTimeUtil.nowUTC())
            .build();

    CaseDTO fakeCaseDTO =
        CaseDTO.builder()
            .id(caseId)
            .caseRef("123456789")
            .caseType("H")
            .createdDateTime(DateTimeUtil.nowUTC())
            .addressLine1("The Novelty Rock Emporium")
            .addressLine2("Rock House")
            .addressLine3("Cowick Lane")
            .townName("Exeter")
            .region("E")
            .postcode("EX2 9HY")
            .caseEvents(Arrays.asList(caseEventDTO1, caseEventDTO2, caseEventDTO3))
            .build();

    return fakeCaseDTO;
  }

  private UUID createSemiRandomFakeUUID() {
    String randomUUID = UUID.randomUUID().toString();
    String firstPart = randomUUID.substring(0, 9);
    String lastPart = randomUUID.substring(23);
    String semiRandomUUID = firstPart + "aaaa-bbbb-cccc" + lastPart;

    return UUID.fromString(semiRandomUUID);
  }
}
