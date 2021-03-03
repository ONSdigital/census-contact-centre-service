package uk.gov.ons.ctp.integration.contactcentresvc.client.addressIndex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.rest.RestClient;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.AddressServiceClientServiceImpl;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexSearchResultsCompositeDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexSearchResultsDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AddressIndexSettings;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostcodeQueryRequestDTO;

public class AddressServiceClientServiceImplTest {

  private static final String ADDRESS_QUERY_PATH = "/addresses";
  private static final String POSTCODE_QUERY_PATH = "/addresses/postcode";
  private static final String UPRN_QUERY_PATH = "/addresses/rh/uprn/{uprn}";
  private static final String ADDRESS_TYPE = "paf";
  private static final long UPRN = 100041045018L;
  private static final String EPOCH = "99";

  @Mock AppConfig appConfig = new AppConfig();

  @Mock RestClient restClient;

  @InjectMocks
  AddressServiceClientServiceImpl addressClientService = new AddressServiceClientServiceImpl();

  @Captor ArgumentCaptor<String> c;

  @Captor ArgumentCaptor<MultiValueMap<String, String>> queryParamsCaptor;

  private AddressIndexSettings addressIndexSettings;

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
    addressIndexSettings = new AddressIndexSettings();
    // Mock the address index settings
    addressIndexSettings.setAddressQueryPath(ADDRESS_QUERY_PATH);
    addressIndexSettings.setPostcodeLookupPath(POSTCODE_QUERY_PATH);
    addressIndexSettings.setUprnLookupPath(UPRN_QUERY_PATH);
    addressIndexSettings.setAddressType(ADDRESS_TYPE);
    addressIndexSettings.setEpoch(EPOCH);
    Mockito.when(appConfig.getAddressIndexSettings()).thenReturn(addressIndexSettings);
  }

  @Test
  public void testAddressQueryProcessing() throws Exception {
    // Build results to be returned from search
    AddressIndexSearchResultsDTO resultsFromAddressIndex =
        FixtureHelper.loadClassFixtures(AddressIndexSearchResultsDTO[].class).get(0);
    Mockito.when(
            restClient.getResource(
                eq(ADDRESS_QUERY_PATH),
                eq(AddressIndexSearchResultsDTO.class),
                any(),
                any(),
                any()))
        .thenReturn(resultsFromAddressIndex);

    // Run the request and sanity check the results. We can't thoroughly check the data as it
    // is not coming from a fixed test data set
    AddressQueryRequestDTO request = AddressQueryRequestDTO.create("Michael", 0, 100);
    AddressIndexSearchResultsDTO results = addressClientService.searchByAddress(request);
    assertEquals("39", results.getDataVersion());
    assertEquals(4, results.getResponse().getAddresses().size());

    // Verify that the query parameters being passed to AddressIndex are as expected
    Mockito.verify(restClient).getResource(any(), any(), any(), queryParamsCaptor.capture(), any());
    MultiValueMap<String, String> queryParams = queryParamsCaptor.getValue();
    assertEquals("[Michael]", queryParams.get("input").toString());
    assertEquals("[0]", queryParams.get("offset").toString());
    assertEquals("[100]", queryParams.get("limit").toString());
    assertEquals("[false]", queryParams.get("historical").toString());
    assertEquals("[99]", queryParams.get("epoch").toString());
    assertEquals("[true]", queryParams.get("includeauxiliarysearch").toString());
    assertEquals("[0]", queryParams.get("matchthreshold").toString());
    assertEquals(7, queryParams.keySet().size());
  }

  @Test
  public void testAddressQueryProcessingNoEpoch() throws Exception {
    addressIndexSettings.setEpoch("");
    // Build results to be returned from search
    AddressIndexSearchResultsDTO resultsFromAddressIndex =
        FixtureHelper.loadClassFixtures(AddressIndexSearchResultsDTO[].class).get(0);
    Mockito.when(
            restClient.getResource(
                eq(ADDRESS_QUERY_PATH),
                eq(AddressIndexSearchResultsDTO.class),
                any(),
                any(),
                any()))
        .thenReturn(resultsFromAddressIndex);

    // Run the request and sanity check the results. We can't thoroughly check the data as it
    // is not coming from a fixed test data set
    AddressQueryRequestDTO request = AddressQueryRequestDTO.create("Michael", 0, 100);
    AddressIndexSearchResultsDTO results = addressClientService.searchByAddress(request);
    assertEquals("39", results.getDataVersion());
    assertEquals(4, results.getResponse().getAddresses().size());

    // Verify that the query parameters being passed to AddressIndex are as expected
    Mockito.verify(restClient).getResource(any(), any(), any(), queryParamsCaptor.capture(), any());
    MultiValueMap<String, String> queryParams = queryParamsCaptor.getValue();
    assertEquals("[Michael]", queryParams.get("input").toString());
    assertEquals("[0]", queryParams.get("offset").toString());
    assertEquals("[100]", queryParams.get("limit").toString());
    assertEquals("[false]", queryParams.get("historical").toString());
    assertEquals("[true]", queryParams.get("includeauxiliarysearch").toString());
    assertEquals("[0]", queryParams.get("matchthreshold").toString());
    assertEquals(6, queryParams.keySet().size());
  }

  @Test
  public void testAddressQuerylessThan5CharactersThrowCTPException() throws CTPException {
    AddressQueryRequestDTO addressQuery = new AddressQueryRequestDTO();
    addressQuery.setInput("W'O 'W");

    CTPException exception = assertThrows("Expected CTPException to be Thrown",
            CTPException.class,
            () -> addressClientService.searchByAddress(addressQuery));

    assertEquals("Address query requires 5 or more characters, not including single quotes or trailing whitespaces",
            exception.getMessage());
  }

  @Test
  public void testPostcodeQueryProcessing() throws Exception {
    // Build results to be returned from search
    AddressIndexSearchResultsDTO resultsFromAddressIndex =
        FixtureHelper.loadClassFixtures(AddressIndexSearchResultsDTO[].class).get(0);
    Mockito.when(
            restClient.getResource(
                eq(POSTCODE_QUERY_PATH),
                eq(AddressIndexSearchResultsDTO.class),
                any(),
                any(),
                eq("EX2 8DD")))
        .thenReturn(resultsFromAddressIndex);

    // Run the request and sanity check the results
    PostcodeQueryRequestDTO request = PostcodeQueryRequestDTO.create("EX2 8DD", 0, 100);
    AddressIndexSearchResultsDTO results = addressClientService.searchByPostcode(request);
    assertEquals("39", results.getDataVersion());
    assertEquals(4, results.getResponse().getAddresses().size());

    // Verify that the query parameters being passed to AddressIndex are as expected
    Mockito.verify(restClient).getResource(any(), any(), any(), queryParamsCaptor.capture(), any());
    MultiValueMap<String, String> queryParams = queryParamsCaptor.getValue();
    assertEquals("[0]", queryParams.get("offset").toString());
    assertEquals("[100]", queryParams.get("limit").toString());
    assertEquals("[99]", queryParams.get("epoch").toString());
    assertEquals("[true]", queryParams.get("includeauxiliarysearch").toString());
    assertEquals(4, queryParams.keySet().size());
  }

  @Test
  public void testUPRNQueryProcessing() throws Exception {
    // Build results to be returned from search
    AddressIndexSearchResultsCompositeDTO resultsFromAddressIndex =
        FixtureHelper.loadMethodFixtures(AddressIndexSearchResultsCompositeDTO[].class).get(0);
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("addresstype", ADDRESS_TYPE);
    queryParams.add("epoch", EPOCH);
    Mockito.when(
            restClient.getResource(
                eq(UPRN_QUERY_PATH),
                eq(AddressIndexSearchResultsCompositeDTO.class),
                Mockito.isNull(),
                eq(queryParams),
                eq(Long.toString(UPRN))))
        .thenReturn(resultsFromAddressIndex);

    AddressIndexSearchResultsCompositeDTO results = addressClientService.searchByUPRN(UPRN);
    assertEquals("72", results.getDataVersion());
    assertEquals(Long.toString(UPRN), results.getResponse().getAddress().getUprn());
    assertEquals("39 Sandford Walk", results.getResponse().getAddress().getAddressLine1());
    assertEquals("", results.getResponse().getAddress().getAddressLine2());
    assertEquals("", results.getResponse().getAddress().getAddressLine3());
    assertEquals("Exeter", results.getResponse().getAddress().getTownName());
    assertEquals("EX1 2ET", results.getResponse().getAddress().getPostcode());
    assertEquals("HH", results.getResponse().getAddress().getCensusAddressType());
    assertEquals("Household", results.getResponse().getAddress().getCensusEstabType());
    assertEquals("E", results.getResponse().getAddress().getCountryCode());
  }
}
