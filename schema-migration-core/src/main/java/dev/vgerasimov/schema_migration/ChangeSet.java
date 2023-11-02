package dev.vgerasimov.schema_migration;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Function;

public abstract class ChangeSet<C extends Context> implements Function<C, ChangeSet.Result>, Serializable {
  private final String id;

  protected ChangeSet(String id) {
    this.id = id;
  }

  public abstract Status mutate(C context);

  @Override
  public Result apply(C context) {
    Status status = mutate(context);
    LocalDateTime now = LocalDateTime.now(context.getClock());
    return new Result(id, status, now);
  }

  public String getId() {
    return id;
  }

  public abstract String getChecksum();

  @Override
  public String toString() {
    return String.format("ChangeSet{%s, %s}", id, getChecksum());
  }

  public static class Result {
    private final String id;
    private final Status status;
    private final LocalDateTime executedAt;

    public Result(String id, Status status, LocalDateTime executedAt) {
      this.id = id;
      this.status = status;
      this.executedAt = executedAt;
    }

    public String getId() {
      return id;
    }

    public Status getStatus() {
      return status;
    }

    public LocalDateTime getExecutedAt() {
      return executedAt;
    }

    @Override
    public String toString() {
      return "Result{" + "id=" + id + ", status=" + status + ", executedAt=" + executedAt + '}';
    }

    @Override
    public boolean equals(Object object) {
      if (this == object) return true;
      if (object == null || getClass() != object.getClass()) return false;
      Result result = (Result) object;
      return status == result.status
          && Objects.equals(id, result.id)
          && Objects.equals(executedAt, result.executedAt);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, status, executedAt);
    }
  }

  public enum Status {
    APPLIED,
    SKIPPED,
    FAILED
  }
}