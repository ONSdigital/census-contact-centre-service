package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Optional;
import java.util.TreeSet;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;

/**
 * Hold a time ordered collection of cases, which may have been populated by RM or our local cache,
 * and sorted by latest date first , where each object takes it's date in the following order:
 *
 * <ol>
 *   <li>lastUpdated
 *   <li>createdDateTime
 * </ol>
 */
public class TimeOrderedCases {

  static class CaseDateComparator implements Comparator<CaseDTO> {
    @Override
    public int compare(CaseDTO first, CaseDTO second) {
      Date firstDate = dateForCompare(first);
      Date secondDate = dateForCompare(second);
      return firstDate.compareTo(secondDate);
    }

    private Date dateForCompare(CaseDTO caze) {
      Date d = caze.getLastUpdated();
      return d == null ? caze.getCreatedDateTime() : d;
    }
  }

  private TreeSet<CaseDTO> caseSet = new TreeSet<>(new CaseDateComparator());

  public TimeOrderedCases() {}

  public void add(Collection<CaseDTO> cases) {
    caseSet.addAll(cases);
  }

  public void addCase(CaseDTO caseToAdd) {
    caseSet.add(caseToAdd);
  }

  public Optional<CaseDTO> latest() {
    return caseSet.size() == 0 ? Optional.empty() : Optional.of(caseSet.last());
  }
}
