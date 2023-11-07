package dev.vgerasimov.schema_migration;

import java.util.List;

/**
 * The source of the history records. It is used to get the history of the migration process and to
 * save new records.
 */
public interface HistorySource {

  /**
   * Returns a list of all history records. The order is guaranteed to be ascending by {@link
   * HistoryRecord#getId()}.
   */
  List<HistoryRecord> getHistory();

  /** Saves a new history record. */
  void saveRecord(HistoryRecord historyRecord);
}
