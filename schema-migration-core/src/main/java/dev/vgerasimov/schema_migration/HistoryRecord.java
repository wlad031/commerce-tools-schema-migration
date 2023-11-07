package dev.vgerasimov.schema_migration;

import java.time.LocalDateTime;
import lombok.Data;

/** The history record of the migration process. */
@Data
public class HistoryRecord {
  private final String id;
  private final LocalDateTime executedAt;
  private final Status status;
  private final String checksum;

  /** The status of the migration process. */
  public enum Status {
    SUCCESS,
    FAILED
  }
}
