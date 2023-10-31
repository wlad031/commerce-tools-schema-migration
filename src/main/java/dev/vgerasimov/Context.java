package dev.vgerasimov;

import java.time.Clock;

public interface Context {
  Clock getClock();

  @SuppressWarnings("Convert2Lambda")
  static Context of(Clock clock) {
    return new Context() {
      @Override
      public Clock getClock() {
        return clock;
      }
    };
  }
}
