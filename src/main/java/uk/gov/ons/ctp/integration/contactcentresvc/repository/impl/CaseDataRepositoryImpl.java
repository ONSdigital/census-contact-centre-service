package uk.gov.ons.ctp.integration.contactcentresvc.repository.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.List;
import java.util.Optional;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.model.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CloudDataStore;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.DataStoreContentionException;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.CaseDataRepository;

@Service
public class CaseDataRepositoryImpl implements CaseDataRepository {

  private static final Logger log = LoggerFactory.getLogger(CaseDataRepositoryImpl.class);

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
  public void writeCachedCase(CachedCase caze) throws CTPException, DataStoreContentionException {
    cloudDataStore.storeObject(caseSchema, caze.getId(), caze);
  }

  @Override
  public Optional<CachedCase> readCachedCaseByUPRN(final UniquePropertyReferenceNumber uprn)
      throws CTPException {
    String key = String.valueOf(uprn.getValue());
    String[] searchByUprnPath = new String[] {"uprn"};
    List<CachedCase> results =
        cloudDataStore.search(CachedCase.class, caseSchema, searchByUprnPath, key);

    if (results.isEmpty()) {
      return Optional.empty();
    } else if (results.size() > 1) {
      log.with("uprn", key).error("More than one cached skeleton case for UPRN");
      throw new CTPException(
          Fault.SYSTEM_ERROR, "More than one cached skeleton case for UPRN: " + key);
    } else {
      return Optional.ofNullable(results.get(0));
    }
  }
}
