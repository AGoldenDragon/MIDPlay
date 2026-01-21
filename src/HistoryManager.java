import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONException;
import cc.nnproject.json.JSONObject;
import java.util.Vector;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStoreException;
import model.Track;
import model.Tracks;

public class HistoryManager {
  private static HistoryManager instance;
  private static final int MAX_HISTORY_SIZE = 100;

  public static HistoryManager getInstance() {
    if (instance == null) {
      instance = new HistoryManager();
    }
    return instance;
  }

  private final RecordStoreManager storage;

  private HistoryManager() {
    storage = new RecordStoreManager(Configuration.STORAGE_HISTORY);
  }

  public void addToHistory(Track track) {
    if (track == null) {
      return;
    }
    try {
      // Remove if already exists to avoid duplicates
      removeTrackFromHistory(track);

      // Create history entry with timestamp
      JSONObject entry = new JSONObject();
      entry.put("track", track.toJSON());
      entry.put("timestamp", System.currentTimeMillis());

      storage.addRecord(entry.toString());

      // Limit history size
      trimHistory();
    } catch (RecordStoreException e) {
      e.printStackTrace();
    }
  }

  public Tracks getHistory() {
    Vector trackVector = new Vector();
    try {
      RecordEnumeration enumeration = storage.enumerateRecords();
      try {
        while (enumeration.hasNextElement()) {
          String data = storage.getRecordAsString(enumeration.nextRecordId());
          JSONObject json = JSON.getObject(data);
          Track track = new Track().fromJSON(json.getObject("track").toString());
          if (track != null) {
            trackVector.addElement(track);
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

    // Reverse to show most recent first
    Vector reversed = new Vector();
    for (int i = trackVector.size() - 1; i >= 0; i--) {
      reversed.addElement(trackVector.elementAt(i));
    }

    return vectorToTracks(reversed);
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

  private void removeTrackFromHistory(Track track) {
    try {
      RecordEnumeration enumeration = storage.enumerateRecords();
      try {
        while (enumeration.hasNextElement()) {
          int recordId = enumeration.nextRecordId();
          String data = storage.getRecordAsString(recordId);
          JSONObject json = JSON.getObject(data);
          Track storedTrack = new Track().fromJSON(json.getObject("track").toString());
          if (storedTrack != null && storedTrack.isSame(track)) {
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
          entries.addElement(new HistoryEntry(recordId, timestamp));
        }
      } finally {
        enumeration.destroy();
      }

      // Sort by timestamp (oldest first)
      for (int i = 0; i < entries.size() - 1; i++) {
        for (int j = i + 1; j < entries.size(); j++) {
          HistoryEntry e1 = (HistoryEntry) entries.elementAt(i);
          HistoryEntry e2 = (HistoryEntry) entries.elementAt(j);
          if (e1.timestamp > e2.timestamp) {
            entries.setElementAt(e2, i);
            entries.setElementAt(e1, j);
          }
        }
      }

      // Remove oldest entries if exceeding max size
      while (entries.size() > MAX_HISTORY_SIZE) {
        HistoryEntry oldest = (HistoryEntry) entries.elementAt(0);
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

  private Tracks vectorToTracks(Vector trackVector) {
    Tracks result = new Tracks();
    if (trackVector.isEmpty()) {
      result.setTracks(new Track[0]);
      result.setHasMore(false);
      return result;
    }
    Track[] trackArray = new Track[trackVector.size()];
    for (int i = 0; i < trackVector.size(); i++) {
      trackArray[i] = (Track) trackVector.elementAt(i);
    }
    result.setTracks(trackArray);
    result.setHasMore(false);
    return result;
  }

  private class HistoryEntry {
    int recordId;
    long timestamp;

    HistoryEntry(int recordId, long timestamp) {
      this.recordId = recordId;
      this.timestamp = timestamp;
    }
  }
}
