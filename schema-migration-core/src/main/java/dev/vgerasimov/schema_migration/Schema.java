package dev.vgerasimov.schema_migration;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

public class Schema<C extends Context> implements Function<C, Schema.Result> {

  private final HistorySource historySource;
  private final ArrayList<ChangeSet<C>> changeSets;

  public Schema(HistorySource historySource) {
    this.historySource = historySource;
    this.changeSets = new ArrayList<>();
  }

  public Schema<C> add(ChangeSet<C> changeSet) {
    changeSets.add(changeSet);
    return this;
  }

  @Override
  public Result apply(C context) {
    List<HistoryRecord> history = historySource.getHistory();

    ArrayList<HistoryRecord> verified = new ArrayList<>();
    ArrayList<ChangeSet<C>> toApply = new ArrayList<>();

    for (int i = 0; i < Math.max(history.size(), changeSets.size()); i++) {
      HistoryRecord historyRecord = i >= history.size() ? null : history.get(i);
      ChangeSet<C> changeSet = i >= changeSets.size() ? null : changeSets.get(i);

      if (historyRecord == null) {
        toApply.add(changeSet);
        continue;
      }

      if (changeSet == null) {
        return new Result.MissingChangeSet(
            historyRecord.getId(), historyRecord.getChecksum(), null, null);
      }

      if (!Objects.equals(historyRecord.getId(), changeSet.getId())) {
        return new Result.MissingChangeSet(
            historyRecord.getId(),
            historyRecord.getChecksum(),
            changeSet.getId(),
            changeSet.getChecksum());
      }

      if (!Objects.equals(historyRecord.getChecksum(), changeSet.getChecksum())) {
        return new Result.ChecksumMismatch(
            historyRecord.getId(),
            changeSet.getChecksum(),
            changeSet.getId(),
            historyRecord.getChecksum());
      }

      verified.add(historyRecord);
    }

    if (toApply.isEmpty()) {
      return new Result.NoChanges();
    }

    ArrayList<ChangeSet.Result> applicationResults = new ArrayList<>();
    for (ChangeSet<C> changeSet : toApply) {
      ChangeSet.Result result = changeSet.apply(context);
      historySource.saveRecord(
          new HistoryRecord(
              changeSet.getId(),
              result.getExecutedAt(),
              HistoryRecord.Status.APPLIED,
              changeSet.getChecksum()));
      applicationResults.add(result);
    }

    return new Result.Applied(
        Stream.concat(
                verified.stream()
                    .map(
                        changeSet ->
                            new ChangeSet.Result(
                                changeSet.getId(),
                                ChangeSet.Status.SKIPPED,
                                changeSet.getExecutedAt())),
                applicationResults.stream())
            .collect(toList()));
  }

  public abstract static class Result {

    public static class NoChanges extends Result {

      @Override
      public String toString() {
        return "NoChanges{}";
      }
    }

    public static class Applied extends Result {
      private final List<ChangeSet.Result> changeSetResults;

      public Applied(List<ChangeSet.Result> changeSetResults) {
        this.changeSetResults = changeSetResults;
      }

      @Override
      public String toString() {
        return "Applied{" + "changeSetResults=" + changeSetResults + '}';
      }

      @Override
      public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Applied applied = (Applied) object;
        return Objects.equals(changeSetResults, applied.changeSetResults);
      }

      @Override
      public int hashCode() {
        return Objects.hash(changeSetResults);
      }
    }

    public static class MissingChangeSet extends Result {
      private final String historyRecordId;
      private final String historyRecordChecksum;
      private final String changeSetId;
      private final String changeSetChecksum;

      public MissingChangeSet(
          String historyRecordId,
          String historyRecordChecksum,
          String changeSetId,
          String changeSetChecksum) {
        this.historyRecordId = historyRecordId;
        this.historyRecordChecksum = historyRecordChecksum;
        this.changeSetId = changeSetId;
        this.changeSetChecksum = changeSetChecksum;
      }

      @Override
      public String toString() {
        return String.format(
            "MissingChangeSet{historyRecordId=%s, historyRecordChecksum=%s, changeSetId=%s, changeSetChecksum=%s}",
            historyRecordId, historyRecordChecksum, changeSetId, changeSetChecksum);
      }
    }

    public static class ChecksumMismatch extends Result {
      private final String historyRecordId;
      private final String historyRecordChecksum;
      private final String changeSetId;
      private final String changeSetChecksum;

      public ChecksumMismatch(
          String historyRecordId,
          String historyRecordChecksum,
          String changeSetId,
          String changeSetChecksum) {
        this.historyRecordId = historyRecordId;
        this.historyRecordChecksum = historyRecordChecksum;
        this.changeSetId = changeSetId;
        this.changeSetChecksum = changeSetChecksum;
      }

      @Override
      public String toString() {
        return String.format(
            "ChecksumMismatch{historyRecordId=%s, historyRecordChecksum=%s, changeSetId=%s, changeSetChecksum=%s}",
            historyRecordId, historyRecordChecksum, changeSetId, changeSetChecksum);
      }
    }
  }
}
