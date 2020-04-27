package uk.gov.ons.ctp.integration.contactcentresvc.repository;

import java.util.Optional;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.model.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.DataStoreContentionException;

/** Repository for Case Data */
public interface CaseDataRepository {

  /**
   * Store newly created skeleton case to repository
   *
   * @param newCase Skeleton case to be stored in repository
   * @throws CTPException undefined system error on storing case to repository
   * @throws DataStoreContentionException repository store operation failing
   */
  void writeCachedCase(CachedCase newCase) throws CTPException, DataStoreContentionException;

  /**
   * Read a Case for an address by Unique Property Reference Number
   *
   * @param uprn of case to read
   * @return Optional containing case for UPRN if available
   * @throws CTPException error reading case
   */
  Optional<CachedCase> readCachedCaseByUPRN(final UniquePropertyReferenceNumber uprn)
      throws CTPException;
}
