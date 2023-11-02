package dev.vgerasimov.schema_migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
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
  void emptyHistory_emptyChangeSets_noChanges() {
    var actual = underTest.apply(Context.of(clock));

    assertTrue(actual instanceof Schema.Result.NoChanges);
    assertIterableEquals(List.of(), historySource.getHistory());
  }

  @Test
  void emptyHistory_oneChangeSet_applied() {
    underTest.add(changeSet("1", "1", ChangeSet.Status.APPLIED));

    Schema.Result actual = underTest.apply(Context.of(clock));

    assertTrue(actual instanceof Schema.Result.Applied);
    assertEquals(
        new Schema.Result.Applied(
            List.of(new ChangeSet.Result("1", ChangeSet.Status.APPLIED, dateTime(timeStart + 0)))),
        actual);
    assertIterableEquals(
        List.of(new HistoryRecord("1", dateTime(timeStart + 0), HistoryRecord.Status.APPLIED, "1")),
        historySource.getHistory());
  }

  @Test
  void emptyHistory_twoChangeSets_applied() {
    underTest.add(changeSet("1", "1", ChangeSet.Status.APPLIED));
    underTest.add(changeSet("2", "2", ChangeSet.Status.APPLIED));

    Schema.Result actual = underTest.apply(Context.of(clock));

    assertTrue(actual instanceof Schema.Result.Applied);
    assertEquals(
        new Schema.Result.Applied(
            List.of(
                new ChangeSet.Result("1", ChangeSet.Status.APPLIED, dateTime(timeStart + 0)),
                new ChangeSet.Result("2", ChangeSet.Status.APPLIED, dateTime(timeStart + 1)))),
        actual);
    assertIterableEquals(
        List.of(
            new HistoryRecord("1", dateTime(timeStart + 0), HistoryRecord.Status.APPLIED, "1"),
            new HistoryRecord("2", dateTime(timeStart + 1), HistoryRecord.Status.APPLIED, "2")),
        historySource.getHistory());
  }

  @Test
  void oneInHistory_twoChangeSets_oneSkippedOneApplied() {
    historySource.saveRecord(
        new HistoryRecord("1", dateTime(timeStart - 9), HistoryRecord.Status.APPLIED, "1"));

    underTest.add(changeSet("1", "1", ChangeSet.Status.APPLIED));
    underTest.add(changeSet("2", "2", ChangeSet.Status.APPLIED));

    Schema.Result actual = underTest.apply(Context.of(clock));

    assertTrue(actual instanceof Schema.Result.Applied);
    assertEquals(
        new Schema.Result.Applied(
            List.of(
                new ChangeSet.Result("1", ChangeSet.Status.SKIPPED, dateTime(timeStart - 9)),
                new ChangeSet.Result("2", ChangeSet.Status.APPLIED, dateTime(timeStart + 0)))),
        actual);
    assertIterableEquals(
        List.of(
            new HistoryRecord("1", dateTime(timeStart - 9), HistoryRecord.Status.APPLIED, "1"),
            new HistoryRecord("2", dateTime(timeStart + 0), HistoryRecord.Status.APPLIED, "2")),
        historySource.getHistory());
  }

  private static <C extends Context> ChangeSet<C> changeSet(
      String id, String checksum, ChangeSet.Status status) {
    return new ChangeSet<>(id) {
      @Override
      public Status mutate(C context) {
        return status;
      }

      @Override
      public String getChecksum() {
        return checksum;
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
