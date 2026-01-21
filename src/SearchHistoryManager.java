import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONException;
import cc.nnproject.json.JSONObject;
import java.util.Vector;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStoreException;

public class SearchHistoryManager {
  private static SearchHistoryManager instance;
  private static final int MAX_HISTORY_SIZE = 50;

  public static SearchHistoryManager getInstance() {
    if (instance == null) {
      instance = new SearchHistoryManager();
    }
    return instance;
  }

  private final RecordStoreManager storage;

  private SearchHistoryManager() {
    storage = new RecordStoreManager(Configuration.STORAGE_SEARCH_HISTORY);
  }

  public void addSearch(String query, String searchType) {
    if (query == null || query.trim().length() == 0) {
      return;
    }
    try {
      // Remove if already exists
      removeSearch(query, searchType);

      // Create search entry with timestamp
      JSONObject entry = new JSONObject();
      entry.put("query", query.trim());
      entry.put("type", searchType);
      entry.put("timestamp", System.currentTimeMillis());

      storage.addRecord(entry.toString());

      // Limit history size
      trimHistory();
    } catch (RecordStoreException e) {
      e.printStackTrace();
    }
  }

  public Vector getSearchHistory() {
    Vector searches = new Vector();
    try {
      RecordEnumeration enumeration = storage.enumerateRecords();
      try {
        while (enumeration.hasNextElement()) {
          String data = storage.getRecordAsString(enumeration.nextRecordId());
          JSONObject json = JSON.getObject(data);
          SearchEntry entry = new SearchEntry();
          entry.query = json.getString("query", "");
          entry.type = json.getString("type", Configuration.SEARCH_TRACK);
          entry.timestamp = json.getLong("timestamp", 0);
          searches.addElement(entry);
        }
      } finally {
        enumeration.destroy();
      }
    } catch (RecordStoreException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    }

    // Sort by timestamp (most recent first)
    for (int i = 0; i < searches.size() - 1; i++) {
      for (int j = i + 1; j < searches.size(); j++) {
        SearchEntry e1 = (SearchEntry) searches.elementAt(i);
        SearchEntry e2 = (SearchEntry) searches.elementAt(j);
        if (e2.timestamp > e1.timestamp) {
          searches.setElementAt(e2, i);
          searches.setElementAt(e1, j);
        }
      }
    }

    return searches;
  }

  public void clearHistory() {
    try {
      RecordEnumeration enumeration = storage.enumerateRecords();
      try {
        while (enumeration.hasNextElement()) {
          int recordId = enumeration.nextRecordId();
          storage.deleteRecord(recordId);
        }
      } finally {
        enumeration.destroy();
      }
    } catch (RecordStoreException e) {
      e.printStackTrace();
    }
  }

  private void removeSearch(String query, String type) {
    try {
      RecordEnumeration enumeration = storage.enumerateRecords();
      try {
        while (enumeration.hasNextElement()) {
          int recordId = enumeration.nextRecordId();
          String data = storage.getRecordAsString(recordId);
          JSONObject json = JSON.getObject(data);
          if (query.equals(json.getString("query", "")) && type.equals(json.getString("type", ""))) {
            storage.deleteRecord(recordId);
            return;
          }
        }
      } finally {
        enumeration.destroy();
      }
    } catch (RecordStoreException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  private void trimHistory() {
    try {
      Vector entries = new Vector();
      RecordEnumeration enumeration = storage.enumerateRecords();
      try {
        while (enumeration.hasNextElement()) {
          int recordId = enumeration.nextRecordId();
          String data = storage.getRecordAsString(recordId);
          JSONObject json = JSON.getObject(data);
          long timestamp = json.getLong("timestamp", 0);
          entries.addElement(new TimestampEntry(recordId, timestamp));
        }
      } finally {
        enumeration.destroy();
      }

      // Sort by timestamp (oldest first)
      for (int i = 0; i < entries.size() - 1; i++) {
        for (int j = i + 1; j < entries.size(); j++) {
          TimestampEntry e1 = (TimestampEntry) entries.elementAt(i);
          TimestampEntry e2 = (TimestampEntry) entries.elementAt(j);
          if (e1.timestamp > e2.timestamp) {
            entries.setElementAt(e2, i);
            entries.setElementAt(e1, j);
          }
        }
      }

      // Remove oldest entries if exceeding max size
      while (entries.size() > MAX_HISTORY_SIZE) {
        TimestampEntry oldest = (TimestampEntry) entries.elementAt(0);
        storage.deleteRecord(oldest.recordId);
        entries.removeElementAt(0);
      }
    } catch (RecordStoreException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  public void close() {
    storage.closeRecordStore();
  }

  public static class SearchEntry {
    public String query;
    public String type;
    public long timestamp;
  }

  private class TimestampEntry {
    int recordId;
    long timestamp;

    TimestampEntry(int recordId, long timestamp) {
      this.recordId = recordId;
      this.timestamp = timestamp;
    }
  }
}
