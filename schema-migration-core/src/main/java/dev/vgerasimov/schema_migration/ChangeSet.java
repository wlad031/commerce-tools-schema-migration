package dev.vgerasimov.schema_migration;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public abstract class ChangeSet<C extends Context>
    implements Function<C, ChangeSet.Result>, Serializable {
  @Getter private final String id;
  @Getter private final String checksum;
  @Getter private final boolean skipChecksumValidation;

  public abstract Status mutate(C context);

  @Override
  public Result apply(C context) {
    Status status = mutate(context);
    LocalDateTime now = LocalDateTime.now(context.getClock());
    return new Result(id, status, now);
  }

  @Data
  public static class Result {
    private final String id;
    private final Status status;
    private final LocalDateTime executedAt;
  }

  public enum Status {
    ALREADY_APPLIED,
    APPLIED,
    SKIPPED,
    FAILED
  }
}
