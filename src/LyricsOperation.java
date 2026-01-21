import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONObject;
import javax.microedition.lcdui.AlertType;
import model.Track;

public class LyricsOperation extends NetworkOperation {
  private final Track track;
  private final LyricsCallback callback;
  private String lyrics;

  public LyricsOperation(Navigator navigator, Track track, LyricsCallback callback) {
    super(navigator);
    this.track = track;
    this.callback = callback;
  }

  protected void execute() throws Exception {
    if (track == null) {
      throw new Exception("No track provided");
    }

    String artist = track.getArtist();
    String title = track.getName();

    if (artist == null || artist.length() == 0 || title == null || title.length() == 0) {
      throw new Exception("Invalid track information");
    }

    // Use a free lyrics API
    String apiUrl = URLProvider.getLyricsUrl(artist, title);
    String response = Network.get(apiUrl);

    if (response == null || response.length() == 0) {
      throw new Exception("No response from lyrics service");
    }

    JSONObject json = JSON.getObject(response);
    lyrics = json.getString("lyrics", "");

    if (lyrics == null || lyrics.length() == 0) {
      lyrics = Lang.tr("lyrics.not_found");
    }
  }

  protected void onSuccess() {
    if (callback != null) {
      callback.onLyricsLoaded(lyrics);
    }
  }

  protected void onError(String error) {
    navigator.showAlert(
        Lang.tr("lyrics.error") + ": " + error,
        AlertType.ERROR
    );
    if (callback != null) {
      callback.onLyricsError(error);
    }
  }

  public interface LyricsCallback {
    void onLyricsLoaded(String lyrics);
    void onLyricsError(String error);
  }
}
