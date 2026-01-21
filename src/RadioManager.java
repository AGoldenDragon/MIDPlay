import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONException;
import cc.nnproject.json.JSONObject;
import java.util.Vector;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStoreException;

public class RadioManager {
  private static RadioManager instance;

  public static RadioManager getInstance() {
    if (instance == null) {
      instance = new RadioManager();
    }
    return instance;
  }

  private final RecordStoreManager storage;

  private RadioManager() {
    storage = new RecordStoreManager(Configuration.STORAGE_RADIO);
    initializeDefaultStations();
  }

  private void initializeDefaultStations() {
    Vector stations = getRadioStations();
    if (stations.isEmpty()) {
      // Add some default radio stations
      addRadioStation(new RadioStation(
          "ClassicFM", "http://media-ice.musicradio.com/ClassicFMMP3", "Classical Music"));
      addRadioStation(new RadioStation(
          "Jazz24", "http://live.str3am.com:2430/jazz24", "Jazz Music"));
      addRadioStation(new RadioStation(
          "SomaFM Groove Salad", "http://ice1.somafm.com/groovesalad-128-mp3", "Electronic/Ambient"));
      addRadioStation(new RadioStation(
          "181.FM The Buzz", "http://listen.181fm.com/181-buzz_128k.mp3", "Alternative Rock"));
      addRadioStation(new RadioStation(
          "DI.FM Vocal Trance", "http://prem2.di.fm:80/vocaltrance", "Trance Music"));
    }
  }

  public void addRadioStation(RadioStation station) {
    if (station == null || station.name == null || station.url == null) {
      return;
    }
    try {
      // Check if already exists
      if (findStationRecord(station.name) != -1) {
        return;
      }

      JSONObject json = new JSONObject();
      json.put("name", station.name);
      json.put("url", station.url);
      json.put("genre", station.genre != null ? station.genre : "");
      json.put("timestamp", System.currentTimeMillis());

      storage.addRecord(json.toString());
    } catch (RecordStoreException e) {
      e.printStackTrace();
    }
  }

  public boolean removeRadioStation(String name) {
    if (name == null) {
      return false;
    }
    int recordId = findStationRecord(name);
    if (recordId == -1) {
      return false;
    }
    try {
      storage.deleteRecord(recordId);
      return true;
    } catch (RecordStoreException e) {
      e.printStackTrace();
      return false;
    }
  }

  public Vector getRadioStations() {
    Vector stations = new Vector();
    try {
      RecordEnumeration enumeration = storage.enumerateRecords();
      try {
        while (enumeration.hasNextElement()) {
          String data = storage.getRecordAsString(enumeration.nextRecordId());
          JSONObject json = JSON.getObject(data);
          RadioStation station = new RadioStation(
              json.getString("name", ""),
              json.getString("url", ""),
              json.getString("genre", "")
          );
          station.timestamp = json.getLong("timestamp", 0);
          stations.addElement(station);
        }
      } finally {
        enumeration.destroy();
      }
    } catch (RecordStoreException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    }

    // Sort by name
    for (int i = 0; i < stations.size() - 1; i++) {
      for (int j = i + 1; j < stations.size(); j++) {
        RadioStation s1 = (RadioStation) stations.elementAt(i);
        RadioStation s2 = (RadioStation) stations.elementAt(j);
        if (s1.name.compareTo(s2.name) > 0) {
          stations.setElementAt(s2, i);
          stations.setElementAt(s1, j);
        }
      }
    }

    return stations;
  }

  private int findStationRecord(String name) {
    try {
      RecordEnumeration enumeration = storage.enumerateRecords();
      try {
        while (enumeration.hasNextElement()) {
          int recordId = enumeration.nextRecordId();
          String data = storage.getRecordAsString(recordId);
          JSONObject json = JSON.getObject(data);
          if (name.equals(json.getString("name", ""))) {
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

  public void close() {
    storage.closeRecordStore();
  }

  public static class RadioStation {
    public String name;
    public String url;
    public String genre;
    public long timestamp;

    public RadioStation(String name, String url, String genre) {
      this.name = name;
      this.url = url;
      this.genre = genre;
    }
  }
}
