package uk.gov.ons.ctp.integration.contactcentresvc.client;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.util.MultiValueMap;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.rest.RestClient;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AddressIndexSettings;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostcodeQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.addressindex.model.AddressIndexSearchResultsDTO;

public class AddressServiceClientServiceImpleTest {
  @Mock AppConfig appConfig = new AppConfig();

  @Mock RestClient restClient;

  @InjectMocks
  AddressServiceClientServiceImpl addressClientService = new AddressServiceClientServiceImpl();

  @Captor ArgumentCaptor<String> c;

  @Captor ArgumentCaptor<MultiValueMap<String, String>> queryParamsCaptor;

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);

    // Mock the address index settings
    AddressIndexSettings addressIndexSettings = new AddressIndexSettings();
    addressIndexSettings.setAddressQueryPath("/addresses");
    addressIndexSettings.setPostcodeLookupPath("/addresses/postcode");
    Mockito.when(appConfig.getAddressIndexSettings()).thenReturn(addressIndexSettings);
  }

  @Test
  public void testAddressQueryProcessing() throws Exception {
    // Build results to be returned from search
    AddressIndexSearchResultsDTO addressIndexResults =
        FixtureHelper.loadClassFixtures(AddressIndexSearchResultsDTO[].class).get(0);
    Mockito.when(
            restClient.getResource(
                eq("/addresses"), eq(AddressIndexSearchResultsDTO.class), any(), any(), any()))
        .thenReturn(addressIndexResults);

    // Run the request and sanity check the results. We can't thoroughly check the data as it
    // is not coming from a fixed test data set
    AddressQueryRequestDTO request = AddressQueryRequestDTO.create("Michael", 0, 100);
    AddressIndexSearchResultsDTO results = addressClientService.addressQuery(request);
    assertEquals("39", results.getDataVersion());
    assertEquals(4, results.getResponse().getAddresses().size());

    // Verify that the query parameters being passed to AddressIndex are as expected
    Mockito.verify(restClient).getResource(any(), any(), any(), queryParamsCaptor.capture(), any());
    MultiValueMap<String, String> queryParams = queryParamsCaptor.getValue();
    assertEquals("[Michael]", queryParams.get("input").toString());
    assertEquals("[0]", queryParams.get("offset").toString());
    assertEquals("[100]", queryParams.get("limit").toString());
    assertEquals(3, queryParams.keySet().size());
  }

  @Test
  public void testPostcodeQueryProcessing() throws Exception {
    // Build results to be returned from search
    AddressIndexSearchResultsDTO addressIndexResults =
        FixtureHelper.loadClassFixtures(AddressIndexSearchResultsDTO[].class).get(0);
    Mockito.when(
            restClient.getResource(
                eq("/addresses/postcode"),
                eq(AddressIndexSearchResultsDTO.class),
                any(),
                any(),
                eq("EX2 8DD")))
        .thenReturn(addressIndexResults);

    // Run the request and sanity check the results
    PostcodeQueryRequestDTO request = PostcodeQueryRequestDTO.create("EX2 8DD", 0, 100);
    AddressIndexSearchResultsDTO results = addressClientService.postcodeQuery(request);
    assertEquals("39", results.getDataVersion());
    assertEquals(4, results.getResponse().getAddresses().size());

    // Verify that the query parameters being passed to AddressIndex are as expected
    Mockito.verify(restClient).getResource(any(), any(), any(), queryParamsCaptor.capture(), any());
    MultiValueMap<String, String> queryParams = queryParamsCaptor.getValue();
    assertEquals("[0]", queryParams.get("offset").toString());
    assertEquals("[100]", queryParams.get("limit").toString());
    assertEquals(2, queryParams.keySet().size());
  }
}
