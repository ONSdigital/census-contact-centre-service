package uk.gov.ons.ctp.integration.contactcentresvc.repository.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.cloud.CloudRetryListener;
import uk.gov.ons.ctp.common.cloud.RetryableCloudDataStore;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
    classes = {CloudRetryListener.class, CaseDataRepositoryImpl.class, AppConfig.class})
@TestPropertySource(
    properties = {"GOOGLE_CLOUD_PROJECT=census-cc-test", "cloud-storage.case-schema-name=new-case"})
public class CaseDataRepositoryImplTest {

  @Value("${GOOGLE_CLOUD_PROJECT}-${cloud-storage.case-schema-name}")
  private String caseSchema;

  @MockBean RetryableCloudDataStore dataStore;

  @Autowired private CaseDataRepositoryImpl repo;

  @Test
  public void init_withExistingNewCaseCollection() throws Exception {
    // Firestore already has the new-case collection
    Set<String> collectionNames = new HashSet<>(Arrays.asList("x", "y", caseSchema));
    Mockito.when(dataStore.getCollectionNames()).thenReturn(collectionNames);

    repo.init();

    // Verify no attempt made to populate new-case collection
    Mockito.verify(dataStore, times(0)).storeObject(any(), any(), any());
  }

  @Test
  public void init_andCreateNewCaseCollection() throws Exception {
    Set<String> collectionNames = new HashSet<>(Arrays.asList("x", "y"));
    Mockito.when(dataStore.getCollectionNames()).thenReturn(collectionNames);

    repo.init();

    Mockito.verify(dataStore, times(1))
        .storeObject(eq("census-cc-test-new-case"), eq("placeholder"), any(), eq("placeholder"));
  }

  @Test
  public void init_failedToCreateNewCaseCollection() throws Exception {
    // Firestore doesn't have the new-case collection
    Set<String> collectionNames = new HashSet<>(Arrays.asList("x", "y"));
    Mockito.when(dataStore.getCollectionNames()).thenReturn(collectionNames);

    // Simulate Firestore failing to create collection
    RuntimeException firestoreException = new RuntimeException("Firestore couldn't create");
    Mockito.doThrow(firestoreException).when(dataStore).storeObject(any(), any(), any(), any());

    try {
      repo.init();
      fail();
    } catch (CTPException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("Firestore couldn't create"));
    }
  }

  @Test
  public void readCachedCaseByUPRN() throws Exception {

    CachedCase caze = FixtureHelper.loadClassFixtures(CachedCase[].class).get(0);

    Mockito.when(
            dataStore.search(CachedCase.class, caseSchema, new String[] {"uprn"}, caze.getUprn()))
        .thenReturn(Collections.singletonList((caze)));

    Optional<CachedCase> result =
        repo.readCachedCaseByUPRN(new UniquePropertyReferenceNumber(caze.getUprn()));

    CachedCase stored = result.get();
    assertEquals(caze.getId(), stored.getId());
    assertEquals(caze.getUprn(), stored.getUprn());
    assertEquals(caze.getAddressLine1(), stored.getAddressLine1());
    assertEquals(caze.getAddressLine2(), stored.getAddressLine2());
    assertEquals(caze.getAddressLine3(), stored.getAddressLine3());
    assertEquals(caze.getTownName(), stored.getTownName());
    assertEquals(caze.getPostcode(), stored.getPostcode());
    assertEquals(caze.getAddressType(), stored.getAddressType());
    assertEquals(caze.getEstabType(), stored.getEstabType());
    assertEquals(caze.getRegion(), stored.getRegion());
  }

  @Test(expected = CTPException.class)
  public void readCachedCaseByUPRN_multipleCases() throws Exception {

    List<CachedCase> cazes = FixtureHelper.loadClassFixtures(CachedCase[].class);
    String uprn = cazes.get(0).getUprn();

    Mockito.when(dataStore.search(CachedCase.class, caseSchema, new String[] {"uprn"}, uprn))
        .thenReturn(cazes);

    repo.readCachedCaseByUPRN(new UniquePropertyReferenceNumber(uprn));
  }

  @Test()
  public void readCachedCaseByUPRN_noCases() throws Exception {

    Mockito.when(
            dataStore.search(CachedCase.class, caseSchema, new String[] {"uprn"}, "9999999999"))
        .thenReturn(Collections.emptyList());

    Optional<CachedCase> result =
        repo.readCachedCaseByUPRN(new UniquePropertyReferenceNumber("9999999999"));
    assertTrue(result.isEmpty());
  }
}
