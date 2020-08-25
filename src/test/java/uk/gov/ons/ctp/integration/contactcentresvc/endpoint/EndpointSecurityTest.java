package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.AN_ADDRESS_LINE_1;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.AN_ADDRESS_LINE_2;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.AN_ADDRESS_LINE_3;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.AN_ESTAB_TYPE;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.A_CASE_STATUS;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.A_CASE_TYPE;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.A_REQUEST_DATE_TIME;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.SOME_NOTES;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_0;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.InvalidateCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ModifyCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.NewCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.Reason;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RefusalRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.Region;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SMSFulfilmentRequestDTO;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"GOOGLE_CLOUD_PROJECT=census-cc-test"})
public abstract class EndpointSecurityTest {

  TestRestTemplate restTemplate;
  URL base;
  @LocalServerPort int port;

  @Before
  public void setUp() throws MalformedURLException {
    restTemplate = new TestRestTemplate("serco_cks", "temporary");
    base = new URL("http://localhost:" + port);
  }

  @Test
  public void whenLoggedUserRequestsInfoPageThenSuccess() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/info", String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().contains("contactcentresvc"));
  }

  @Test
  public void whenAnonymousUserRequestsInfoPageThenSuccess() {
    restTemplate = new TestRestTemplate();
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/info", String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().contains("contactcentresvc"));
  }

  @Test
  public void whenUserWithWrongCredentialsRequestsVersionThenUnauthorizedPage() throws Exception {

    restTemplate = new TestRestTemplate("user", "wrongpassword");
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/version", String.class);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertTrue(response.getBody().contains("Unauthorized"));
  }

  @Test
  public void whenUserWithCorrectCredentialsRequestsVersionThenSuccess() throws Exception {

    restTemplate = new TestRestTemplate("serco_cks", "temporary");
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/version", String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  void testGetUACForCase(HttpStatus expectedStatus) {
    UUID caseId = UUID.randomUUID();
    ResponseEntity<String> response =
        restTemplate.getForEntity(
            base.toString() + "/cases/" + caseId + "/uac?adLocationId=12345&individual=false",
            String.class);
    assertEquals(expectedStatus, response.getStatusCode());
  }

  void testAccessCasesByUPRN(HttpStatus expectedStatus) {
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/cases/uprn/123456789012", String.class);

    assertEquals(expectedStatus, response.getStatusCode());
  }

  void testGetAddresses(HttpStatus expectedStatus) {
    ResponseEntity<String> response =
        restTemplate.getForEntity(
            base.toString() + "/addresses?input=2A%20Priors%20Way", String.class);
    assertEquals(expectedStatus, response.getStatusCode());
  }

  void testGetAddressesPostcode(HttpStatus expectedStatus) {
    ResponseEntity<String> response =
        restTemplate.getForEntity(
            base.toString() + "/addresses/postcode?postcode=EX10 1BD", String.class);
    assertEquals(expectedStatus, response.getStatusCode());
  }

  void testPostCase(HttpStatus expectedStatus) {
    NewCaseRequestDTO requestBody = new NewCaseRequestDTO();
    requestBody.setCaseType(CaseType.HH);
    requestBody.setDateTime(new Date());
    requestBody.setAddressLine1("1 Contagion Street");
    requestBody.setTownName("Coronaville");
    requestBody.setPostcode("SO22 4HJ");
    requestBody.setRegion(Region.E);
    requestBody.setEstabType(EstabType.GATED_APARTMENTS);

    ResponseEntity<String> response =
        restTemplate.postForEntity(base.toString() + "/cases", requestBody, String.class);
    assertEquals(expectedStatus, response.getStatusCode());
  }

  void testPostRefusal(HttpStatus expectedStatus) {
    UUID caseId = UUID.randomUUID();
    RefusalRequestDTO requestBody = new RefusalRequestDTO();
    requestBody.setCaseId(caseId);
    requestBody.setReason(Reason.HARD);
    requestBody.setAgentId(12345);
    requestBody.setCallId("8989-NOW");
    requestBody.setIsHouseholder(false);
    requestBody.setDateTime(new Date());

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            base.toString() + "/cases/" + caseId + "/refusal", requestBody, String.class);
    assertEquals(expectedStatus, response.getStatusCode());
  }

  void testPostInvalidateCase(HttpStatus expectedStatus) {
    InvalidateCaseRequestDTO requestBody =
        InvalidateCaseRequestDTO.builder()
            .caseId(UUID_0)
            .status(A_CASE_STATUS)
            .notes(SOME_NOTES)
            .dateTime(A_REQUEST_DATE_TIME)
            .build();

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            base.toString() + "/cases/" + requestBody.getCaseId() + "/invalidate",
            requestBody,
            String.class);
    assertEquals(expectedStatus, response.getStatusCode());
  }

  void testPutCase(HttpStatus expectedStatus) {
    ModifyCaseRequestDTO requestBody = ModifyCaseRequestDTO.builder().caseId(UUID_0).build();

    requestBody.setAddressLine1(AN_ADDRESS_LINE_1);
    requestBody.setAddressLine2(AN_ADDRESS_LINE_2);
    requestBody.setAddressLine3(AN_ADDRESS_LINE_3);
    requestBody.setDateTime(A_REQUEST_DATE_TIME);
    requestBody.setCaseType(A_CASE_TYPE);
    requestBody.setEstabType(AN_ESTAB_TYPE);

    HttpHeaders headers = new HttpHeaders();
    Map<String, String> param = new HashMap<String, String>();
    HttpEntity<ModifyCaseRequestDTO> requestEntity =
        new HttpEntity<ModifyCaseRequestDTO>(requestBody, headers);
    ResponseEntity<ResponseDTO> response =
        restTemplate.exchange(
            base.toString() + "/cases/" + requestBody.getCaseId(),
            HttpMethod.PUT,
            requestEntity,
            ResponseDTO.class,
            param);

    assertEquals(expectedStatus, response.getStatusCode());
  }

  void testGetCCSCaseByPostcode(HttpStatus expectedStatus) {
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/cases/ccs/postcode/SO22 4HJ", String.class);
    assertEquals(expectedStatus, response.getStatusCode());
  }

  void testGetCaseByCaseId(HttpStatus expectedStatus) {
    UUID caseId = UUID.randomUUID();
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/cases/" + caseId, String.class);
    assertEquals(expectedStatus, response.getStatusCode());
  }

  void testGetCaseByCaseRef(HttpStatus expectedStatus) {
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/cases/ref/123456789", String.class);
    assertEquals(expectedStatus, response.getStatusCode());
  }

  void testGetCaseLaunch(HttpStatus expectedStatus) {
    UUID caseId = UUID.randomUUID();
    ResponseEntity<String> response =
        restTemplate.getForEntity(
            base.toString() + "/cases/" + caseId + "/launch?individual=false&agentId=12345",
            String.class);
    assertEquals(expectedStatus, response.getStatusCode());
  }

  void testGetFulfilfments(HttpStatus expectedStatus) {
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/fulfilments", String.class);
    assertEquals(expectedStatus, response.getStatusCode());
  }

  void testPostFulfilmentPost(HttpStatus expectedStatus) {
    UUID caseId = UUID.randomUUID();
    PostalFulfilmentRequestDTO requestBody = new PostalFulfilmentRequestDTO();
    requestBody.setDateTime(new Date());
    requestBody.setCaseId(caseId);
    requestBody.setFulfilmentCode("ABC123");

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            base.toString() + "/cases/" + caseId + "/fulfilment/post", requestBody, String.class);
    assertEquals(expectedStatus, response.getStatusCode());
  }

  void testPostFulfilmentSMS(HttpStatus expectedStatus) {
    UUID caseId = UUID.randomUUID();
    SMSFulfilmentRequestDTO requestBody = new SMSFulfilmentRequestDTO();
    requestBody.setDateTime(new Date());
    requestBody.setCaseId(caseId);
    requestBody.setFulfilmentCode("ABC123");
    requestBody.setTelNo("447123456789");

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            base.toString() + "/cases/" + caseId + "/fulfilment/sms", requestBody, String.class);
    assertEquals(expectedStatus, response.getStatusCode());
  }
}
