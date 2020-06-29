package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;

import java.util.Arrays;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.AddressType;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.CollectionCaseNewAddress;
import uk.gov.ons.ctp.common.event.model.NewAddress;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.DeliveryChannel;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.NewCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/**
 * Unit Test {@link CaseService#createCaseForNewAddress(NewCaseRequestDTO) createCaseForNewAddress}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CaseServiceImplCreateCaseForNewAddressTest extends CaseServiceImplTestBase {

  // the actual census name & id as per the application.yml and also RM
  private static final String SURVEY_NAME = "CENSUS";
  private static final String COLLECTION_EXERCISE_ID = "34d7f3bb-91c9-45d0-bb2d-90afce4fc790";

  @Before
  public void setup() {
    Mockito.when(appConfig.getChannel()).thenReturn(Channel.CC);
    Mockito.when(appConfig.getSurveyName()).thenReturn(SURVEY_NAME);
    Mockito.when(appConfig.getCollectionExerciseId()).thenReturn(COLLECTION_EXERCISE_ID);
  }

  @Test
  public void testNewCaseForNewAddress() throws Exception {
    NewCaseRequestDTO caseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(0);

    doTestNewCaseForNewAddress(caseRequestDTO, "SPG", true);
  }

  @Test
  public void testNewCaseForNewAddress_forEstabTypeOfOther() throws Exception {
    // Load request, which has estabType of Other
    NewCaseRequestDTO caseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(1);

    // Address type will be that for EstabType.Other
    doTestNewCaseForNewAddress(caseRequestDTO, "HH", false);
  }

  @Test
  public void testNewCaseForNewAddress_mismatchedCaseAndAddressType() throws Exception {
    // Load request, which has caseType of HH and estabType with a CE addressType
    NewCaseRequestDTO caseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(2);

    try {
      doTestNewCaseForNewAddress(caseRequestDTO, null, false);
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
  public void testNewCaseForNewAddress_ceWithNonPositiveNumberOfResidents() throws Exception {
    // Load valid request and then update so that it's invalid
    NewCaseRequestDTO caseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(2);
    // Simulate error by making request a CE with a non-positive number of residents
    caseRequestDTO.setCaseType(CaseType.CE);
    caseRequestDTO.setCeUsualResidents(0);

    try {
      doTestNewCaseForNewAddress(caseRequestDTO, null, false);
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

    doTestNewCaseForNewAddress(caseRequestDTO, "CE", true);
  }

  private void doTestNewCaseForNewAddress(
      NewCaseRequestDTO caseRequestDTO,
      String expectedAddressType,
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
    expectedCase.setEstabType(caseRequestDTO.getEstabType().getCode());
    assertEquals(expectedCase, storedCase);

    // Verify the NewAddressEvent
    CollectionCaseNewAddress expectedAddress =
        mapperFacade.map(caseRequestDTO, CollectionCaseNewAddress.class);
    expectedAddress.setAddress(mapperFacade.map(caseRequestDTO, Address.class));
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
    Mockito.verify(eventPublisher, times(1))
        .sendEvent(
            EventType.NEW_ADDRESS_REPORTED,
            Source.CONTACT_CENTRE_API,
            appConfig.getChannel(),
            payload);
  }
}
