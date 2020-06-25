package uk.gov.ons.ctp.integration.contactcentresvc.repository;

import java.util.Optional;
import java.util.UUID;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;

/** Repository for Case Data */
public interface CaseDataRepository {

  /**
   * Store newly created skeleton case to repository
   *
   * @param newCase Skeleton case to be stored in repository
   * @throws CTPException undefined system error on storing case to repository
   */
  void writeCachedCase(final CachedCase newCase) throws CTPException;

  /**
   * Read a Case for an address by Unique Property Reference Number
   *
   * @param uprn of case to read
   * @return Optional containing case for UPRN if available
   * @throws CTPException error reading case
   */
  Optional<CachedCase> readCachedCaseByUPRN(final UniquePropertyReferenceNumber uprn)
      throws CTPException;

  /**
   * Read a skeleton Case by Id
   *
   * @param caseId of case to read
   * @return Optional containing case for Id if available
   * @throws CTPException for error reading case
   */
  Optional<CachedCase> readCachedCaseById(final UUID caseId) throws CTPException;
}
