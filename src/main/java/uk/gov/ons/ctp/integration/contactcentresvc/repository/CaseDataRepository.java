package uk.gov.ons.ctp.integration.contactcentresvc.repository;

import java.util.Optional;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CollectionCase;

/** Repository for Case Data */
public interface CaseDataRepository {

  /* Write a Case */
  void writeCollectionCase(CollectionCase collectionCase) throws CTPException;

  /* Read a Case */
  Optional<CollectionCase> readCollectionCase(String caseId) throws CTPException;
}
