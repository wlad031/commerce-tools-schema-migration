package dev.vgerasimov.schema_migration;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * Represents the result of the schema migration.
 *
 * @implNote This class is supposed to be a "sealed" class, i.e. all its subclasses are supposed to
 *     be declared in the same file.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public abstract class MigrationResult {

  /** Represents a successful migration. */
  @RequiredArgsConstructor(access = AccessLevel.PUBLIC)
  @ToString
  @EqualsAndHashCode(callSuper = true)
  public static class Success extends MigrationResult {
    @Getter private final List<ChangeSet.Result> changeSetResults;
  }

  /** Represents a failed migration. */
  @RequiredArgsConstructor(access = AccessLevel.PUBLIC)
  @ToString
  @EqualsAndHashCode(callSuper = true)
  public static class ApplicationFailed extends MigrationResult {
    @Getter private final List<ChangeSet.Result> changeSetResults;
  }

  /** Represents a migration which is failed because of missing change set. */
  @RequiredArgsConstructor(access = AccessLevel.PUBLIC)
  @ToString
  @EqualsAndHashCode(callSuper = true)
  public static class MissingChangeSet extends MigrationResult {
    @Getter private final String historyRecordId;
    @Getter private final String historyRecordChecksum;
    @Getter private final String changeSetId;
    @Getter private final String changeSetChecksum;
  }

  /** Represents a migration which is failed because of checksum mismatch. */
  @RequiredArgsConstructor(access = AccessLevel.PUBLIC)
  @ToString
  @EqualsAndHashCode(callSuper = true)
  public static class ChecksumMismatch extends MigrationResult {
    @Getter private final String historyRecordId;
    @Getter private final String historyRecordChecksum;
    @Getter private final String changeSetId;
    @Getter private final String changeSetChecksum;
  }
}
