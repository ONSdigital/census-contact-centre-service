package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
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
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseStatus;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseType;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.EstabType;
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
  public void whenLoggedUserRequestsInfoPageThenSuccess()
      throws IllegalStateException, IOException {
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/info", String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().contains("contactcentresvc"));
  }

  @Test
  public void whenAnonymousUserRequestsInfoPageThenSuccess()
      throws IllegalStateException, IOException {
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

  void testGetUACForCase(HttpStatus expectedStatus) throws IllegalStateException, IOException {
    UUID caseId = UUID.randomUUID();
    ResponseEntity<String> response =
        restTemplate.getForEntity(
            base.toString()
                + "/cases/"
                + caseId
                + "/uac?adLocation=12345&individual=false&caseId="
                + caseId,
            String.class);
    assertEquals(expectedStatus, response.getStatusCode());
  }

  void testAccessCasesByUPRN(HttpStatus expectedStatus) throws IllegalStateException, IOException {
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/cases/uprn/123456789012", String.class);

    assertEquals(expectedStatus, response.getStatusCode());
  }

  void testGetAddresses(HttpStatus expectedStatus) throws IllegalStateException, IOException {
    ResponseEntity<String> response =
        restTemplate.getForEntity(
            base.toString() + "/addresses?input=2A%20Priors%20Way", String.class);
    assertEquals(expectedStatus, response.getStatusCode());
  }

  void testGetAddressesPostcode(HttpStatus expectedStatus)
      throws IllegalStateException, IOException {
    ResponseEntity<String> response =
        restTemplate.getForEntity(
            base.toString() + "/addresses/postcode?postcode=EX10 1BD", String.class);
    assertEquals(expectedStatus, response.getStatusCode());
  }

  void testPostCase(HttpStatus expectedStatus) throws IllegalStateException, IOException {
    NewCaseRequestDTO requestBody = new NewCaseRequestDTO();
    requestBody.setCaseType(CaseType.HH);
    requestBody.setDateTime(new Date());
    requestBody.setAddressLine1("1 Contagion Street");
    requestBody.setTownName("Coronaville");
    requestBody.setPostcode("SO22 4HJ");
    requestBody.setRegion(Region.E);

    ResponseEntity<String> response =
        restTemplate.postForEntity(base.toString() + "/cases", requestBody, String.class);
    assertEquals(expectedStatus, response.getStatusCode());
  }

  void testPostRefusal(HttpStatus expectedStatus) throws IllegalStateException, IOException {
    UUID caseId = UUID.randomUUID();
    RefusalRequestDTO requestBody = new RefusalRequestDTO();
    requestBody.setCaseId(caseId.toString());
    requestBody.setReason(Reason.HARD);
    requestBody.setAgentId("12345");
    requestBody.setDateTime(new Date());

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            base.toString() + "/cases/" + caseId + "/refusal", requestBody, String.class);
    assertEquals(expectedStatus, response.getStatusCode());
  }

  void testPutCase(HttpStatus expectedStatus) throws IllegalStateException, IOException {

    UUID caseId = UUID.randomUUID();
    ModifyCaseRequestDTO requestBody = new ModifyCaseRequestDTO();
    requestBody.setCaseId(caseId);
    requestBody.setStatus(CaseStatus.UNCHANGED);
    requestBody.setEstabType(EstabType.HOUSEHOLD);
    requestBody.setDateTime(new Date());
    requestBody.setAddressLine1("1 Contagion Street");
    requestBody.setTownName("Coronaville");
    requestBody.setPostcode("SO22 4HJ");
    requestBody.setRegion(Region.E);

    HttpHeaders headers = new HttpHeaders();
    Map<String, String> param = new HashMap<String, String>();
    HttpEntity<ModifyCaseRequestDTO> requestEntity =
        new HttpEntity<ModifyCaseRequestDTO>(requestBody, headers);
    ResponseEntity<ResponseDTO> response =
        restTemplate.exchange(
            base.toString() + "/cases/" + caseId,
            HttpMethod.PUT,
            requestEntity,
            ResponseDTO.class,
            param);

    assertEquals(expectedStatus, response.getStatusCode());
  }

  void testGetCCSCaseByPostcode(HttpStatus expectedStatus)
      throws IllegalStateException, IOException {
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/cases/ccs/postcode/SO22 4HJ", String.class);
    assertEquals(expectedStatus, response.getStatusCode());
  }

  void testGetCaseByCaseId(HttpStatus expectedStatus) throws IllegalStateException, IOException {
    UUID caseId = UUID.randomUUID();
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/cases/" + caseId, String.class);
    assertEquals(expectedStatus, response.getStatusCode());
  }

  void testGetCaseByCaseRef(HttpStatus expectedStatus) throws IllegalStateException, IOException {
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/cases/ref/123456789", String.class);
    assertEquals(expectedStatus, response.getStatusCode());
  }

  void testGetCaseLaunch(HttpStatus expectedStatus) throws IllegalStateException, IOException {
    UUID caseId = UUID.randomUUID();
    ResponseEntity<String> response =
        restTemplate.getForEntity(
            base.toString() + "/cases/" + caseId + "/launch?individual=false&agentId=12345",
            String.class);
    assertEquals(expectedStatus, response.getStatusCode());
  }

  void testGetFulfilfments(HttpStatus expectedStatus) throws IllegalStateException, IOException {
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/fulfilments", String.class);
    assertEquals(expectedStatus, response.getStatusCode());
  }

  void testPostFulfilmentPost(HttpStatus expectedStatus) throws IllegalStateException, IOException {
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

  void testPostFulfilmentSMS(HttpStatus expectedStatus) throws IllegalStateException, IOException {
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
