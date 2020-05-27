package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import java.io.IOException;
import java.net.URL;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.ons.ctp.integration.contactcentresvc.service.AddressService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test-cc")
@TestPropertySource(properties = {"google-cloud-project=gcp-project"})
public class CCEndpointSecurityTest extends EndpointSecurityTest {

  TestRestTemplate restTemplate;
  URL base;
  @LocalServerPort int port;

  @MockBean CaseService caseService;
  @MockBean AddressService addressService;

  @Test
  public void ccOkGetAddresses() throws IllegalStateException, IOException {
    testGetAddresses(HttpStatus.OK);
  }

  @Test
  public void ccOkGetAddressesPostcode() throws IllegalStateException, IOException {
    testGetAddressesPostcode(HttpStatus.OK);
  }

  @Test
  public void ccOkGetCaseByUPRN() throws IllegalStateException, IOException {
    testAccessCasesByUPRN(HttpStatus.OK);
  }

  @Test
  public void ccOkGetCCSCaseByPostcode() throws IllegalStateException, IOException {
    testGetCCSCaseByPostcode(HttpStatus.OK);
  }

  @Test
  public void ccOkGetCaseByCaseId() throws IllegalStateException, IOException {
    testGetCaseByCaseId(HttpStatus.OK);
  }

  @Test
  public void ccOkGetCaseByCaseRef() throws IllegalStateException, IOException {
    testGetCaseByCaseRef(HttpStatus.OK);
  }

  @Test
  public void ccOkGetCaseLaunch() throws IllegalStateException, IOException {
    testGetCaseLaunch(HttpStatus.OK);
  }

  @Test
  public void ccOkGetFulfilfments() throws IllegalStateException, IOException {
    testGetFulfilfments(HttpStatus.OK);
  }

  @Test
  public void ccOkPutCase() throws IllegalStateException, IOException {
    testPutCase(HttpStatus.OK);
  }

  @Test
  public void ccOkPostCase() throws IllegalStateException, IOException {
    testPostCase(HttpStatus.OK);
  }

  @Test
  public void ccOkPostRefusal() throws IllegalStateException, IOException {
    testPostRefusal(HttpStatus.OK);
  }

  @Test
  public void ccOkPostFulfilfmentPost() throws IllegalStateException, IOException {
    testPostFulfilmentPost(HttpStatus.OK);
  }

  @Test
  public void ccOkPostFulfilfmentSMS() throws IllegalStateException, IOException {
    testPostFulfilmentSMS(HttpStatus.OK);
  }

  @Test
  public void ccForbiddenGetUACForCase() throws IllegalStateException, IOException {
    testGetUACForCase(HttpStatus.FORBIDDEN);
  }
}
