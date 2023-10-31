package dev.vgerasimov;

import java.time.LocalDateTime;
import java.util.Objects;

public class HistoryRecord {
  private final String id;
  private final LocalDateTime executedAt;
  private final Status status;
  private final String checksum;

  public HistoryRecord(String id, LocalDateTime executedAt, Status status, String checksum) {
    this.id = id;
    this.executedAt = executedAt;
    this.status = status;
    this.checksum = checksum;
  }

  public String getId() {
    return id;
  }

  public LocalDateTime getExecutedAt() {
    return executedAt;
  }

  public Status getStatus() {
    return status;
  }

  public String getChecksum() {
    return checksum;
  }

  @Override
  public String toString() {
    return String.format("HistoryRecord{%s, %s, %s, %s}", id, executedAt, status, checksum);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) return true;
    if (object == null || getClass() != object.getClass()) return false;
    HistoryRecord that = (HistoryRecord) object;
    return Objects.equals(id, that.id)
        && Objects.equals(executedAt, that.executedAt)
        && status == that.status
        && Objects.equals(checksum, that.checksum);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, executedAt, status, checksum);
  }

  public enum Status {
    APPLIED,
    FAILED
  }
}
