package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

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
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.CaseDataRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.service.AddressService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test-cc")
public class CCEndpointSecurityTest extends EndpointSecurityTest {

  TestRestTemplate restTemplate;
  URL base;
  @LocalServerPort int port;

  @MockBean CaseService caseService;
  @MockBean AddressService addressService;
  @MockBean CaseDataRepository caseDataRepository;

  @Test
  public void ccOkGetAddresses() {
    testGetAddresses(HttpStatus.OK);
  }

  @Test
  public void ccOkGetAddressesPostcode() {
    testGetAddressesPostcode(HttpStatus.OK);
  }

  @Test
  public void ccOkGetCaseByUPRN() {
    testAccessCasesByUPRN(HttpStatus.OK);
  }

  @Test
  public void ccOkGetCCSCaseByPostcode() {
    testGetCCSCaseByPostcode(HttpStatus.OK);
  }

  @Test
  public void ccOkGetCaseByCaseId() {
    testGetCaseByCaseId(HttpStatus.OK);
  }

  @Test
  public void ccOkGetCaseByCaseRef() {
    testGetCaseByCaseRef(HttpStatus.OK);
  }

  @Test
  public void ccOkGetCaseLaunch() {
    testGetCaseLaunch(HttpStatus.OK);
  }

  @Test
  public void ccOkGetFulfilfments() {
    testGetFulfilfments(HttpStatus.OK);
  }

  @Test
  public void ccOkPutCase() {
    testPutCase(HttpStatus.OK);
  }

  @Test
  public void ccOkPostCase() {
    testPostCase(HttpStatus.OK);
  }

  @Test
  public void ccOkPostRefusal() {
    testPostRefusal(HttpStatus.OK);
  }

  @Test
  public void ccOkPostInvalidate() {
    testPostInvalidateCase(HttpStatus.OK);
  }

  @Test
  public void ccOkPostFulfilfmentPost() {
    testPostFulfilmentPost(HttpStatus.OK);
  }

  @Test
  public void ccOkPostFulfilfmentSMS() {
    testPostFulfilmentSMS(HttpStatus.OK);
  }

  @Test
  public void ccForbiddenGetUACForCase() {
    testGetUACForCase(HttpStatus.FORBIDDEN);
  }
}
