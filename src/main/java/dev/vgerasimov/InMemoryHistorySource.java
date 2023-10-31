package dev.vgerasimov;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple implementation of {@link HistorySource} that stores all records in memory. Useful for
 * testing only.
 */
public class InMemoryHistorySource implements HistorySource {
  private final ArrayList<HistoryRecord> records = new ArrayList<>();

  @Override
  public List<HistoryRecord> getHistory() {
    return records;
  }

  @Override
  public void saveRecord(HistoryRecord historyRecord) {
    records.add(historyRecord);
  }
}
