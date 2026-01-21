import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONException;
import cc.nnproject.json.JSONObject;
import java.util.Vector;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStoreException;
import model.Track;
import model.Tracks;

/**
 * Manages the listening queue system similar to Spotify's queue.
 * Handles upcoming tracks, play history, and queue persistence.
 */
public class QueueManager {
  private static QueueManager instance;

  private static final String STORAGE_QUEUE = "storage.queue";
  private static final int MAX_HISTORY_SIZE = 50;

  private final RecordStoreManager storage;
  private Vector queue; // Upcoming tracks
  private Vector history; // Recently played tracks
  private boolean queueActive; // Whether queue is being used

  public static QueueManager getInstance() {
    if (instance == null) {
      instance = new QueueManager();
    }
    return instance;
  }

  private QueueManager() {
    storage = new RecordStoreManager(STORAGE_QUEUE);
    queue = new Vector();
    history = new Vector();
    queueActive = false;
    loadQueueState();
  }

  /**
   * Add a track to play next (top of queue)
   */
  public void playNext(Track track) {
    if (track == null) return;
    queue.insertElementAt(track, 0);
    queueActive = true;
    saveQueueState();
  }

  /**
   * Add a track to the end of the queue
   */
  public void addToQueue(Track track) {
    if (track == null) return;
    queue.addElement(track);
    queueActive = true;
    saveQueueState();
  }

  /**
   * Add multiple tracks to the end of the queue
   */
  public void addToQueue(Track[] tracks) {
    if (tracks == null) return;
    for (int i = 0; i < tracks.length; i++) {
      if (tracks[i] != null) {
        queue.addElement(tracks[i]);
      }
    }
    if (tracks.length > 0) {
      queueActive = true;
      saveQueueState();
    }
  }

  /**
   * Get the next track from the queue
   */
  public Track getNextTrack() {
    if (queue.isEmpty()) {
      queueActive = false;
      return null;
    }
    Track track = (Track) queue.elementAt(0);
    queue.removeElementAt(0);
    saveQueueState();
    return track;
  }

  /**
   * Peek at the next track without removing it
   */
  public Track peekNextTrack() {
    if (queue.isEmpty()) {
      return null;
    }
    return (Track) queue.elementAt(0);
  }

  /**
   * Check if there are tracks in the queue
   */
  public boolean hasNext() {
    return !queue.isEmpty();
  }

  /**
   * Check if queue is currently active
   */
  public boolean isQueueActive() {
    return queueActive && !queue.isEmpty();
  }

  /**
   * Get the current queue as an array
   */
  public Track[] getQueue() {
    Track[] tracks = new Track[queue.size()];
    for (int i = 0; i < queue.size(); i++) {
      tracks[i] = (Track) queue.elementAt(i);
    }
    return tracks;
  }

  /**
   * Get the current queue as Tracks object
   */
  public Tracks getQueueAsTracks() {
    Tracks tracks = new Tracks();
    tracks.setTracks(getQueue());
    tracks.setHasMore(false);
    return tracks;
  }

  /**
   * Get queue size
   */
  public int getQueueSize() {
    return queue.size();
  }

  /**
   * Remove a track from the queue by index
   */
  public boolean removeFromQueue(int index) {
    if (index < 0 || index >= queue.size()) {
      return false;
    }
    queue.removeElementAt(index);
    if (queue.isEmpty()) {
      queueActive = false;
    }
    saveQueueState();
    return true;
  }

  /**
   * Move a track in the queue
   */
  public boolean moveTrack(int fromIndex, int toIndex) {
    if (fromIndex < 0 || fromIndex >= queue.size() ||
        toIndex < 0 || toIndex >= queue.size()) {
      return false;
    }
    Track track = (Track) queue.elementAt(fromIndex);
    queue.removeElementAt(fromIndex);
    queue.insertElementAt(track, toIndex);
    saveQueueState();
    return true;
  }

  /**
   * Clear the entire queue
   */
  public void clearQueue() {
    queue.removeAllElements();
    queueActive = false;
    saveQueueState();
  }

