package dev.vgerasimov.schema_migration.commercetools;

import com.commercetools.api.client.ProjectApiRoot;
import dev.vgerasimov.schema_migration.ChangeSet;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;

public abstract class CommerceToolChangeSet extends ChangeSet<CommerceToolsContext> implements Serializable {

  private final transient String checksum;

  private CommerceToolChangeSet(String id, String checksum) {
    super(id);
    this.checksum = checksum;
  }

  public static CommerceToolChangeSet of(
      String id, String checksum, Function<CommerceToolsContext, Status> mutate) {
    return new CommerceToolChangeSet(id, checksum) {
      @Override
      public Status mutate(CommerceToolsContext context) {
        return mutate.apply(context);
      }
    };
  }

  @Override
  public String getChecksum() {
    return checksum;
  }

  static List<CommerceToolChangeSet> getChangeSets() {
    return List.of(
        of(
            /* id= */ "1",
            /* checksum= */ "1",
            context -> {
              ProjectApiRoot apiRoot = context.getApiRoot();
              return Status.APPLIED;
            }),
        of(
            /* id= */ "2",
            /* checksum= */ "2",
            context -> {
              ProjectApiRoot apiRoot = context.getApiRoot();
              return null;
            }));
  }
}
