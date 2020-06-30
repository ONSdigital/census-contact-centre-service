package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.A_REGION;

import java.util.Date;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.AddressCompact;
import uk.gov.ons.ctp.common.event.model.Contact;
import uk.gov.ons.ctp.common.event.model.RespondentRefusalDetails;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.Reason;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RefusalRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/** Unit Test {@link CaseService#reportRefusal(UUID, RefusalRequestDTO) reportRefusal}. */
@RunWith(MockitoJUnitRunner.class)
public class CaseServiceImplReportRefusalTest extends CaseServiceImplTestBase {
  private static final EventType REFUSAL_EVENT_TYPE_FIELD_VALUE = EventType.REFUSAL_RECEIVED;
  private static final Source REFUSAL_SOURCE_FIELD_VALUE = Source.CONTACT_CENTRE_API;
  private static final Channel REFUSAL_CHANNEL_FIELD_VALUE = Channel.CC;
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

  @Test
  public void testRespondentRefusal_forUnknownUUID() throws Exception {
    UUID unknownCaseId = null;
    UUID expectedEventCaseId = UUID.fromString("00000000-0000-0000-0000-000000000000");
    String expectedResponseCaseId = "unknown";
    doRespondentRefusalTest(
        unknownCaseId,
        expectedEventCaseId,
        expectedResponseCaseId,
        new Date(),
        Reason.EXTRAORDINARY);
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
            .caseId(caseId == null ? null : caseId.toString())
            .agentId("123")
            .notes("Description of refusal")
            .title("Mr")
            .forename("Steve")
            .surname("Jones")
            .telNo("+447890000000")
            .addressLine1("1 High Street")
            .addressLine2("Delph")
            .addressLine3("Oldham")
            .townName("Manchester")
            .postcode("OL3 5DJ")
            .uprn(uprn)
            .region(A_REGION)
            .reason(reason)
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

    // Grab the published event
    ArgumentCaptor<EventType> eventTypeCaptor = ArgumentCaptor.forClass(EventType.class);
    ArgumentCaptor<Source> sourceCaptor = ArgumentCaptor.forClass(Source.class);
    ArgumentCaptor<Channel> channelCaptor = ArgumentCaptor.forClass(Channel.class);
    ArgumentCaptor<RespondentRefusalDetails> refusalEventCaptor =
        ArgumentCaptor.forClass(RespondentRefusalDetails.class);
    verify(eventPublisher)
        .sendEventWithPersistance(
            eventTypeCaptor.capture(),
            sourceCaptor.capture(),
            channelCaptor.capture(),
            refusalEventCaptor.capture());

    assertEquals(REFUSAL_EVENT_TYPE_FIELD_VALUE, eventTypeCaptor.getValue());
    assertEquals(REFUSAL_SOURCE_FIELD_VALUE, sourceCaptor.getValue());
    assertEquals(REFUSAL_CHANNEL_FIELD_VALUE, channelCaptor.getValue());

    // Validate payload of published event
    RespondentRefusalDetails refusal = refusalEventCaptor.getValue();
    assertEquals("Description of refusal", refusal.getReport());
    assertEquals("123", refusal.getAgentId());
    assertEquals(A_CALL_ID, refusal.getCallId());
    assertEquals(expectedEventCaseId, refusal.getCollectionCase().getId());

    verifyRefusalAddress(refusal, uprn);
    assertEquals(reason.name() + "_REFUSAL", refusal.getType());
    Contact expectedContact = new Contact("Mr", "Steve", "Jones", "+447890000000");
    assertEquals(expectedContact, refusal.getContact());
  }

  private void verifyRefusalAddress(
      RespondentRefusalDetails refusal, UniquePropertyReferenceNumber expectedUprn) {
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