  /**
   * Add a track to history (called when a track finishes playing)
   */
  public void addToHistory(Track track) {
    if (track == null) return;

    // Remove duplicates from history
    for (int i = history.size() - 1; i >= 0; i--) {
      Track t = (Track) history.elementAt(i);
      if (t.isSame(track)) {
        history.removeElementAt(i);
      }
    }

    // Add to beginning of history
    history.insertElementAt(track, 0);

    // Limit history size
    while (history.size() > MAX_HISTORY_SIZE) {
      history.removeElementAt(history.size() - 1);
    }

    saveQueueState();
  }

  /**
   * Get the play history as an array
   */
  public Track[] getHistory() {
    Track[] tracks = new Track[history.size()];
    for (int i = 0; i < history.size(); i++) {
      tracks[i] = (Track) history.elementAt(i);
    }
    return tracks;
  }

  /**
   * Get the play history as Tracks object
   */
  public Tracks getHistoryAsTracks() {
    Tracks tracks = new Tracks();
    tracks.setTracks(getHistory());
    tracks.setHasMore(false);
    return tracks;
  }

  /**
   * Get history size
   */
  public int getHistorySize() {
    return history.size();
  }

  /**
   * Clear the play history
   */
  public void clearHistory() {
    history.removeAllElements();
    saveQueueState();
  }

  /**
   * Save queue state to persistent storage
   */
  private void saveQueueState() {
    try {
      JSONObject data = new JSONObject();

      // Save queue
      JSONArray queueArray = new JSONArray();
      for (int i = 0; i < queue.size(); i++) {
        Track track = (Track) queue.elementAt(i);
        if (track != null) {
          queueArray.add(track.toJSON());
        }
      }
      data.put("queue", queueArray);

      // Save history
      JSONArray historyArray = new JSONArray();
      for (int i = 0; i < history.size(); i++) {
        Track track = (Track) history.elementAt(i);
        if (track != null) {
          historyArray.add(track.toJSON());
        }
      }
      data.put("history", historyArray);

      data.put("queueActive", queueActive);

      // Clear existing records and save new state
      clearStorage();
      storage.addRecord(data.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Load queue state from persistent storage
   */
  private void loadQueueState() {
    try {
      RecordEnumeration enumeration = storage.enumerateRecords();
      try {
        if (enumeration.hasNextElement()) {
          String data = storage.getRecordAsString(enumeration.nextRecordId());
          JSONObject json = JSON.getObject(data);

          // Load queue
          if (json.has("queue")) {
            JSONArray queueArray = json.getArray("queue");
            queue.removeAllElements();
            for (int i = 0; i < queueArray.size(); i++) {
              Track track = new Track().fromJSON(queueArray.getObject(i).toString());
              if (track != null) {
                queue.addElement(track);
              }
            }
          }

          // Load history
          if (json.has("history")) {
            JSONArray historyArray = json.getArray("history");
            history.removeAllElements();
            for (int i = 0; i < historyArray.size(); i++) {
              Track track = new Track().fromJSON(historyArray.getObject(i).toString());
              if (track != null) {
                history.addElement(track);
              }
            }
          }

          // Load queue active state
          queueActive = json.getBoolean("queueActive", false);

          // If queue is empty, set queueActive to false
          if (queue.isEmpty()) {
            queueActive = false;
          }
        }
      } finally {
        enumeration.destroy();
      }
    } catch (Exception e) {
      e.printStackTrace();
      // Initialize empty state on error
      queue = new Vector();
      history = new Vector();
      queueActive = false;
    }
  }

  /**
   * Clear all records from storage
   */
  private void clearStorage() {
    try {
      RecordEnumeration enumeration = storage.enumerateRecords();
      try {
        Vector recordIds = new Vector();
        while (enumeration.hasNextElement()) {
          recordIds.addElement(new Integer(enumeration.nextRecordId()));
        }
        for (int i = 0; i < recordIds.size(); i++) {
          storage.deleteRecord(((Integer) recordIds.elementAt(i)).intValue());
        }
      } finally {
        enumeration.destroy();
      }
    } catch (RecordStoreException e) {
      e.printStackTrace();
    }
  }

  /**
   * Close the storage
   */
  public void close() {
    storage.closeRecordStore();
  }

  /**
   * Reset queue manager state (for testing)
   */
  public void reset() {
    queue.removeAllElements();
    history.removeAllElements();
    queueActive = false;
    clearStorage();
  }
}
