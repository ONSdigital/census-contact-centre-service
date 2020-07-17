package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.Test;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;

public class TimeOrderedCasesTest {
  private static final Date DATE1 = makeDate(2020, 6, 14);
  private static final Date DATE2 = makeDate(2020, 6, 15);
  private static final Date DATE3 = makeDate(2021, 2, 3);
  private static final Date DATE4 = makeDate(2021, 2, 4);

  private TimeOrderedCases timeOrderedCases = new TimeOrderedCases();

  private List<CaseDTO> caseList = new ArrayList<>();

  private static Date makeDate(int year, int month, int day) {
    return Date.from(LocalDateTime.of(year, month, day, 0, 0, 0).toInstant(ZoneOffset.UTC));
  }

  @Test
  public void shouldReturnEmptyWithNoContents() {
    assertTrue(timeOrderedCases.latest().isEmpty());
  }

  @Test
  public void shouldFindOnlyResult() {
    CaseDTO c = createCase(null, DATE1);
    caseList.add(c);
    timeOrderedCases.add(caseList);
    assertTrue(timeOrderedCases.latest().isPresent());
    assertEquals(c, timeOrderedCases.latest().get());
  }

  @Test
  public void shouldFindLatestUpdated() {
    CaseDTO c1 = createCase(null, DATE1);
    CaseDTO c2 = createCase(null, DATE2);
    caseList.add(c1);
    caseList.add(c2);
    timeOrderedCases.add(caseList);
    assertTrue(timeOrderedCases.latest().isPresent());
    assertEquals(c2, timeOrderedCases.latest().get());
  }

  @Test
  public void shouldFindLatestCreated() {
    CaseDTO c1 = createCase(DATE1, null);
    CaseDTO c2 = createCase(DATE2, null);
    caseList.add(c1);
    caseList.add(c2);
    timeOrderedCases.add(caseList);
    assertTrue(timeOrderedCases.latest().isPresent());
    assertEquals(c2, timeOrderedCases.latest().get());
  }

  @Test
  public void shouldFindLatestWhereDateUpdatedPreferred() {
    CaseDTO c1 = createCase(DATE1, DATE2);
    CaseDTO c2 = createCase(DATE2, DATE1);
    caseList.add(c1);
    caseList.add(c2);
    timeOrderedCases.add(caseList);
    assertTrue(timeOrderedCases.latest().isPresent());
    assertEquals(c1, timeOrderedCases.latest().get());
  }

  @Test
  public void shouldFindLatestFromMixedListWhereLastUpdatedWins() {
    CaseDTO c1 = createCase(DATE1, DATE2);
    CaseDTO c2 = createCase(DATE1, null);
    CaseDTO c3 = createCase(DATE1, DATE3);
    CaseDTO c4 = createCase(DATE4, DATE1);
    CaseDTO c5 = createCase(null, DATE1);
    CaseDTO c6 = createCase(null, DATE2);
    caseList.addAll(List.of(c1, c2, c3, c4, c5, c6));
    timeOrderedCases.add(caseList);
    assertTrue(timeOrderedCases.latest().isPresent());
    assertEquals(c3, timeOrderedCases.latest().get());
  }

  @Test
  public void shouldFindLatestFromMixedListWhereCreatedWins() {
    CaseDTO c1 = createCase(DATE1, DATE2);
    CaseDTO c2 = createCase(DATE1, null);
    CaseDTO c3 = createCase(DATE1, DATE3);
    CaseDTO c4 = createCase(DATE4, null);
    CaseDTO c5 = createCase(null, DATE1);
    CaseDTO c6 = createCase(null, DATE2);
    caseList.addAll(List.of(c1, c2, c3, c4, c5, c6));
    timeOrderedCases.add(caseList);
    assertTrue(timeOrderedCases.latest().isPresent());
    assertEquals(c4, timeOrderedCases.latest().get());
  }

  private CaseDTO createCase(Date created, Date updated) {
    assertFalse(
        "Don't create a test for object with no dates set", created == null && updated == null);
    return CaseDTO.builder().createdDateTime(created).lastUpdated(updated).build();
  }
}
