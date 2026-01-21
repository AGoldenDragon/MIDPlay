import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONException;
import cc.nnproject.json.JSONObject;
import java.util.Vector;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStoreException;
import model.Track;

public class StatisticsManager {
  private static StatisticsManager instance;

  public static StatisticsManager getInstance() {
    if (instance == null) {
      instance = new StatisticsManager();
    }
    return instance;
  }

  private final RecordStoreManager storage;

  private StatisticsManager() {
    storage = new RecordStoreManager(Configuration.STORAGE_STATISTICS);
  }

  public void incrementPlayCount(Track track) {
    if (track == null) {
      return;
    }
    try {
      TrackStats stats = getTrackStats(track);
      if (stats == null) {
        stats = new TrackStats(track);
      }
      stats.playCount++;
      stats.lastPlayed = System.currentTimeMillis();
      stats.totalDuration += track.getDuration();
      saveTrackStats(stats);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public TrackStats getTrackStats(Track track) {
    if (track == null) {
      return null;
    }
    try {
      RecordEnumeration enumeration = storage.enumerateRecords();
      try {
        while (enumeration.hasNextElement()) {
          int recordId = enumeration.nextRecordId();
          String data = storage.getRecordAsString(recordId);
          JSONObject json = JSON.getObject(data);
          Track storedTrack = new Track().fromJSON(json.getObject("track").toString());
          if (storedTrack != null && storedTrack.isSame(track)) {
            TrackStats stats = new TrackStats(storedTrack);
            stats.playCount = json.getInt("playCount", 0);
            stats.lastPlayed = json.getLong("lastPlayed", 0);
            stats.totalDuration = json.getInt("totalDuration", 0);
            stats.recordId = recordId;
            return stats;
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
    return null;
  }

  public Vector getAllStats() {
    Vector statsList = new Vector();
    try {
      RecordEnumeration enumeration = storage.enumerateRecords();
      try {
        while (enumeration.hasNextElement()) {
          int recordId = enumeration.nextRecordId();
          String data = storage.getRecordAsString(recordId);
          JSONObject json = JSON.getObject(data);
          Track track = new Track().fromJSON(json.getObject("track").toString());
          if (track != null) {
            TrackStats stats = new TrackStats(track);
            stats.playCount = json.getInt("playCount", 0);
            stats.lastPlayed = json.getLong("lastPlayed", 0);
            stats.totalDuration = json.getInt("totalDuration", 0);
            stats.recordId = recordId;
            statsList.addElement(stats);
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
    return statsList;
  }

  public Vector getMostPlayedTracks(int limit) {
    Vector allStats = getAllStats();
    // Sort by play count (descending)
    for (int i = 0; i < allStats.size() - 1; i++) {
      for (int j = i + 1; j < allStats.size(); j++) {
        TrackStats s1 = (TrackStats) allStats.elementAt(i);
        TrackStats s2 = (TrackStats) allStats.elementAt(j);
        if (s2.playCount > s1.playCount) {
          allStats.setElementAt(s2, i);
          allStats.setElementAt(s1, j);
        }
      }
    }
    // Return top N
    Vector result = new Vector();
    for (int i = 0; i < Math.min(limit, allStats.size()); i++) {
      result.addElement(allStats.elementAt(i));
    }
    return result;
  }

  private void saveTrackStats(TrackStats stats) {
    try {
      JSONObject json = new JSONObject();
      json.put("track", stats.track.toJSON());
      json.put("playCount", stats.playCount);
      json.put("lastPlayed", stats.lastPlayed);
      json.put("totalDuration", stats.totalDuration);

      if (stats.recordId != -1) {
        storage.setRecord(stats.recordId, json.toString());
      } else {
        storage.addRecord(json.toString());
      }
    } catch (RecordStoreException e) {
      e.printStackTrace();
    }
  }

  public void clearStats() {
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

  public void close() {
    storage.closeRecordStore();
  }

  public static class TrackStats {
    public Track track;
    public int playCount;
    public long lastPlayed;
    public int totalDuration;
    public int recordId = -1;

    public TrackStats(Track track) {
      this.track = track;
    }
  }
}
