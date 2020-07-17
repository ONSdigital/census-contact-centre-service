package uk.gov.ons.ctp.integration.contactcentresvc.repository.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.cloud.RetryableCloudDataStore;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.CaseDataRepository;

@Service
public class CaseDataRepositoryImpl implements CaseDataRepository {

  private static final Logger log = LoggerFactory.getLogger(CaseDataRepositoryImpl.class);

  @Value("${GOOGLE_CLOUD_PROJECT}")
  private String gcpProject;

  @Value("${cloud-storage.case-schema-name}")
  private String caseSchemaName;

  private String caseSchema;

  private RetryableCloudDataStore cloudDataStore;

  // This is the name of the document that is used to create and retain the new-case collection
  private static final String PLACEHOLDER_CASE_NAME = "placeholder";

  private static final String[] SEARCH_BY_UPRN_PATH = new String[] {"uprn"};

  private static final String[] SEARCH_BY_CASEID_PATH = new String[] {"id"};

  @PostConstruct
  public void init() throws CTPException {
    caseSchema = gcpProject + "-" + caseSchemaName.toLowerCase();
    ensureCollectionExists(caseSchema);
  }

  @Autowired
  public CaseDataRepositoryImpl(RetryableCloudDataStore cloudDataStore) {
    this.cloudDataStore = cloudDataStore;
  }

  private void ensureCollectionExists(String collectionName) throws CTPException {
    log.with("collectionName", collectionName).info("Checking if collection exists");

    Set<String> collectionNames = cloudDataStore.getCollectionNames();

    if (!collectionNames.contains(collectionName)) {
      log.with("collectionName", collectionName).info("Creating collection");

      try {
        // Force collection creation by adding an object.
        // Firestore doesn't have the concept of an empty collection. A collection only exists
        // when it holds at least one document. So we therefore have to leave the placeholder
        // object to keep the collection.
        CachedCase dummyCase = new CachedCase();
        cloudDataStore.storeObject(
            collectionName, PLACEHOLDER_CASE_NAME, dummyCase, PLACEHOLDER_CASE_NAME);
      } catch (Exception e) {
        log.error("Failed to create collection", e);
        throw new CTPException(Fault.SYSTEM_ERROR, e);
      }
    }

    log.with("collectionName", collectionName).info("Collection check completed");
  }

  @Override
  public void writeCachedCase(final CachedCase caze) throws CTPException {
    cloudDataStore.storeObject(caseSchema, caze.getId(), caze, caze.getId());
  }

  @Override
  public List<CachedCase> readCachedCasesByUprn(UniquePropertyReferenceNumber uprn)
      throws CTPException {
    String key = String.valueOf(uprn.getValue());
    return cloudDataStore.search(CachedCase.class, caseSchema, SEARCH_BY_UPRN_PATH, key);
  }

  public List<CachedCase> readCachedCasesById(UUID caseId) throws CTPException {
    String key = caseId.toString();
    return cloudDataStore.search(CachedCase.class, caseSchema, SEARCH_BY_CASEID_PATH, key);
  }

  @Override
  public Optional<CachedCase> readCachedCaseById(final UUID caseId) throws CTPException {
    return cloudDataStore.retrieveObject(CachedCase.class, caseSchema, caseId.toString());
  }
}
