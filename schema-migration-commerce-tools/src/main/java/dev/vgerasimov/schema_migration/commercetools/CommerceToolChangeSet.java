package dev.vgerasimov.schema_migration.commercetools;

import dev.vgerasimov.schema_migration.ChangeSet;

import java.io.*;
import java.util.List;
import java.util.function.Function;

public abstract class CommerceToolChangeSet extends ChangeSet<CommerceToolsContext>
    implements Serializable {

  private final transient String checksum;

  private CommerceToolChangeSet(String id, String checksum) {
    super(id, checksum, checksum == null);
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

  public static void main(String[] args){
    List<CommerceToolChangeSet> changeSets = getChangeSets();
    for (CommerceToolChangeSet changeSet : changeSets) {
      System.out.println(changeSet.getChecksum());
    }
  }

  static List<CommerceToolChangeSet> getChangeSets() {
    return List.of(
            new CommerceToolChangeSet("id", null) {
              @Override
              public Status mutate(CommerceToolsContext context) {
                System.out.println("2");
                System.out.println("2");
                return Status.APPLIED;
              }
            });
  }
}
