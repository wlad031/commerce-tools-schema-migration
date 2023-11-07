package dev.vgerasimov.schema_migration;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Simple implementation of {@link HistorySource} that stores all records in memory. Useful for
 * testing only.
 */
public class InMemoryHistorySource implements HistorySource {
  private final TreeMap<String, HistoryRecord> records = new TreeMap<>();

  @Override
  public List<HistoryRecord> getHistory() {
    return new ArrayList<>(records.values());
  }

  @Override
  public void saveRecord(HistoryRecord historyRecord) {
    records.put(historyRecord.getId(), historyRecord);
  }
}
