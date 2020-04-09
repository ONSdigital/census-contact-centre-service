package uk.gov.ons.ctp.integration.contactcentresvc.repository;

import java.util.Optional;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.model.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.DataStoreContentionException;

/** Repository for Case Data */
public interface CaseDataRepository {

  /** Write a Case */
  void storeCaseByUPRN(CachedCase newCase) throws CTPException, DataStoreContentionException;

  /** Read Cases for an address UPRN */
  Optional<CachedCase> readCaseByUPRN(final UniquePropertyReferenceNumber uprn) throws CTPException;
}
