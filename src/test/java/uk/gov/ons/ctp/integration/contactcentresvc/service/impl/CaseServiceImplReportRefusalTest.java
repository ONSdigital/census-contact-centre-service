package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.A_REGION;

import java.util.Date;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.model.AddressCompact;
import uk.gov.ons.ctp.common.event.model.ContactCompact;
import uk.gov.ons.ctp.common.event.model.RespondentRefusalDetails;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.Reason;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RefusalRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/** Unit Test {@link CaseService#reportRefusal(UUID, RefusalRequestDTO) reportRefusal}. */
@RunWith(MockitoJUnitRunner.class)
public class CaseServiceImplReportRefusalTest extends CaseServiceImplTestBase {
  private static final String A_CALL_ID = "8989-NOW";
  private static final String A_UPRN = "1234";

  @Before
  public void setup() {
    when(appConfig.getChannel()).thenReturn(Channel.CC);
  }

  @Test
  public void testRespondentRefusal_withExtraordinaryReason() throws Exception {
    Date dateTime = new Date();
    UUID caseId = UUID.randomUUID();
    UUID expectedEventCaseId = caseId;
    String expectedResponseCaseId = caseId.toString();
    doRespondentRefusalTest(
        caseId, expectedEventCaseId, expectedResponseCaseId, dateTime, Reason.EXTRAORDINARY);
  }

  @Test
  public void testRespondentRefusal_withHardReason() throws Exception {
    Date dateTime = new Date();
    UUID caseId = UUID.randomUUID();
    UUID expectedEventCaseId = caseId;
    String expectedResponseCaseId = caseId.toString();
    doRespondentRefusalTest(
        caseId, expectedEventCaseId, expectedResponseCaseId, dateTime, Reason.HARD);
  }

  @Test
  public void testRespondentRefusal_withUUID() throws Exception {
    Date dateTime = new Date();
    UUID caseId = UUID.randomUUID();
    UUID expectedEventCaseId = caseId;
    String expectedResponseCaseId = caseId.toString();
    doRespondentRefusalTest(
        caseId, expectedEventCaseId, expectedResponseCaseId, dateTime, Reason.EXTRAORDINARY);
  }

  @Test
  public void testRespondentRefusal_withoutDateTime() throws Exception {
    Date dateTime = null;
    UUID caseId = UUID.randomUUID();
    UUID expectedEventCaseId = caseId;
    String expectedResponseCaseId = caseId.toString();
    doRespondentRefusalTest(
        caseId, expectedEventCaseId, expectedResponseCaseId, dateTime, Reason.EXTRAORDINARY);
  }

  private void doRespondentRefusalTest(
      UUID caseId,
      UUID expectedEventCaseId,
      String expectedResponseCaseId,
      Date dateTime,
      Reason reason)
      throws Exception {
    UniquePropertyReferenceNumber uprn = new UniquePropertyReferenceNumber(A_UPRN);
    RefusalRequestDTO refusalPayload =
        RefusalRequestDTO.builder()
            .caseId(caseId)
            .agentId(123)
            .title("Mr")
            .forename("Steve")
            .surname("Jones")
            .addressLine1("1 High Street")
            .addressLine2("Delph")
            .addressLine3("Oldham")
            .townName("Manchester")
            .postcode("OL3 5DJ")
            .uprn(uprn)
            .region(A_REGION)
            .reason(reason)
            .isHouseholder(true)
            .dateTime(dateTime)
            .callId(A_CALL_ID)
            .build();

    // report the refusal
    long timeBeforeInvocation = System.currentTimeMillis();
    ResponseDTO refusalResponse = target.reportRefusal(caseId, refusalPayload);
    long timeAfterInvocation = System.currentTimeMillis();

    // Validate the response to the refusal
    assertEquals(expectedResponseCaseId, refusalResponse.getId());
    verifyTimeInExpectedRange(
        timeBeforeInvocation, timeAfterInvocation, refusalResponse.getDateTime());

    // Validate payload of published event
    RespondentRefusalDetails refusal =
        verifyEventSent(EventType.REFUSAL_RECEIVED, RespondentRefusalDetails.class);
    assertEquals("123", refusal.getAgentId());
    assertEquals(A_CALL_ID, refusal.getCallId());
    assertTrue(refusal.isHouseholder());
    assertEquals(expectedEventCaseId, refusal.getCollectionCase().getId());

    verifyRefusalAddress(refusal);
    assertEquals(reason.name() + "_REFUSAL", refusal.getType());
    ContactCompact expectedContact = new ContactCompact("Mr", "Steve", "Jones");
    assertEquals(expectedContact, refusal.getContact());
  }

  private void verifyRefusalAddress(RespondentRefusalDetails refusal) {
    // Validate address
    AddressCompact address = refusal.getAddress();
    assertEquals("1 High Street", address.getAddressLine1());
    assertEquals("Delph", address.getAddressLine2());
    assertEquals("Oldham", address.getAddressLine3());
    assertEquals("Manchester", address.getTownName());
    assertEquals("OL3 5DJ", address.getPostcode());
    assertEquals(A_REGION.name(), address.getRegion());
    assertEquals(A_UPRN, address.getUprn());
  }
}
