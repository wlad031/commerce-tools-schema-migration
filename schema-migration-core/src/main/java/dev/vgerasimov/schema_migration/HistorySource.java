package dev.vgerasimov.schema_migration;

import java.util.List;

public interface HistorySource {

  List<HistoryRecord> getHistory();

  void saveRecord(HistoryRecord historyRecord);
}
