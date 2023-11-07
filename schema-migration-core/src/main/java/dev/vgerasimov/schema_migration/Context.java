package dev.vgerasimov.schema_migration;

import java.time.Clock;

/**
 * The context of the migration process. The only thing that is required for all implementations is
 * a {@link Clock} instance which is used to get the current time.
 */
public interface Context {
  Clock getClock();

  /** Creates a context with the given {@link Clock} and nothing else. Can be useful for testing. */
  static Context basic(Clock clock) {
    return () -> clock;
  }
}
