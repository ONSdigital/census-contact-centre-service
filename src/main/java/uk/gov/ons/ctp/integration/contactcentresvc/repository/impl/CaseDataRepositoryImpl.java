package uk.gov.ons.ctp.integration.contactcentresvc.repository.impl;

import java.util.Optional;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.model.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CloudDataStore;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.DataStoreContentionException;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.CaseDataRepository;

@Service
public class CaseDataRepositoryImpl implements CaseDataRepository {

  @Value("${GOOGLE_CLOUD_PROJECT}")
  private String gcpProject;

  @Value("${cloudStorage.caseSchemaName}")
  private String caseSchemaName;

  private String caseSchema;

  @Autowired private CloudDataStore cloudDataStore;

  @PostConstruct
  public void init() {
    caseSchema = gcpProject + "-" + caseSchemaName.toLowerCase();
    this.cloudDataStore.connect();
  }

  /**
   * Store a New Address Case object into the data store
   *
   * @param CollectionCaseNewAddress to store
   */
  @Retryable(
      label = "writeNewCase",
      include = DataStoreContentionException.class,
      backoff =
          @Backoff(
              delayExpression = "#{${cloudStorage.backoffInitial}}",
              multiplierExpression = "#{${cloudStorage.backoffMultiplier}}",
              maxDelayExpression = "#{${cloudStorage.backoffMax}}"),
      maxAttemptsExpression = "#{${cloudStorage.backoffMaxAttempts}}",
      listeners = "ccRetryListener")
  @Override
  public void storeCaseByUPRN(CachedCase caze) throws CTPException, DataStoreContentionException {
    cloudDataStore.storeObject(caseSchema, caze.getUprn(), caze);
  }

  /**
   * Read a New Address Case object from data store
   *
   * @param Unique Property Reference Number under which the case is stored
   * @return Case stored for UPRN
   */
  @Override
  public Optional<CachedCase> readCaseByUPRN(final UniquePropertyReferenceNumber uprn)
      throws CTPException {
    return cloudDataStore.retrieveObject(
        CachedCase.class, caseSchema, String.valueOf(uprn.getValue()));
  }
}
