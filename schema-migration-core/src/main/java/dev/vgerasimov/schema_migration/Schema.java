package dev.vgerasimov.schema_migration;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * A schema migration tool. It applies change sets to the database and saves the history of applied
 * change sets. "Database" here is a generic term, it can be any kind of storage, and this class
 * does not know anything about the storage details.
 *
 * <p>Essentially, this class is a {@link BiFunction} that takes a "context" and a list of change
 * sets. The context is an object that contains everything that is needed to apply change sets. The
 * return type of the function is a {@link MigrationResult} containing the results of applying change sets.
 *
 * @param <C> the type of the context
 */
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public class Schema<C extends Context> implements BiFunction<C, List<ChangeSet<C>>, MigrationResult> {

  private final HistorySource historySource;

  @Override
  public MigrationResult apply(C context, List<ChangeSet<C>> changeSets) {
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
        return new MigrationResult.MissingChangeSet(
            historyRecord.getId(), historyRecord.getChecksum(), null, null);
      }

      if (!Objects.equals(historyRecord.getId(), changeSet.getId())) {
        return new MigrationResult.MissingChangeSet(
            historyRecord.getId(),
            historyRecord.getChecksum(),
            changeSet.getId(),
            changeSet.getChecksum());
      }

      if (!changeSet.isSkipChecksumValidation()
          && !Objects.equals(historyRecord.getChecksum(), changeSet.getChecksum())) {
        return new MigrationResult.ChecksumMismatch(
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
    List<ChangeSet.Result> results =
        Stream.concat(
                verified.stream()
                    .map(
                        changeSet ->
                            new ChangeSet.Result(
                                changeSet.getId(),
                                ChangeSet.Status.ALREADY_APPLIED,
                                changeSet.getExecutedAt())),
                applicationResult.second.stream())
            .collect(toList());
    return applicationResult.first
        ? new MigrationResult.Success(results)
        : new MigrationResult.ApplicationFailed(results);
  }

  /**
   * Returns a pair of boolean and list of change set results. The boolean value indicates whether
   * all change sets were applied successfully.
   */
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

  /**
   * A pair of two values.
   *
   * @param <T> the type of the first value
   * @param <S> the type of the second value
   */
  // Who needs popular utilities like Pair from Apache Commons?
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  private static final class Pair<T, S> {
    private final T first;
    private final S second;
  }
}
