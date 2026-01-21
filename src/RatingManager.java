import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONException;
import cc.nnproject.json.JSONObject;
import java.util.Vector;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStoreException;
import model.Track;

public class RatingManager {
  private static RatingManager instance;

  public static RatingManager getInstance() {
    if (instance == null) {
      instance = new RatingManager();
    }
    return instance;
  }

  private final RecordStoreManager storage;

  private RatingManager() {
    storage = new RecordStoreManager(Configuration.STORAGE_RATINGS);
  }

  public void setRating(Track track, int rating) {
    if (track == null || rating < 0 || rating > 5) {
      return;
    }
    try {
      int recordId = findRatingRecord(track);
      JSONObject json = new JSONObject();
      json.put("track", track.toJSON());
      json.put("rating", rating);
      json.put("timestamp", System.currentTimeMillis());

      if (recordId != -1) {
        storage.setRecord(recordId, json.toString());
      } else {
        storage.addRecord(json.toString());
      }
    } catch (RecordStoreException e) {
      e.printStackTrace();
    }
  }

  public int getRating(Track track) {
    if (track == null) {
      return 0;
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
            return json.getInt("rating", 0);
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
    return 0;
  }

  public Vector getTopRatedTracks(int minRating, int limit) {
    Vector ratings = new Vector();
    try {
      RecordEnumeration enumeration = storage.enumerateRecords();
      try {
        while (enumeration.hasNextElement()) {
          String data = storage.getRecordAsString(enumeration.nextRecordId());
          JSONObject json = JSON.getObject(data);
          int rating = json.getInt("rating", 0);
          if (rating >= minRating) {
            Track track = new Track().fromJSON(json.getObject("track").toString());
            if (track != null) {
              TrackRating tr = new TrackRating();
              tr.track = track;
              tr.rating = rating;
              tr.timestamp = json.getLong("timestamp", 0);
              ratings.addElement(tr);
            }
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

    // Sort by rating (descending), then by timestamp (most recent first)
    for (int i = 0; i < ratings.size() - 1; i++) {
      for (int j = i + 1; j < ratings.size(); j++) {
        TrackRating r1 = (TrackRating) ratings.elementAt(i);
        TrackRating r2 = (TrackRating) ratings.elementAt(j);
        if (r2.rating > r1.rating || (r2.rating == r1.rating && r2.timestamp > r1.timestamp)) {
          ratings.setElementAt(r2, i);
          ratings.setElementAt(r1, j);
        }
      }
    }

    // Return top N
    Vector result = new Vector();
    for (int i = 0; i < Math.min(limit, ratings.size()); i++) {
      result.addElement(ratings.elementAt(i));
    }
    return result;
  }

  private int findRatingRecord(Track track) {
    try {
      RecordEnumeration enumeration = storage.enumerateRecords();
      try {
        while (enumeration.hasNextElement()) {
          int recordId = enumeration.nextRecordId();
          String data = storage.getRecordAsString(recordId);
          JSONObject json = JSON.getObject(data);
          Track storedTrack = new Track().fromJSON(json.getObject("track").toString());
          if (storedTrack != null && storedTrack.isSame(track)) {
            return recordId;
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
    return -1;
  }

  public void clearRatings() {
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

  public static class TrackRating {
    public Track track;
    public int rating;
    public long timestamp;
  }
}
