package dev.vgerasimov.schema_migration;

import java.util.function.Function;

public interface ChecksumCalculator<C extends Context, CS extends ChangeSet<C>>
    extends Function<CS, String> {

  class ChecksumCalculationException extends RuntimeException {

    public ChecksumCalculationException(String message) {
      super(message);
    }

    public ChecksumCalculationException(String message, Throwable cause) {
      super(message, cause);
    }

    public ChecksumCalculationException(Throwable cause) {
      super(cause);
    }
  }
}
