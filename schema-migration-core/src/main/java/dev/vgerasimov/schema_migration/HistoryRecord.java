package dev.vgerasimov.schema_migration;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HistoryRecord {
  private final String id;
  private final LocalDateTime executedAt;
  private final Status status;
  private final String checksum;

  public enum Status {
    SUCCESS,
    FAILED
  }
}
