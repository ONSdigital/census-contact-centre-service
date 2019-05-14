package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.CaseServiceClientServiceImpl;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.model.CaseDetailsDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/**
 * This class tests the CaseServiceImpl layer. It mocks out the layer below
 * (CaseServiceClientServiceImpl), which would deal with actually sending a HTTP request to the case
 * service.
 */
public class CaseServiceImplTest {

  @Mock CaseServiceClientServiceImpl CaseServiceClientService = new CaseServiceClientServiceImpl();

  @InjectMocks CaseService caseService = new CaseServiceImpl();

  private UUID uuid = UUID.fromString("b7565b5e-1396-4965-91a2-918c0d3642ed");

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testGetCaseByCaseId_withCaseDetails() throws Exception {
    // Build results to be returned from search
    CaseDetailsDTO caseFromCaseService =
        FixtureHelper.loadClassFixtures(CaseDetailsDTO[].class).get(0);
    Mockito.when(CaseServiceClientService.getCaseById(any(), any()))
        .thenReturn(caseFromCaseService);

    // Run the request
    boolean caseEvents = true;
    CaseRequestDTO requestParams = new CaseRequestDTO(caseEvents);
    CaseDTO results = caseService.getCaseById(uuid, requestParams);

    verifyCase(results, caseEvents);
  }

  private void verifyCase(CaseDTO results, boolean caseEvents) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateTimeUtil.DATE_FORMAT_IN_JSON);

    assertEquals(uuid, results.getId());
    assertEquals("1000000000000001", results.getCaseRef());
    assertEquals("H", results.getCaseType());
    assertEquals(
        "2019-05-14T16:11:41.561+01:00",
        LocalDateTime.now().format(formatter)); // results.getCreatedDateTime().format(formatter));
    /**
     * private String caseType;
     *
     * <p>private LocalDateTime createdDateTime;
     *
     * <p>private List<CaseResponseDTO> responses;
     *
     * <p>private List<CaseEventDTO> caseEvents;
     */
  }

  /**
   * Postcode and address queries return the same results, so this method validates the data in both
   * cases.
   *
   * <p>To identify the source of an address these use these constants as a unit or house number
   * suffix: f = formatted n = Nag p = Paf wn = Welsh Nag wp = Welsh Paf
   */
  private void verifyAddresses(AddressQueryResponseDTO results) {
    assertEquals("39", results.getDataVersion());
    assertEquals(23, results.getTotal()); // Total as returned by Address Index

    ArrayList<AddressDTO> addresses = results.getAddresses();
    assertEquals(4, addresses.size());

    // Firstly confirm that Paf addresses take precedence over the others
    assertThat(addresses.get(0).getFormattedAddress(), startsWith("Unit 11p,"));
    assertThat(addresses.get(0).getWelshFormattedAddress(), startsWith("Unit 11wp,"));
    assertEquals("100041045018", addresses.get(0).getUprn());

    // Nag addresses used when there is no Paf address
    assertThat(addresses.get(1).getFormattedAddress(), startsWith("Unit 14n,"));
    assertThat(addresses.get(1).getWelshFormattedAddress(), startsWith("Unit 14wn,"));
    assertEquals("100041045021", addresses.get(1).getUprn());

    // Formatted address used when there is no Paf or Nag address
    assertThat(addresses.get(2).getFormattedAddress(), startsWith("Unit 19f,"));
    assertThat(addresses.get(2).getWelshFormattedAddress(), startsWith("Unit 19f,"));
    assertEquals("100041045024", addresses.get(2).getUprn());

    // Pathological case in which none of the addresses are set
    assertEquals("", addresses.get(3).getFormattedAddress());
    assertEquals("", addresses.get(3).getWelshFormattedAddress());
    assertEquals("100041133344", addresses.get(3).getUprn());
  }
}
