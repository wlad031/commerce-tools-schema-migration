package dev.vgerasimov.schema_migration;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public class Schema<C extends Context> implements BiFunction<C, List<ChangeSet<C>>, Schema.Result> {

  private final HistorySource historySource;

  @Override
  public Result apply(C context, List<ChangeSet<C>> changeSets) {
    if (changeSets == null) {
      throw new IllegalArgumentException("Change sets list must not be null");
    }

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

      if (!changeSet.isSkipChecksumValidation()
          && !Objects.equals(historyRecord.getChecksum(), changeSet.getChecksum())) {
        return new Result.ChecksumMismatch(
            historyRecord.getId(),
            changeSet.getChecksum(),
            changeSet.getId(),
            historyRecord.getChecksum());
      }

      if (historyRecord.getStatus() == HistoryRecord.Status.FAILED) {
        toApply.add(changeSet);
        continue;
      }

      verified.add(historyRecord);
    }

    var applicationResult = applyAndSaveRecords(context, toApply);

    Function<List<ChangeSet.Result>, Result> resultConstructor;
    if (applicationResult.first) resultConstructor = Result.Success::new;
    else resultConstructor = Result.ApplicationFailed::new;
    return resultConstructor.apply(
        Stream.concat(
                verified.stream()
                    .map(
                        changeSet ->
                            new ChangeSet.Result(
                                changeSet.getId(),
                                ChangeSet.Status.ALREADY_APPLIED,
                                changeSet.getExecutedAt())),
                applicationResult.second.stream())
            .collect(toList()));
  }

  private Pair<Boolean, List<ChangeSet.Result>> applyAndSaveRecords(
      C context, ArrayList<ChangeSet<C>> changeSetsToApply) {
    ArrayList<ChangeSet.Result> applicationResults = new ArrayList<>();
    boolean failed = false;
    for (ChangeSet<C> changeSet : changeSetsToApply) {
      if (failed) {
        applicationResults.add(
            new ChangeSet.Result(changeSet.getId(), ChangeSet.Status.SKIPPED, null));
        continue;
      }
      ChangeSet.Result changeSetApplicationResult;
      try {
        changeSetApplicationResult = changeSet.apply(context);
      } catch (Exception e) {
        changeSetApplicationResult =
            new ChangeSet.Result(changeSet.getId(), ChangeSet.Status.FAILED, null);
      }
      failed = changeSetApplicationResult.getStatus() == ChangeSet.Status.FAILED;
      historySource.saveRecord(
          new HistoryRecord(
              changeSet.getId(),
              changeSetApplicationResult.getExecutedAt(),
              changeSetApplicationResult.getStatus() == ChangeSet.Status.FAILED
                  ? HistoryRecord.Status.FAILED
                  : HistoryRecord.Status.SUCCESS,
              changeSet.getChecksum()));
      applicationResults.add(changeSetApplicationResult);
    }
    return new Pair<>(!failed, applicationResults);
  }

  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  @EqualsAndHashCode
  public abstract static class Result {

    @RequiredArgsConstructor(access = AccessLevel.PUBLIC)
    @ToString
    @EqualsAndHashCode(callSuper = true)
    public static class Success extends Result {
      @Getter private final List<ChangeSet.Result> changeSetResults;
    }

    @RequiredArgsConstructor(access = AccessLevel.PUBLIC)
    @ToString
    @EqualsAndHashCode(callSuper = true)
    public static class ApplicationFailed extends Result {
      @Getter private final List<ChangeSet.Result> changeSetResults;
    }

    @RequiredArgsConstructor(access = AccessLevel.PUBLIC)
    @ToString
    @EqualsAndHashCode(callSuper = true)
    public static class MissingChangeSet extends Result {
      @Getter private final String historyRecordId;
      @Getter private final String historyRecordChecksum;
      @Getter private final String changeSetId;
      @Getter private final String changeSetChecksum;
    }

    @RequiredArgsConstructor(access = AccessLevel.PUBLIC)
    @ToString
    @EqualsAndHashCode(callSuper = true)
    public static class ChecksumMismatch extends Result {
      @Getter private final String historyRecordId;
      @Getter private final String historyRecordChecksum;
      @Getter private final String changeSetId;
      @Getter private final String changeSetChecksum;
    }
  }

  // Who needs popular utilities like Pair from Apache Commons?
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  private static final class Pair<T, S> {
    private final T first;
    private final S second;
  }
}
