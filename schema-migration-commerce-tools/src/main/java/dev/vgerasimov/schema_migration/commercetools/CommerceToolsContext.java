package dev.vgerasimov.schema_migration.commercetools;

import com.commercetools.api.client.ProjectApiRoot;
import dev.vgerasimov.schema_migration.Context;

import java.time.Clock;

public class CommerceToolsContext implements Context {

  private final Clock clock;
  private final ProjectApiRoot apiRoot;

  public CommerceToolsContext(Clock clock, ProjectApiRoot apiRoot) {
    this.clock = clock;
    this.apiRoot = apiRoot;
  }

  @Override
  public Clock getClock() {
    return clock;
  }

  public ProjectApiRoot getApiRoot() {
    return apiRoot;
  }
}
