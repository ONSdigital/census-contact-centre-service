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

  @Value("${google-cloud-project}")
  private String gcpProject;

  @Value("${cloud-storage.case-schema-name}")
  private String caseSchemaName;

  private String caseSchema;

  @Autowired private CloudDataStore cloudDataStore;

  @PostConstruct
  public void init() {
    caseSchema = gcpProject + "-" + caseSchemaName.toLowerCase();
    this.cloudDataStore.connect();
  }

  @Retryable(
      label = "writeNewCase",
      include = DataStoreContentionException.class,
      backoff =
          @Backoff(
              delayExpression = "#{${cloud-storage.backoff-initial}}",
              multiplierExpression = "#{${cloud-storage.backoff-multiplier}}",
              maxDelayExpression = "#{${cloud-storage.backoff-max}}"),
      maxAttemptsExpression = "#{${cloud-storage.backoff-max-attempts}}",
      listeners = "ccRetryListener")
  @Override
  public void storeCaseByUPRN(CachedCase caze) throws CTPException, DataStoreContentionException {
    cloudDataStore.storeObject(caseSchema, caze.getUprn(), caze);
  }

  @Override
  public Optional<CachedCase> readCaseByUPRN(final UniquePropertyReferenceNumber uprn)
      throws CTPException {
    return cloudDataStore.retrieveObject(
        CachedCase.class, caseSchema, String.valueOf(uprn.getValue()));
  }
}
