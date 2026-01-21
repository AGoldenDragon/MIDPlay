import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import javax.microedition.lcdui.AlertType;
import model.Playlist;
import model.Playlists;

public class ArtistsOperation extends NetworkOperation {
  private final int pageIndex;
  private final ArtistsCallback callback;
  private Playlists artists;

  public ArtistsOperation(Navigator navigator, int pageIndex, ArtistsCallback callback) {
    super(navigator);
    this.pageIndex = pageIndex;
    this.callback = callback;
  }

  protected void execute() throws Exception {
    String url = URLProvider.getArtists(pageIndex);
    String response = Network.get(url);

    if (response == null || response.length() == 0) {
      throw new Exception("Empty response");
    }

    JSONObject json = JSON.getObject(response);
    JSONArray items = json.getArray("items");

    Playlist[] artistArray = new Playlist[items.size()];
    for (int i = 0; i < items.size(); i++) {
      JSONObject item = items.getObject(i);
      Playlist artist = new Playlist();
      artist.setKey(item.getString("key", ""));
      artist.setName(item.getString("name", ""));
      artist.setImageUrl(item.getString("image", ""));
      artist.setCustom(false);
      artistArray[i] = artist;
    }

    artists = new Playlists();
    artists.setPlaylists(artistArray);
    artists.setHasMore(json.getBoolean("hasMore", false));
  }

  protected void onSuccess() {
    if (callback != null) {
      callback.onArtistsLoaded(artists);
    }
  }

  protected void onError(String error) {
    navigator.showAlert(error, AlertType.ERROR);
    if (callback != null) {
      callback.onArtistsError(error);
    }
  }

  public interface ArtistsCallback {
    void onArtistsLoaded(Playlists artists);
    void onArtistsError(String error);
  }
}
