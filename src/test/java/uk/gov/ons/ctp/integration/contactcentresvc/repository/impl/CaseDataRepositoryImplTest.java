package uk.gov.ons.ctp.integration.contactcentresvc.repository.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
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
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.model.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CloudDataStore;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.DataStoreContentionException;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.config.CcRetryListener;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.CaseDataRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
    classes = {CcRetryListener.class, CaseDataRepositoryImpl.class, AppConfig.class})
@TestPropertySource(
    properties = {
      "google-cloud-project=census-cc-test",
      "cloud-storage.case-schema-name=new-case",
      "cloud-storage.backoff-initial=10",
      "cloud-storage.backoff-multiplier=1.2",
      "cloud-storage.backoff-max=300",
      "cloud-storage.backoff-max-attempts=3",
    })
public class CaseDataRepositoryImplTest {

  @Value("${google-cloud-project}-${cloud-storage.case-schema-name}")
  private String caseSchema;

  @MockBean CloudDataStore dataStore;

  @Autowired private CaseDataRepository repo;

  @Test
  public void writeCachedCaseWithRetry() throws Exception {

    CachedCase caze = FixtureHelper.loadClassFixtures(CachedCase[].class).get(0);

    Mockito.doThrow(new DataStoreContentionException("Test", new Exception()))
        .when(dataStore)
        .storeObject(any(), any(), any());

    try {
      repo.writeCachedCase(caze);
      Assert.fail("Exception should be thrown");
    } catch (DataStoreContentionException ex) {
      // Catch final retry
    }

    Mockito.verify(dataStore, times(3)).storeObject(caseSchema, caze.getId(), caze);
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
