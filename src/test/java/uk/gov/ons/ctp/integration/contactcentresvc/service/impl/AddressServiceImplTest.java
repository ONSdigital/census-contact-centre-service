package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.AddressServiceClientServiceImpl;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexAddressCompositeDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexSearchResultsCompositeDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexSearchResultsDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostcodeQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.AddressService;

public class AddressServiceImplTest {

  @Mock
  AddressServiceClientServiceImpl addressClientService = new AddressServiceClientServiceImpl();

  @InjectMocks AddressService addressService = new AddressServiceImpl();

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testAddressQueryProcessing() throws Exception {
    // Build results to be returned from search
    AddressIndexSearchResultsDTO addressIndexResults =
        FixtureHelper.loadClassFixtures(AddressIndexSearchResultsDTO[].class).get(0);
    Mockito.when(addressClientService.searchByAddress(any())).thenReturn(addressIndexResults);

    // Run the request and verify results
    AddressQueryRequestDTO request = AddressQueryRequestDTO.create("Michael", 0, 100);
    AddressQueryResponseDTO results = addressService.addressQuery(request);
    verifyAddresses(results);
  }

  @Test
  public void testPostcodeQueryProcessing() throws Exception {
    // Build results to be returned from search
    AddressIndexSearchResultsDTO addressIndexResults =
        FixtureHelper.loadClassFixtures(AddressIndexSearchResultsDTO[].class).get(0);
    Mockito.when(addressClientService.searchByPostcode(any())).thenReturn(addressIndexResults);

    // Run the request and verify results
    PostcodeQueryRequestDTO request = PostcodeQueryRequestDTO.create("EX2 8DD", 0, 100);
    AddressQueryResponseDTO results = addressService.postcodeQuery(request);
    verifyAddresses(results);
  }

  @Test
  public void testUPRNQueryProcessing() throws Exception {
    // Build results to be returned from search
    AddressIndexSearchResultsCompositeDTO addressIndexResults =
        FixtureHelper.loadMethodFixtures(AddressIndexSearchResultsCompositeDTO[].class).get(0);
    Mockito.when(addressClientService.searchByUPRN(any())).thenReturn(addressIndexResults);

    // Run the request and verify results
    AddressIndexAddressCompositeDTO address = addressService.uprnQuery(100041045018L);

    assertEquals("100041045018", address.getUprn());
    assertEquals("39 Sandford Walk", address.getAddressLine1());
    assertEquals("", address.getAddressLine2());
    assertEquals("", address.getAddressLine3());
    assertEquals("Exeter", address.getTownName());
    assertEquals("EX1 2ET", address.getPostcode());
  }

  @Test(expected = CTPException.class)
  public void testUPRNQueryJSONStatusNot200() throws Exception {
    // Build results to be returned from search
    final String message = "Server too busy";
    final int status = 429;
    AddressIndexSearchResultsCompositeDTO addressIndexResults =
        FixtureHelper.loadMethodFixtures(AddressIndexSearchResultsCompositeDTO[].class).get(0);
    addressIndexResults.getStatus().setCode(status);
    addressIndexResults.getStatus().setMessage(message);
    Mockito.when(addressClientService.searchByUPRN(any())).thenReturn(addressIndexResults);

    addressService.uprnQuery(100041045018L);
  }

  @Test(expected = ResponseStatusException.class)
  public void testUPRNQueryRestClientException() throws Exception {

    Mockito.doThrow(new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT))
        .when(addressClientService)
        .searchByUPRN(100041045018L);

    addressService.uprnQuery(100041045018L);
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
    assertEquals("E", addresses.get(0).getRegion());
    assertEquals("HH", addresses.get(0).getAddressType());
    assertEquals("HOUSEHOLD", addresses.get(0).getEstabType());
    assertEquals("HOUSEHOLD", addresses.get(0).getEstabDescription());

    // Nag addresses used when there is no Paf address
    assertThat(addresses.get(1).getFormattedAddress(), startsWith("Unit 14n,"));
    assertThat(addresses.get(1).getWelshFormattedAddress(), startsWith("Unit 14wn,"));
    assertEquals("100041045021", addresses.get(1).getUprn());
    assertEquals("E", addresses.get(1).getRegion());
    assertEquals("CE", addresses.get(1).getAddressType());
    assertEquals("CARE_HOME", addresses.get(1).getEstabType());
    assertEquals("CARE HOME", addresses.get(1).getEstabDescription());

    // Formatted address used when there is no Paf or Nag address
    assertThat(addresses.get(2).getFormattedAddress(), startsWith("Unit 19f,"));
    assertThat(addresses.get(2).getWelshFormattedAddress(), startsWith("Unit 19f,"));
    assertEquals("100041045024", addresses.get(2).getUprn());
    assertEquals("E", addresses.get(2).getRegion());
    assertEquals("SPG", addresses.get(2).getAddressType());
    assertEquals("PRISON", addresses.get(2).getEstabType());
    assertEquals("PRISON", addresses.get(2).getEstabDescription());

    // Pathological case in which none of the addresses are set
    assertEquals("", addresses.get(3).getFormattedAddress());
    assertEquals("", addresses.get(3).getWelshFormattedAddress());
    assertEquals("100041133344", addresses.get(3).getUprn());
    assertEquals("E", addresses.get(3).getRegion());
    assertEquals("HH", addresses.get(3).getAddressType());
    assertEquals("OTHER", addresses.get(3).getEstabType()); // Test uses an unknown estab type
    assertEquals("OLD RAILWAY CARRIAGE", addresses.get(3).getEstabDescription());
  }
}
