package uk.gov.ons.ctp.integration.contactcentresvc.repository.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.cloud.RetryableCloudDataStore;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;

@RunWith(MockitoJUnitRunner.class)
public class CaseDataRepositoryImplTest {

  private static final String GCP_PROJECT_NAME = "census-test-project";
  private static final String SCHEMA_NAME = "schema-name";
  private static final String CASE_SCHEMA = GCP_PROJECT_NAME + "-" + SCHEMA_NAME;

  @Mock RetryableCloudDataStore dataStore;

  @InjectMocks private CaseDataRepositoryImpl repo;

  @Before
  public void setup() {
    ReflectionTestUtils.setField(repo, "caseSchemaName", SCHEMA_NAME);
    ReflectionTestUtils.setField(repo, "gcpProject", GCP_PROJECT_NAME);
  }

  @Test
  public void init_withExistingNewCaseCollection() throws Exception {
    // Firestore already has the new-case collection
    Set<String> collectionNames = new HashSet<>(Arrays.asList("x", "y", CASE_SCHEMA));
    when(dataStore.getCollectionNames()).thenReturn(collectionNames);

    repo.init();

    // Verify no attempt made to populate new-case collection
    verify(dataStore, never()).storeObject(any(), any(), any(), any());
  }

  @Test
  public void init_andCreateNewCaseCollection() throws Exception {
    Set<String> collectionNames = new HashSet<>(Arrays.asList("x", "y"));
    when(dataStore.getCollectionNames()).thenReturn(collectionNames);

    repo.init();

    verify(dataStore).storeObject(eq(CASE_SCHEMA), eq("placeholder"), any(), eq("placeholder"));
  }

  @Test
  public void init_failedToCreateNewCaseCollection() throws Exception {
    // Firestore doesn't have the new-case collection
    Set<String> collectionNames = new HashSet<>(Arrays.asList("x", "y"));
    when(dataStore.getCollectionNames()).thenReturn(collectionNames);

    // Simulate Firestore failing to create collection
    RuntimeException firestoreException = new RuntimeException("Firestore couldn't create");
    doThrow(firestoreException).when(dataStore).storeObject(any(), any(), any(), any());

    CTPException e = assertThrows(CTPException.class, () -> repo.init());
    assertTrue(e.getMessage(), e.getMessage().contains("Firestore couldn't create"));
  }

  private List<CachedCase> readCachedCases(String uprn) throws Exception {
    repo.init();
    return repo.readCachedCasesByUprn(new UniquePropertyReferenceNumber(uprn));
  }

  @Test
  public void shouldReadSingleCachedCaseByUprn() throws Exception {
    CachedCase caze = FixtureHelper.loadClassFixtures(CachedCase[].class).get(0);

    when(dataStore.search(CachedCase.class, CASE_SCHEMA, new String[] {"uprn"}, caze.getUprn()))
        .thenReturn(Collections.singletonList((caze)));

    List<CachedCase> result = readCachedCases(caze.getUprn());
    CachedCase stored = result.get(0);
    assertEquals(caze, stored);
  }

  @Test
  public void shouldReadMultipleCachedCaseByUprn() throws Exception {
    List<CachedCase> cachedCases = FixtureHelper.loadClassFixtures(CachedCase[].class);
    String uprn = cachedCases.get(0).getUprn();

    when(dataStore.search(CachedCase.class, CASE_SCHEMA, new String[] {"uprn"}, uprn))
        .thenReturn(cachedCases);

    List<CachedCase> result = readCachedCases(uprn);

    assertEquals(cachedCases.get(0), result.get(0));
    assertEquals(cachedCases.get(1), result.get(1));
  }

  @Test()
  public void shouldReadNoCachedCasesByUprn() throws Exception {
    when(dataStore.search(CachedCase.class, CASE_SCHEMA, new String[] {"uprn"}, "9999999999"))
        .thenReturn(Collections.emptyList());

    List<CachedCase> result = readCachedCases("9999999999");
    assertTrue(result.isEmpty());
  }
}
