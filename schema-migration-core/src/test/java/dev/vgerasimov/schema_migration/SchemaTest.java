package dev.vgerasimov.schema_migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PointlessArithmeticExpression")
class SchemaTest {

  final long timeStart = 1000;
  Clock clock = iteratingClock(timeStart);

  HistorySource historySource;

  Schema<Context> underTest;

  @BeforeEach
  void setUp() {
    historySource = new InMemoryHistorySource();
    underTest = new Schema<>(historySource);
  }

  @Test
  void nullChangeSets_exception() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> underTest.apply(Context.of(clock), null));
    assertEquals("Change sets list must not be null", exception.getMessage());
  }

  @Test
  void emptyHistory_emptyChangeSets_noChanges() {
    var actual = underTest.apply(Context.of(clock), List.of());

    assertEquals(new Schema.Result.Success(List.of()), actual);
    assertHistory();
  }

  @Test
  void emptyHistory_oneChangeSet_applied() {
    Schema.Result actual =
        underTest.apply(
            Context.of(clock), List.of(changeSet("1", "1", __ -> ChangeSet.Status.APPLIED)));

    assertEquals(
        new Schema.Result.Success(
            List.of(
                new ChangeSet.Result("1", ChangeSet.Status.APPLIED, dateTime(timeStart + 0)))),
        actual);
    assertHistory(
        new HistoryRecord("1", dateTime(timeStart + 0), HistoryRecord.Status.SUCCESS, "1"));
  }

  @Test
  void emptyHistory_twoChangeSets_applied() {
    Schema.Result actual =
        underTest.apply(
            Context.of(clock),
            List.of(
                changeSet("1", "1", __ -> ChangeSet.Status.APPLIED),
                changeSet("2", "2", __ -> ChangeSet.Status.APPLIED)));

    assertEquals(
        new Schema.Result.Success(
            List.of(
                new ChangeSet.Result("1", ChangeSet.Status.APPLIED, dateTime(timeStart + 0)),
                new ChangeSet.Result("2", ChangeSet.Status.APPLIED, dateTime(timeStart + 1)))),
        actual);
    assertHistory(
        new HistoryRecord("1", dateTime(timeStart + 0), HistoryRecord.Status.SUCCESS, "1"),
        new HistoryRecord("2", dateTime(timeStart + 1), HistoryRecord.Status.SUCCESS, "2"));
  }

  @Test
  void oneInHistory_twoChangeSets_oneSkippedOneApplied() {
    saveHistory(new HistoryRecord("1", dateTime(timeStart - 9), HistoryRecord.Status.SUCCESS, "1"));

    Schema.Result actual =
        underTest.apply(
            Context.of(clock),
            List.of(
                changeSet("1", "1", __ -> ChangeSet.Status.APPLIED),
                changeSet("2", "2", __ -> ChangeSet.Status.APPLIED)));

    assertEquals(
        new Schema.Result.Success(
            List.of(
                new ChangeSet.Result("1", ChangeSet.Status.ALREADY_APPLIED, dateTime(timeStart - 9)),
                new ChangeSet.Result("2", ChangeSet.Status.APPLIED, dateTime(timeStart + 0)))),
        actual);
    assertHistory(
        new HistoryRecord("1", dateTime(timeStart - 9), HistoryRecord.Status.SUCCESS, "1"),
        new HistoryRecord("2", dateTime(timeStart + 0), HistoryRecord.Status.SUCCESS, "2"));
  }

  @Test
  void twoInHistory_twoChangeSets_bothSkipped() {
    saveHistory(
        new HistoryRecord("1", dateTime(timeStart - 9), HistoryRecord.Status.SUCCESS, "1"),
        new HistoryRecord("2", dateTime(timeStart - 8), HistoryRecord.Status.SUCCESS, "2"));

    Schema.Result actual =
        underTest.apply(
            Context.of(clock),
            List.of(
                changeSet("1", "1", __ -> ChangeSet.Status.APPLIED),
                changeSet("2", "2", __ -> ChangeSet.Status.APPLIED)));

    assertEquals(
        new Schema.Result.Success(
            List.of(
                new ChangeSet.Result("1", ChangeSet.Status.ALREADY_APPLIED, dateTime(timeStart - 9)),
                new ChangeSet.Result(
                    "2", ChangeSet.Status.ALREADY_APPLIED, dateTime(timeStart - 8)))),
        actual);
    assertHistory(
        new HistoryRecord("1", dateTime(timeStart - 9), HistoryRecord.Status.SUCCESS, "1"),
        new HistoryRecord("2", dateTime(timeStart - 8), HistoryRecord.Status.SUCCESS, "2"));
  }

  @Test
  void oneInHistory_threeChangeSets_secondThrowsException_failedSavedAndSkippedReturned() {
    saveHistory(new HistoryRecord("1", dateTime(timeStart - 9), HistoryRecord.Status.SUCCESS, "1"));

    Schema.Result actual =
        underTest.apply(
            Context.of(clock),
            List.of(
                changeSet("1", "1", __ -> ChangeSet.Status.APPLIED),
                changeSet(
                    "2",
                    "2",
                    __ -> {
                      throw new RuntimeException();
                    }),
                changeSet("3", "3", __ -> ChangeSet.Status.APPLIED)));

    assertEquals(
        new Schema.Result.ApplicationFailed(
            List.of(
                new ChangeSet.Result("1", ChangeSet.Status.ALREADY_APPLIED, dateTime(timeStart - 9)),
                new ChangeSet.Result("2", ChangeSet.Status.FAILED, null),
                new ChangeSet.Result("3", ChangeSet.Status.SKIPPED, null))),
        actual);
    assertHistory(
        new HistoryRecord("1", dateTime(timeStart - 9), HistoryRecord.Status.SUCCESS, "1"),
        new HistoryRecord("2", null, HistoryRecord.Status.FAILED, "2"));
  }

  @Test
  void oneInHistory_threeChangeSets_secondReturnsFailed_failedSavedAndSkippedReturned() {
    saveHistory(new HistoryRecord("1", dateTime(timeStart - 9), HistoryRecord.Status.SUCCESS, "1"));

    Schema.Result actual =
        underTest.apply(
            Context.of(clock),
            List.of(
                changeSet("1", "1", __ -> ChangeSet.Status.APPLIED),
                changeSet("2", "2", __ -> ChangeSet.Status.FAILED),
                changeSet("3", "3", __ -> ChangeSet.Status.APPLIED)));

    assertEquals(
        new Schema.Result.ApplicationFailed(
            List.of(
                new ChangeSet.Result("1", ChangeSet.Status.ALREADY_APPLIED, dateTime(timeStart - 9)),
                new ChangeSet.Result("2", ChangeSet.Status.FAILED, dateTime(timeStart + 0)),
                new ChangeSet.Result("3", ChangeSet.Status.SKIPPED, null))),
        actual);
    assertHistory(
        new HistoryRecord("1", dateTime(timeStart - 9), HistoryRecord.Status.SUCCESS, "1"),
        new HistoryRecord("2", dateTime(timeStart + 0), HistoryRecord.Status.FAILED, "2"));
  }

  @Test
  void twoInHistory_secondFailed_threeChangeSets_failedRestarted() {
    saveHistory(
        new HistoryRecord("1", dateTime(timeStart - 9), HistoryRecord.Status.SUCCESS, "1"),
        new HistoryRecord("2", null, HistoryRecord.Status.FAILED, "2"));

    Schema.Result actual =
        underTest.apply(
            Context.of(clock),
            List.of(
                changeSet("1", "1", __ -> ChangeSet.Status.APPLIED),
                changeSet("2", "2", __ -> ChangeSet.Status.APPLIED),
                changeSet("3", "3", __ -> ChangeSet.Status.APPLIED)));

    assertEquals(
        new Schema.Result.Success(
            List.of(
                new ChangeSet.Result("1", ChangeSet.Status.ALREADY_APPLIED, dateTime(timeStart - 9)),
                new ChangeSet.Result("2", ChangeSet.Status.APPLIED, dateTime(timeStart + 0)),
                new ChangeSet.Result("3", ChangeSet.Status.APPLIED, dateTime(timeStart + 1)))),
        actual);
    assertHistory(
        new HistoryRecord("1", dateTime(timeStart - 9), HistoryRecord.Status.SUCCESS, "1"),
        new HistoryRecord("2", dateTime(timeStart + 0), HistoryRecord.Status.SUCCESS, "2"),
        new HistoryRecord("3", dateTime(timeStart + 1), HistoryRecord.Status.SUCCESS, "3"));
  }

  private void saveHistory(HistoryRecord... records) {
    for (HistoryRecord record : records) {
      historySource.saveRecord(record);
    }
  }

  private void assertHistory(HistoryRecord... records) {
    assertIterableEquals(List.of(records), historySource.getHistory());
  }

  private static <C extends Context> ChangeSet<C> changeSet(
      String id, Function<C, ChangeSet.Status> mutate) {
    return changeSet(id, null, mutate);
  }

  private static <C extends Context> ChangeSet<C> changeSet(
      String id, String checksum, Function<C, ChangeSet.Status> mutate) {
    return new ChangeSet<>(id, checksum, checksum == null) {
      @Override
      public Status mutate(C context) {
        return mutate.apply(context);
      }
    };
  }

  private LocalDateTime dateTime(long time) {
    return LocalDateTime.ofInstant(Instant.ofEpochSecond(time), ZoneId.of("UTC"));
  }

  private static Clock iteratingClock(long timeStart) {
    return new Clock() {
      private long time = timeStart;

      @Override
      public ZoneId getZone() {
        return ZoneId.of("UTC");
      }

      @Override
      public Clock withZone(ZoneId zone) {
        return this;
      }

      @Override
      public Instant instant() {
        return Instant.ofEpochSecond(time++);
      }
    };
  }
}
