package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.AddressType;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.CollectionCaseNewAddress;
import uk.gov.ons.ctp.common.event.model.NewAddress;
import uk.gov.ons.ctp.common.rest.RestClient;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexAddressDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexSearchResultsDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AddressIndexSettings;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.DeliveryChannel;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.NewCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.Region;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/**
 * Unit Test {@link CaseService#createCaseForNewAddress(NewCaseRequestDTO) createCaseForNewAddress}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CaseServiceImplCreateCaseForNewAddressTest extends CaseServiceImplTestBase {

  // the actual census name & id as per the application.yml and also RM
  private static final String SURVEY_NAME = "CENSUS";
  private static final String COLLECTION_EXERCISE_ID = "34d7f3bb-91c9-45d0-bb2d-90afce4fc790";

  private static final String POSTCODE_QUERY_PATH = "/addresses/postcode";
  private static final String EPOCH = "";

  @Mock RestClient restClient;

  @Before
  public void setup() {
    Mockito.when(appConfig.getChannel()).thenReturn(Channel.CC);
    Mockito.when(appConfig.getSurveyName()).thenReturn(SURVEY_NAME);
    Mockito.when(appConfig.getCollectionExerciseId()).thenReturn(COLLECTION_EXERCISE_ID);
    AddressIndexSettings addressIndexSettings = new AddressIndexSettings();
    // Mock the address index settings
    addressIndexSettings.setPostcodeLookupPath(POSTCODE_QUERY_PATH);
    addressIndexSettings.setEpoch(EPOCH);
    Mockito.when(appConfig.getAddressIndexSettings()).thenReturn(addressIndexSettings);
  }

  @Test
  public void testNewCaseForNewAddress() throws Exception {
    setupMockAIPostcodeQuery("E");

    NewCaseRequestDTO caseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(0);

    doTestNewCaseForNewAddress(caseRequestDTO, "SPG", Region.E, true);
  }

  @Test
  public void testNewCaseForNewAddress_forEstabTypeOfOther() throws Exception {
    setupMockAIPostcodeQuery("E");

    // Load request, which has estabType of Other
    NewCaseRequestDTO caseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(1);

    // Address type will be that for EstabType.Other
    doTestNewCaseForNewAddress(caseRequestDTO, "HH", Region.E, false);
  }

  @Test
  public void testNewCaseForNewAddress_mismatchedCaseAndAddressType() {
    // Load request, which has caseType of HH and estabType with a CE addressType
    NewCaseRequestDTO caseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(2);

    try {
      doTestNewCaseForNewAddress(caseRequestDTO, null, Region.E, false);
      fail();
    } catch (CTPException e) {
      assertEquals(Fault.BAD_REQUEST, e.getFault());
      assertTrue(
          e.toString(),
          e.getMessage()
              .matches(".* address type .*CE.* from .*MILITARY_SLA.* not compatible .*HH.*"));
    }
  }

  @Test
  public void testNewCaseForNewAddress_ceWithNonPositiveNumberOfResidents() {
    // Load valid request and then update so that it's invalid
    NewCaseRequestDTO caseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(2);
    // Simulate error by making request a CE with a non-positive number of residents
    caseRequestDTO.setCaseType(CaseType.CE);
    caseRequestDTO.setCeUsualResidents(0);

    setupMockAIPostcodeQuery("E");

    try {
      doTestNewCaseForNewAddress(caseRequestDTO, null, Region.E, false);
      fail();
    } catch (CTPException e) {
      assertEquals(Fault.BAD_REQUEST, e.getFault());
      assertTrue(e.toString(), e.getMessage().matches(".*Number of residents .* for CE .*"));
    }
  }

  @Test
  public void testNewCaseForNewAddress_ceWithNullNumberOfResidents() {
    // Load valid request and then update so that it's invalid
    NewCaseRequestDTO caseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(2);
    // Simulate error by making request a CE with a non-positive number of residents
    caseRequestDTO.setCaseType(CaseType.CE);
    caseRequestDTO.setCeUsualResidents(null);

    setupMockAIPostcodeQuery("E");

    try {
      doTestNewCaseForNewAddress(caseRequestDTO, null, Region.E, false);
      fail();
    } catch (CTPException e) {
      assertEquals(Fault.BAD_REQUEST, e.getFault());
      assertTrue(e.toString(), e.getMessage().matches(".*Number of residents .* for CE .*"));
    }
  }

  @Test
  public void testNewCaseForNewAddress_cePositiveNumberOfResidents() throws Exception {
    // Test that the check for a CE with non zero number residents is correct
    NewCaseRequestDTO caseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(2);
    // Simulate condition by making request a CE with a positive number of residents
    caseRequestDTO.setCaseType(CaseType.CE);
    caseRequestDTO.setCeUsualResidents(11);

    setupMockAIPostcodeQuery("E");

    doTestNewCaseForNewAddress(caseRequestDTO, "CE", Region.E, true);
  }

  @Test
  public void testNewCaseForNewAddress_ceCaseTypeNotAllowedIfRegionN() throws Exception {
    // Test that a request for a new case where the caseType is CE and the region is N will be
    // rejected
    NewCaseRequestDTO caseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(0);
    // Simulate condition by making the request a CE with a region of N
    caseRequestDTO.setCaseType(CaseType.CE);
    caseRequestDTO.setRegion(Region.N);

    try {
      doTestNewCaseForNewAddress(caseRequestDTO, "CE", Region.E, true);
      fail();
    } catch (CTPException e) {
      assertEquals(Fault.BAD_REQUEST, e.getFault());
      assertTrue(
          e.toString(),
          e.getMessage()
              .matches(
                  "All queries relating to Communal Establishments "
                      + "in Northern Ireland should be escalated to NISRA HQ"));
    }
  }

  @Test
  public void testNewCaseForNewAddress_rejectGuernseyPostcode() throws Exception {
    rejectCrownDependencyPostcodes("GY1 6AB", "Channel Island addresses are not valid for Census");
  }

  @Test
  public void testNewCaseForNewAddress_rejectJerseyPostcode() throws Exception {
    rejectCrownDependencyPostcodes("JE1 6AB", "Channel Island addresses are not valid for Census");
  }

  @Test
  public void testNewCaseForNewAddress_rejectIsleOfManPostcode() throws Exception {
    rejectCrownDependencyPostcodes("IM8 6AB", "Isle of Man addresses are not valid for Census");
  }

  public void rejectCrownDependencyPostcodes(String postcode, String expectedErrorMessage)
      throws Exception {

    // Test that a request for a new case where the caseType is CE and the region is N will be
    // rejected
    NewCaseRequestDTO caseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(0);
    caseRequestDTO.setPostcode(postcode);

    try {
      doTestNewCaseForNewAddress(caseRequestDTO, "CE", Region.E, true);
      fail();
    } catch (CTPException e) {
      assertEquals(Fault.BAD_REQUEST, e.getFault());
      assertTrue(e.toString(), e.getMessage().matches(expectedErrorMessage));
    }
  }

  @Test
  public void testNewCaseForNewAddress_forceNIAddressesToRegionN() throws Exception {
    // A NI postcode will cause the region to be forced to N
    NewCaseRequestDTO caseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(0);
    caseRequestDTO.setPostcode("BT1 1AA"); // NI postcode
    caseRequestDTO.setRegion(Region.E); // Will get blatted as postcode in in Northern Ireland

    doTestNewCaseForNewAddress(caseRequestDTO, "SPG", Region.N, true);
  }

  @Test
  public void testNewCaseForNewAddress_allowEnglishPostcode() throws Exception {
    NewCaseRequestDTO caseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(0);
    caseRequestDTO.setPostcode("SO15 5NF"); // Southampton postcode
    caseRequestDTO.setRegion(Region.E);

    setupMockAIPostcodeQuery("E");

    doTestNewCaseForNewAddress(caseRequestDTO, "SPG", Region.E, true);
  }

  @Test
  public void testNewCaseForNewAddress_overrideSercoRegion() throws Exception {
    NewCaseRequestDTO caseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(0);
    caseRequestDTO.setPostcode("SO15 5NF"); // Southampton postcode
    caseRequestDTO.setRegion(Region.W); // Serco region will be overriden by AI region

    setupMockAIPostcodeQuery("E");

    doTestNewCaseForNewAddress(caseRequestDTO, "SPG", Region.E, true);
  }

  @Test
  public void testNewCaseForNewAddress_rejectScottishPostcode() throws Exception {
    // A NI postcode will cause the
    NewCaseRequestDTO caseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(0);
    caseRequestDTO.setPostcode("SO15 5NF"); // Scottish postcode
    caseRequestDTO.setRegion(Region.E);

    // Get AI to respond with a Scottish region
    setupMockAIPostcodeQuery("S");

    try {
      doTestNewCaseForNewAddress(caseRequestDTO, "SPG", Region.E, true);
      fail();
    } catch (CTPException e) {
      assertEquals(Fault.BAD_REQUEST, e.getFault());
      assertTrue(e.toString(), e.getMessage().startsWith("Scottish addresses are not valid"));
    }
  }

  @Test
  public void testNewCaseForNewAddress_useSercoRegionOnAIEmptyResult() throws Exception {
    NewCaseRequestDTO caseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(0);
    caseRequestDTO.setPostcode("SO15 6XX");
    caseRequestDTO.setRegion(Region.W);

    // AI fails to find any addresses
    AddressIndexSearchResultsDTO resultsFromAddressIndex =
        FixtureHelper.loadClassFixtures(AddressIndexSearchResultsDTO[].class).get(0);
    ArrayList<AddressIndexAddressDTO> emptyAddresses = new ArrayList<>();
    resultsFromAddressIndex.getResponse().setAddresses(emptyAddresses);
    Mockito.when(
            restClient.getResource(
                eq(POSTCODE_QUERY_PATH),
                eq(AddressIndexSearchResultsDTO.class),
                any(),
                any(),
                any()))
        .thenReturn(resultsFromAddressIndex);

    doTestNewCaseForNewAddress(caseRequestDTO, "SPG", Region.W, true); // Serco region wins
  }

  @Test
  public void testNewCaseForNewAddress_useSercoRegionOnAIFailure() throws Exception {
    NewCaseRequestDTO caseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(0);
    caseRequestDTO.setPostcode("SO15 6NF");
    caseRequestDTO.setRegion(Region.W);

    // AI fails to find any addresses
    Mockito.when(
            restClient.getResource(
                eq(POSTCODE_QUERY_PATH),
                eq(AddressIndexSearchResultsDTO.class),
                any(),
                any(),
                any()))
        .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

    doTestNewCaseForNewAddress(caseRequestDTO, "SPG", Region.W, true); // Serco region wins
  }

  private void doTestNewCaseForNewAddress(
      NewCaseRequestDTO caseRequestDTO,
      String expectedAddressType,
      Region expectedRegion,
      boolean expectedIsSecureEstablishment)
      throws CTPException {
    // Run code under test
    CaseDTO response = target.createCaseForNewAddress(caseRequestDTO);

    // Grab created case
    ArgumentCaptor<CachedCase> caseCaptor = ArgumentCaptor.forClass(CachedCase.class);
    Mockito.verify(dataRepo, times(1)).writeCachedCase(caseCaptor.capture());
    CachedCase storedCase = caseCaptor.getValue();

    // Check contents of new case
    CachedCase expectedCase = mapperFacade.map(caseRequestDTO, CachedCase.class);
    expectedCase.setId(storedCase.getId());
    expectedCase.setCreatedDateTime(storedCase.getCreatedDateTime());
    String caseTypeName = caseRequestDTO.getCaseType().name();
    expectedCase.setAddressType(expectedAddressType);
    expectedCase.setRegion(expectedRegion.name());
    expectedCase.setEstabType(caseRequestDTO.getEstabType().getCode());
    assertEquals(expectedCase, storedCase);

    // Verify the NewAddressEvent
    CollectionCaseNewAddress expectedAddress =
        mapperFacade.map(caseRequestDTO, CollectionCaseNewAddress.class);
    expectedAddress.setAddress(mapperFacade.map(caseRequestDTO, Address.class));
    expectedAddress.getAddress().setRegion(expectedRegion.name());
    expectedAddress.setId(storedCase.getId());
    verifyNewAddressEventSent(
        expectedCase.getAddressType(),
        caseRequestDTO.getEstabType().getCode(),
        caseRequestDTO.getCeUsualResidents(),
        expectedAddress);

    // Verify response
    verifyCaseDTOContent(expectedCase, caseTypeName, expectedIsSecureEstablishment, response);
  }

  private void verifyCaseDTOContent(
      CachedCase cachedCase,
      String expectedCaseType,
      boolean isSecureEstablishment,
      CaseDTO actualCaseDto) {
    CaseDTO expectedNewCaseResult = mapperFacade.map(cachedCase, CaseDTO.class);
    expectedNewCaseResult.setCreatedDateTime(actualCaseDto.getCreatedDateTime());
    expectedNewCaseResult.setCaseType(expectedCaseType);
    expectedNewCaseResult.setEstabType(EstabType.forCode(cachedCase.getEstabType()));
    expectedNewCaseResult.setSecureEstablishment(isSecureEstablishment);
    expectedNewCaseResult.setAllowedDeliveryChannels(Arrays.asList(DeliveryChannel.values()));
    assertEquals(expectedNewCaseResult, actualCaseDto);
  }

  private void verifyNewAddressEventSent(
      String expectedAddressType,
      String expectedEstabTypeCode,
      Integer expectedCapacity,
      CollectionCaseNewAddress newAddress) {
    newAddress.setCaseType(expectedAddressType);
    newAddress.setSurvey(SURVEY_NAME);
    newAddress.setCollectionExerciseId(COLLECTION_EXERCISE_ID);
    newAddress.setCeExpectedCapacity(expectedCapacity);
    Optional<AddressType> addressType = EstabType.forCode(expectedEstabTypeCode).getAddressType();
    if (addressType.isPresent() && addressType.get() == AddressType.CE) {
      newAddress.getAddress().setAddressLevel("E");
    } else {
      newAddress.getAddress().setAddressLevel("U");
    }
    newAddress.getAddress().setAddressType(expectedAddressType);
    newAddress.getAddress().setEstabType(expectedEstabTypeCode);
    NewAddress payload = new NewAddress();
    payload.setCollectionCase(newAddress);
    NewAddress payloadSent = verifyEventSent(EventType.NEW_ADDRESS_REPORTED, NewAddress.class);
    assertEquals(payload, payloadSent);
  }

  private void setupMockAIPostcodeQuery(String countryCode) {
    AddressIndexSearchResultsDTO resultsFromAddressIndex =
        FixtureHelper.loadClassFixtures(AddressIndexSearchResultsDTO[].class).get(0);
    resultsFromAddressIndex
        .getResponse()
        .getAddresses()
        .get(0)
        .getCensus()
        .setCountryCode(countryCode);
    Mockito.when(
            restClient.getResource(
                eq(POSTCODE_QUERY_PATH),
                eq(AddressIndexSearchResultsDTO.class),
                any(),
                any(),
                any()))
        .thenReturn(resultsFromAddressIndex);
  }
}
