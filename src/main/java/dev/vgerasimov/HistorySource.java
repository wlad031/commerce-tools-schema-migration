package dev.vgerasimov;

import java.util.List;

public interface HistorySource {

  List<HistoryRecord> getHistory();

  void saveRecord(HistoryRecord historyRecord);
}
