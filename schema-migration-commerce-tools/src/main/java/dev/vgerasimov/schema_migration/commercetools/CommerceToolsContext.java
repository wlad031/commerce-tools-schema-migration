package dev.vgerasimov.schema_migration.commercetools;

import com.commercetools.api.client.ProjectApiRoot;
import dev.vgerasimov.schema_migration.Context;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Clock;

@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public class CommerceToolsContext implements Context {
  private final Clock clock;
  @Getter private final ProjectApiRoot apiRoot;

  @Override
  public Clock getClock() {
    return clock;
  }
}
