package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.integration.contactcentresvc.client.AddressServiceClientServiceImpl;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostcodeQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.AddressService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.addressindex.model.AddressIndexSearchResultsDTO;

public class AddressServiceImplTest {

  @Mock AddressServiceClientServiceImpl addressClientService = new AddressServiceClientServiceImpl();

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
    Mockito.when(addressClientService.addressQuery(any()))
        .thenReturn(addressIndexResults);

    // Run the request and verify results
    AddressQueryRequestDTO request = new AddressQueryRequestDTO("Michael", 0, 100);
    AddressQueryResponseDTO results = addressService.addressQuery(request);
    verifyAddresses(results);
  }

  @Test
  public void testPostcodeQueryProcessing() throws Exception {
    // Build results to be returned from search
    AddressIndexSearchResultsDTO addressIndexResults =
        FixtureHelper.loadClassFixtures(AddressIndexSearchResultsDTO[].class).get(0);
    Mockito.when(addressClientService.postcodeQuery(any()))
        .thenReturn(addressIndexResults);

    // Run the request and verify results
    PostcodeQueryRequestDTO request = new PostcodeQueryRequestDTO("EX2 8DD", 0, 100);
    AddressQueryResponseDTO results = addressService.postcodeQuery(request);
    verifyAddresses(results);
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
