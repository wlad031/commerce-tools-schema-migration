package dev.vgerasimov.schema_migration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Simple implementation of {@link HistorySource} that stores all records in memory. Useful for
 * testing only.
 */
public class InMemoryHistorySource implements HistorySource {
  private final LinkedHashMap<String, HistoryRecord> records = new LinkedHashMap<>();

  @Override
  public List<HistoryRecord> getHistory() {
    return new ArrayList<>(records.values());
  }

  @Override
  public void saveRecord(HistoryRecord historyRecord) {
    records.put(historyRecord.getId(), historyRecord);
  }
}
