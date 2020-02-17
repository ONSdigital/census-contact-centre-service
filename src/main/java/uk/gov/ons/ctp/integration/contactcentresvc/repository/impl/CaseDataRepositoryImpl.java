package uk.gov.ons.ctp.integration.contactcentresvc.repository.impl;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CollectionCaseNewAddress;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CloudDataStore;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.FirestoreDataStore;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.CaseDataRepository;

@Service
public class CaseDataRepositoryImpl implements CaseDataRepository {

  @Value("${GOOGLE_CLOUD_PROJECT}")
  private String gcpProject;

  @Value("${cloudStorage.caseSchemaName}")
  private String caseSchemaName;
  
  private String caseSchema;

  private CloudDataStore cloudDataStore;

  public CaseDataRepositoryImpl() {
    this.cloudDataStore = new FirestoreDataStore();
    this.cloudDataStore.connect();
  }
  @PostConstruct
  public void init() {
    caseSchema = gcpProject + "-" + caseSchemaName.toLowerCase();
  }

  @Override
  public void writeCollectionCase(CollectionCaseNewAddress collectionCase) throws CTPException {
    // TODO Auto-generated method stub

  }

  @Override
  public List<CollectionCaseNewAddress> readCollectionCaseByUPRN(final String uprn)
      throws CTPException {
    String[] searchByUprnPath = new String[] {"address", "uprn"};
    List<CollectionCaseNewAddress> searchResults =
        cloudDataStore.search(CollectionCaseNewAddress.class, caseSchema, searchByUprnPath, uprn);

  return searchResults;
  }  
}
