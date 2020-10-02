package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.A_REGION;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
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
import uk.gov.ons.ctp.integration.contactcentresvc.util.PgpDecrypt;
import uk.gov.ons.ctp.integration.contactcentresvc.util.PgpEncryptTest;

/** Unit Test {@link CaseService#reportRefusal(UUID, RefusalRequestDTO) reportRefusal}. */
@RunWith(MockitoJUnitRunner.class)
public class CaseServiceImplReportRefusalTest extends CaseServiceImplTestBase {
  private static final String A_CALL_ID = "8989-NOW";
  private static final String A_UPRN = "1234";
  private static final String PUBLIC_KEY_1 = "pgp/key1.asc";
  private static final String PUBLIC_KEY_2 = "pgp/key2.asc";
  private static final String PRIVATE_KEY_1 = "pgp/priv-key1.asc";
  private static final String PRIVATE_KEY_2 = "pgp/priv-key2.asc";

  @Before
  public void setup() {
    when(appConfig.getChannel()).thenReturn(Channel.CC);

    Resource pubKey1 = new ClassPathResource(PUBLIC_KEY_1);
    Resource pubKey2 = new ClassPathResource(PUBLIC_KEY_2);
    when(appConfig.getPublicPgpKey1()).thenReturn(pubKey1);
    when(appConfig.getPublicPgpKey2()).thenReturn(pubKey2);
  }

  @Test
  public void testRespondentRefusal_withExtraordinaryReason() throws Exception {
    Date dateTime = new Date();
    doRespondentRefusalTest(dateTime, Reason.EXTRAORDINARY, createContact());
  }

  @Test
  public void testRespondentRefusal_withHardReason() throws Exception {
    Date dateTime = new Date();
    doRespondentRefusalTest(dateTime, Reason.HARD, createContact());
  }

  @Test
  public void testRespondentRefusal_withEmptyContactFields() throws Exception {
    Date dateTime = new Date();
    ContactCompact c = new ContactCompact("", "", "");
    doRespondentRefusalTest(dateTime, Reason.HARD, c);
  }

  @Test
  public void testRespondentRefusal_withNullContactFields() throws Exception {
    Date dateTime = new Date();
    ContactCompact c = new ContactCompact();
    doRespondentRefusalTest(dateTime, Reason.HARD, c);
  }

  @Test
  public void testRespondentRefusal_withUUID() throws Exception {
    Date dateTime = new Date();
    doRespondentRefusalTest(dateTime, Reason.EXTRAORDINARY, createContact());
  }

  @Test
  public void testRespondentRefusal_withoutDateTime() throws Exception {
    Date dateTime = null;
    doRespondentRefusalTest(dateTime, Reason.EXTRAORDINARY, createContact());
  }

  private ContactCompact createContact() {
    ContactCompact c = new ContactCompact("Mr", "Steve", "Jones");
    return c;
  }

  private RefusalRequestDTO createRefusalDto(
      UUID caseId, Date dateTime, Reason reason, ContactCompact contact) {
    UniquePropertyReferenceNumber uprn = new UniquePropertyReferenceNumber(A_UPRN);
    return RefusalRequestDTO.builder()
        .caseId(caseId)
        .agentId(123)
        .title(contact.getTitle())
        .forename(contact.getForename())
        .surname(contact.getSurname())
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
  }

  private void doRespondentRefusalTest(Date dateTime, Reason reason, ContactCompact contact)
      throws Exception {
    UUID caseId = UUID.randomUUID();
    UUID expectedEventCaseId = caseId;
    String expectedResponseCaseId = caseId.toString();
    RefusalRequestDTO refusalPayload =
        createRefusalDto(expectedEventCaseId, dateTime, reason, contact);

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
    if (Reason.EXTRAORDINARY.equals(reason)) {
      assertNull(refusal.getContact());
    } else {
      ContactCompact c = refusal.getContact();
      verifyEncryptedField(contact.getTitle(), c.getTitle());
      verifyEncryptedField(contact.getForename(), c.getForename());
      verifyEncryptedField(contact.getSurname(), c.getSurname());
    }
  }

  private void verifyEncryptedField(String clear, String sendField) throws Exception {
    if (clear == null) {
      assertNull(sendField);
      return;
    }
    String privKey = PgpEncryptTest.readFileIntoString(PRIVATE_KEY_1);
    String pgpField = new String(Base64.getDecoder().decode(sendField), StandardCharsets.UTF_8);
    try (ByteArrayInputStream secretKeyFile = new ByteArrayInputStream(privKey.getBytes())) {
      String decrypted =
          PgpDecrypt.decrypt(secretKeyFile, pgpField, PgpEncryptTest.PASS_PHRASE.toCharArray());
      assertEquals(clear, decrypted);
    }

    String privKey2 = PgpEncryptTest.readFileIntoString(PRIVATE_KEY_2);
    String pgpField2 = new String(Base64.getDecoder().decode(sendField), StandardCharsets.UTF_8);
    try (ByteArrayInputStream secretKeyFile = new ByteArrayInputStream(privKey2.getBytes())) {
      String decrypted =
          PgpDecrypt.decrypt(secretKeyFile, pgpField2, PgpEncryptTest.PASS_PHRASE2.toCharArray());
      assertEquals(clear, decrypted);
    }
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
