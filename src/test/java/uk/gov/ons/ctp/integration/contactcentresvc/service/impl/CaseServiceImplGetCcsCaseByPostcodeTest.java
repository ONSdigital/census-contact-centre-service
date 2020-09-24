package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/**
 * Unit Test {@link CaseService#getCaseByUPRN(UniquePropertyReferenceNumber, CaseQueryRequestDTO)
 * getCaseByUPRN}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CaseServiceImplGetCcsCaseByPostcodeTest extends CaseServiceImplTestBase {
  // private static final String AN_ESTAB_UPRN = "334111111111";
  // private static final UniquePropertyReferenceNumber UPRN =
  // new UniquePropertyReferenceNumber(334999999999L);
  //
  // // the actual census name & id as per the application.yml and also RM
  // private static final String SURVEY_NAME = "CENSUS";
  // private static final String COLLECTION_EXERCISE_ID = "34d7f3bb-91c9-45d0-bb2d-90afce4fc790";
  //
  private static final UUID CASE_ID = UUID.fromString("b7565b5e-1396-4965-91a2-918c0d3642ed");
  // private static final UUID CACHED_CASE_ID_1 =
  // UUID.fromString("c46e5dd4-4b17-45ac-a034-0e514e8592c0");

  private static final String POSTCODE_IN_CCS_SET = "GW12AAA";
  private static final String POSTCODE_NOT_IN_CCS_SET = "GW12AAC";
  List<CaseContainerDTO> casesFromRm;

  @Before
  public void setup() {
    mockCcsPostcodes();
    casesFromRm = FixtureHelper.loadPackageFixtures(CaseContainerDTO[].class);
  }

  @Test(expected = ResponseStatusException.class)
  public void testGetCcsCaseByPostcode_caseSvcRestClientException() throws Exception {

    doThrow(new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT))
        .when(caseServiceClient)
        .getCcsCaseByPostcode(eq(POSTCODE_IN_CCS_SET));

    target.getCCSCaseByPostcode(POSTCODE_IN_CCS_SET);
  }

  @Test
  public void testGetCcsCaseByPostcode_withPostcodeInRMAndInCCSPostcodes() throws CTPException {
    casesFromRm.get(1).setPostcode(POSTCODE_IN_CCS_SET);
    // mockCasesFromRm();
    when(caseServiceClient.getCcsCaseByPostcode(eq(POSTCODE_IN_CCS_SET))).thenReturn(casesFromRm);
    CaseDTO result = getCasesByPostcode(POSTCODE_IN_CCS_SET);
    assertEquals(CASE_ID, result.getId());
  }

  @Test
  public void testGetCcsCaseByPostcode_withPostcodeInRMAndNotInCCSPostcodes() throws CTPException {
    // casesFromRm.get(1).setPostcode(POSTCODE_NOT_IN_CCS_SET);
    try {
      getCasesByPostcode(POSTCODE_NOT_IN_CCS_SET);
      fail();
    } catch (CTPException badRequest) {
      assertEquals(Fault.BAD_REQUEST, badRequest.getFault());
    }
  }

  @Test
  public void testGetCcsCaseByPostcode_withPostcodeNotInRMAndInCCSPostcodes() throws CTPException {
    //    casesFromRm.get(1).setPostcode(POSTCODE_IN_CCS_SET);
    // mockCasesFromRm();
    //
    // when(caseServiceClient.getCcsCaseByPostcode(eq(POSTCODE_IN_CCS_SET))).thenReturn(casesFromRm);
    //    ResponseStatusException ex = new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
    //        "Resource Not Found", new HttpClientErrorException(HttpStatus.NOT_FOUND));
    //    Mockito.doThrow(ex).when(caseServiceClient.getCcsCaseByPostcode(POSTCODE_IN_CCS_SET));
    doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCcsCaseByPostcode(eq(POSTCODE_IN_CCS_SET));
    try {
      getCasesByPostcode(POSTCODE_IN_CCS_SET);
      fail();
    } catch (ResponseStatusException notFound) {
      //      assertTrue(notFound.getMessage().contains(Fault.RESOURCE_NOT_FOUND.toString()));
      assertEquals(HttpStatus.NOT_FOUND, notFound.getStatus());
    }
  }
  //
  // @Test
  // public void testGetCcsCaseByPostcode_withPostcodeNotInRMAndNotInCCSPostcodes() {
  //
  // }

  // @Test
  // public void testGetCaseByUprn_withCaseDetailsForCaseTypeHH() throws Exception {
  // casesFromRm.get(0).setCaseType(CaseType.HH.name());

  // CaseDTO result = getCasesByUprn(true);
  // verifyNonCachedCase(result, true, 0);
  // }
  //
  // @Test
  // public void testGetCaseByUprn_withCaseDetailsForCaseTypeCE() throws Exception {
  // casesFromRm.get(1).setCaseType(CaseType.CE.name());
  // setLastUpdated(casesFromRm.get(0), 2020, 5, 14);
  // setLastUpdated(casesFromRm.get(1), 2020, 5, 15);
  // mockCasesFromRm();
  // CaseDTO result = getCasesByUprn(true);
  // verifyNonCachedCase(result, true, 1);
  // }
  //
  // @Test
  // public void testGetCaseByUprn_withNoCaseDetailsForCaseTypeHH() throws Exception {
  // casesFromRm.get(0).setCaseType(CaseType.HH.name());
  // mockCasesFromRm();
  // CaseDTO result = getCasesByUprn(false);
  // verifyNonCachedCase(result, false, 0);
  // }

  // @Test
  // public void testGetCaseByUprn_withNoCaseDetailsForCaseTypeCE() throws Exception {
  // casesFromRm.get(1).setCaseType(CaseType.CE.name());
  // setLastUpdated(casesFromRm.get(0), 2020, 5, 14);
  // setLastUpdated(casesFromRm.get(1), 2020, 5, 15);
  // mockCasesFromRm();
  // CaseDTO result = getCasesByUprn(false);
  // verifyNonCachedCase(result, false, 1);
  // }

  // @Test
  // public void testGetCaseByUprn_householdIndividualCase_emptyResultSet_noCachedCase()
  // throws Exception {
  //
  // casesFromRm.get(0).setCaseType("HI");
  // casesFromRm.get(1).setCaseType("HI");
  //
  // mockCasesFromRm();
  // mockNothingInTheCache();
  // mockAddressFromAI();
  //
  // CaseDTO result = getCasesByUprn(false);
  // verifyNewCase(result);
  // }

  // @Test
  // public void testGetCaseByUprn_caseSvcNotFoundResponse_noCachedCase_HH() throws Exception {
  //
  // doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND)).when(caseServiceClient)
  // .getCaseByUprn(eq(UPRN.getValue()), any());
  //
  // mockNothingInTheCache();
  // mockAddressFromAI();
  //
  // CaseDTO result = getCasesByUprn(false);
  // verifyNewCase(result);
  // }

  // private void verifyCreatedNewCase(String estabType) throws Exception {
  // addressFromAI.setCensusEstabType("marina");
  //
  // mockNothingInRm();
  // mockNothingInTheCache();
  // mockAddressFromAI();
  //
  // CaseDTO result = getCasesByUprn(false);
  // verifyNewCase(result);
  // }

  // @Test
  // public void testGetCaseByUprn_caseSvcNotFoundResponse_noCachedCase_SPG() throws Exception {
  // verifyCreatedNewCase("marina");
  // }
  //
  // @Test
  // public void testGetCaseByUprn_caseSvcNotFoundResponse_noCachedCase_CE() throws Exception {
  // verifyCreatedNewCase("CARE HOME");
  // }

  // @Test
  // public void testGetCaseByUprn_caseSvcNotFoundResponse_noCachedCase_NA() throws Exception {
  // verifyCreatedNewCase("NA");
  // }

  // @Test(expected = CTPException.class)
  // public void testGetCaseByUprn_caseSvcNotFoundResponse_noCachedCase_addressServiceNotFound()
  // throws Exception {
  //
  // mockNothingInRm();
  // mockNothingInTheCache();
  //
  // doThrow(new
  // CTPException(Fault.RESOURCE_NOT_FOUND)).when(addressSvc).uprnQuery(UPRN.getValue());
  // target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(false));
  // verify(caseServiceClient, times(1)).getCaseByUprn(any(Long.class), any(Boolean.class));
  // verifyHasReadCachedCases();
  // verifyNotWrittenCachedCase();
  // verify(addressSvc, times(1)).uprnQuery(anyLong());
  // verifyEventNotSent();
  // }
  //
  // @Test(expected = ResponseStatusException.class)
  // public void
  // testGetCaseByUprn_caseSvcNotFoundResponse_noCachedCase_addressSvcRestClientException()
  // throws Exception {
  //
  // mockNothingInRm();
  // mockNothingInTheCache();
  //
  // doThrow(new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT)).when(addressSvc)
  // .uprnQuery(eq(UPRN.getValue()));
  //
  // target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(false));
  // }

  // @Test(expected = CTPException.class)
  // public void testGetCaseByUprn_caseSvcNotFoundResponse_noCachedCase_scottishAddress()
  // throws Exception {
  //
  // addressFromAI.setCountryCode("S");
  //
  // mockNothingInRm();
  // mockNothingInTheCache();
  // mockAddressFromAI();
  //
  // target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(false));
  // }

  // @Test(expected = CTPException.class)
  // public void testGetCaseByUprn_caseSvcNotFoundResponse_NoCachedCase_RetriesExhausted()
  // throws Exception {
  //
  // mockNothingInRm();
  // mockNothingInTheCache();
  // mockAddressFromAI();
  //
  // doThrow(new CTPException(Fault.SYSTEM_ERROR, new Exception(), "Retries exhausted"))
  // .when(dataRepo).writeCachedCase(any());
  // target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(false));
  // }

  // @Test
  // public void testGetCaseByUprn_mixedCaseTypes() throws Exception {
  // casesFromRm.get(0).setCaseType("HI"); // Household Individual case
  // mockCasesFromRm();
  // CaseDTO result = getCasesByUprn(true);
  // verifyNonCachedCase(result, true, 1);
  // }

  // @Test
  // public void testGetCaseByUprn_caseSPG() throws Exception {
  // casesFromRm.get(0).setCaseType("SPG");
  // mockCasesFromRm();
  // CaseDTO result = getCasesByUprn(true);
  // verifyNonCachedCase(result, true, 0);
  // }

  // @Test
  // public void testGetCaseByUprn_caseHH() throws Exception {
  // casesFromRm.get(0).setCaseType("HH");
  // mockCasesFromRm();
  // CaseDTO result = getCasesByUprn(true);
  // verifyNonCachedCase(result, true, 0);
  // }
  //
  // @Test
  // public void shouldGetSecureEstablishmentByUprn() throws Exception {
  // setLastUpdated(casesFromRm.get(0), 2020, 5, 14);
  // setLastUpdated(casesFromRm.get(1), 2020, 5, 15);
  // mockCasesFromRm();
  // CaseDTO result = getCasesByUprn(false);
  // assertTrue(result.isSecureEstablishment());
  // assertEquals(new UniquePropertyReferenceNumber(AN_ESTAB_UPRN), result.getEstabUprn());
  // }
  //
  // // --- results from both RM and cache ...
  //
  // @Test
  // public void shouldGetLatestFromCacheWhenResultsFromBothRmAndCache() throws Exception {
  // mockCasesFromRm();
  // mockCasesFromCache();
  // CaseDTO result = getCasesByUprn(false);
  // assertEquals(CACHED_CASE_ID_1, result.getId());
  // }

  // @Test
  // public void shouldGetLatestFromCacheWhenResultsFromBothRmAndCacheWithSmallTimeDifference()
  // throws Exception {
  // casesFromCache.get(0).setCreatedDateTime(utcDate(LocalDateTime.of(2020, 2, 3, 10, 4, 6)));
  // casesFromCache.get(1).setCreatedDateTime(utcDate(LocalDateTime.of(2020, 2, 3, 10, 4, 5)));
  // casesFromRm.get(0).setLastUpdated(utcDate(LocalDateTime.of(2020, 1, 4, 0, 0)));
  // casesFromRm.get(1).setLastUpdated(utcDate(LocalDateTime.of(2019, 12, 12, 0, 0)));
  // mockCasesFromRm();
  // mockCasesFromCache();
  // CaseDTO result = getCasesByUprn(false);
  // assertEquals(CACHED_CASE_ID_0, result.getId());
  // }
  //
  // @Test
  // public void shouldGetLatestFromRmWhenResultsFromBothRmAndCache() throws Exception {
  // casesFromCache.get(0).setCreatedDateTime(utcDate(LocalDateTime.of(2020, 1, 2, 0, 0)));
  // casesFromCache.get(1).setCreatedDateTime(utcDate(LocalDateTime.of(2020, 1, 3, 0, 0)));
  // casesFromRm.get(0).setLastUpdated(utcDate(LocalDateTime.of(2020, 1, 1, 0, 0)));
  // casesFromRm.get(1).setLastUpdated(utcDate(LocalDateTime.of(2020, 1, 23, 0, 0)));
  // mockCasesFromRm();
  // mockCasesFromCache();
  // CaseDTO result = getCasesByUprn(false);
  // assertEquals(UUID_1, result.getId());
  // }
  //
  // @Test
  // public void shouldGetLatestFromRmWhenResultsFromBothRmAndCacheWithSmallTimeDifferences()
  // throws Exception {
  // casesFromCache.get(0).setCreatedDateTime(utcDate(LocalDateTime.of(2020, 1, 3, 0, 0)));
  // casesFromCache.get(1).setCreatedDateTime(utcDate(LocalDateTime.of(2020, 2, 3, 10, 4, 5)));
  // casesFromRm.get(0).setLastUpdated(utcDate(LocalDateTime.of(2020, 1, 1, 0, 0)));
  // casesFromRm.get(1).setLastUpdated(utcDate(LocalDateTime.of(2020, 2, 3, 10, 4, 6)));
  // mockCasesFromRm();
  // mockCasesFromCache();
  // CaseDTO result = getCasesByUprn(false);
  // assertEquals(UUID_1, result.getId());
  // }

  // ---- helpers methods below ---

  // private void mockCasesFromRm() {
  // when(caseServiceClient.getCcsCaseByPostcode(eq(POSTCODE_IN_CCS_SET))).thenReturn(casesFromRm);
  // }

  private void mockNothingInRm() {
    doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCcsCaseByPostcode(eq(POSTCODE_IN_CCS_SET));
  }

  private void verifyCallToGetCasesFromRm() {
    verify(caseServiceClient).getCaseByUprn(any(Long.class), any(Boolean.class));
  }

  // private void verifyHasReadCachedCases() throws Exception {
  // verify(dataRepo).readCachedCasesByUprn(any(UniquePropertyReferenceNumber.class));
  // }
  //
  // private CachedCase verifyHasWrittenCachedCase() throws Exception {
  // ArgumentCaptor<CachedCase> cachedCaseCaptor = ArgumentCaptor.forClass(CachedCase.class);
  // verify(dataRepo).writeCachedCase(cachedCaseCaptor.capture());
  // return cachedCaseCaptor.getValue();
  // }
  //
  // // private void mockAddressFromAI() throws Exception {
  // // when(addressSvc.uprnQuery(UPRN.getValue())).thenReturn(addressFromAI);
  // // }
  //
  // private void verifyNonCachedCase(CaseDTO results, boolean caseEventsExpected, int dataIndex)
  // throws Exception {
  // CaseDTO expectedCaseResult =
  // createExpectedCaseDTO(casesFromRm.get(dataIndex), caseEventsExpected);
  //
  // verifyCase(results, expectedCaseResult, caseEventsExpected);
  // verifyHasReadCachedCases();
  // }

  // private void verifyNewCase(CaseDTO result) throws Exception {
  //
  // verifyCallToGetCasesFromRm();
  // verifyHasReadCachedCases();
  // verify(addressSvc, times(1)).uprnQuery(anyLong());
  //
  // // Verify content of case written to Firestore
  // CachedCase capturedCase = verifyHasWrittenCachedCase();
  // verifyCachedCaseContent(result.getId(), CaseType.HH, capturedCase);
  //
  // // Verify response
  // CachedCase cachedCase = mapperFacade.map(addressFromAI, CachedCase.class);
  // cachedCase.setId(result.getId().toString());
  // verifyCaseDTOContent(cachedCase, CaseType.HH.name(), false, result);
  //
  // // Verify the NewAddressEvent
  // CollectionCaseNewAddress newAddress =
  // mapperFacade.map(addressFromAI, CollectionCaseNewAddress.class);
  // newAddress.setId(cachedCase.getId());
  // verifyNewAddressEventSent(addressFromAI.getCensusAddressType(),
  // addressFromAI.getCensusEstabType(), newAddress);
  // }

  // private void verifyCachedCaseContent(UUID expectedId, CaseType expectedCaseType,
  // CachedCase expectedCase) {
  // assertEquals(expectedId.toString(), expectedCase.getId());
  // assertEquals(addressFromAI.getUprn(), expectedCase.getUprn());
  // assertEquals(addressFromAI.getAddressLine1(), expectedCase.getAddressLine1());
  // assertEquals(addressFromAI.getAddressLine2(), expectedCase.getAddressLine2());
  // assertEquals(addressFromAI.getAddressLine3(), expectedCase.getAddressLine3());
  // assertEquals(addressFromAI.getTownName(), expectedCase.getTownName());
  // assertEquals(addressFromAI.getPostcode(), expectedCase.getPostcode());
  // assertEquals(addressFromAI.getCensusAddressType(), expectedCase.getAddressType());
  // assertEquals(expectedCaseType, expectedCase.getCaseType());
  // assertEquals(addressFromAI.getCensusEstabType(), expectedCase.getEstabType());
  // assertEquals(addressFromAI.getCountryCode(), expectedCase.getRegion());
  // assertEquals(addressFromAI.getOrganisationName(), expectedCase.getCeOrgName());
  // assertEquals(0, expectedCase.getCaseEvents().size());
  // }

  // private void verifyCaseDTOContent(CachedCase cachedCase, String expectedCaseType,
  // boolean isSecureEstablishment, CaseDTO actualCaseDto) {
  // CaseDTO expectedNewCaseResult = mapperFacade.map(cachedCase, CaseDTO.class);
  // expectedNewCaseResult.setCreatedDateTime(actualCaseDto.getCreatedDateTime());
  // expectedNewCaseResult.setCaseType(expectedCaseType);
  // expectedNewCaseResult.setEstabType(EstabType.forCode(cachedCase.getEstabType()));
  // expectedNewCaseResult.setSecureEstablishment(isSecureEstablishment);
  // expectedNewCaseResult.setAllowedDeliveryChannels(Arrays.asList(DeliveryChannel.values()));
  // expectedNewCaseResult.setCaseEvents(Collections.emptyList());
  // assertEquals(expectedNewCaseResult, actualCaseDto);
  // }

  // private CaseDTO getCasesByUprn(boolean caseEvents) throws CTPException {
  // List<CaseDTO> results = target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(caseEvents));
  // assertEquals(1, results.size());
  // return results.get(0);
  // }

  private CaseDTO getCasesByPostcode(String postcode) throws CTPException {
    List<CaseDTO> results = target.getCCSCaseByPostcode(postcode);
    // assertEquals(2, results.size());
    return results.get(0);
  }
}
