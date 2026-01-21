import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import javax.microedition.lcdui.AlertType;
import model.Playlist;
import model.Playlists;

public class AlbumsOperation extends NetworkOperation {
  private final int pageIndex;
  private final AlbumsCallback callback;
  private Playlists albums;

  public AlbumsOperation(Navigator navigator, int pageIndex, AlbumsCallback callback) {
    super(navigator);
    this.pageIndex = pageIndex;
    this.callback = callback;
  }

  protected void execute() throws Exception {
    String url = URLProvider.getAlbums(pageIndex);
    String response = Network.get(url);

    if (response == null || response.length() == 0) {
      throw new Exception("Empty response");
    }

    JSONObject json = JSON.getObject(response);
    JSONArray items = json.getArray("items");

    Playlist[] albumArray = new Playlist[items.size()];
    for (int i = 0; i < items.size(); i++) {
      JSONObject item = items.getObject(i);
      Playlist album = new Playlist();
      album.setKey(item.getString("key", ""));
      album.setName(item.getString("name", ""));
      album.setImageUrl(item.getString("image", ""));
      album.setCustom(false);
      albumArray[i] = album;
    }

    albums = new Playlists();
    albums.setPlaylists(albumArray);
    albums.setHasMore(json.getBoolean("hasMore", false));
  }

  protected void onSuccess() {
    if (callback != null) {
      callback.onAlbumsLoaded(albums);
    }
  }

  protected void onError(String error) {
    navigator.showAlert(error, AlertType.ERROR);
    if (callback != null) {
      callback.onAlbumsError(error);
    }
  }

  public interface AlbumsCallback {
    void onAlbumsLoaded(Playlists albums);
    void onAlbumsError(String error);
  }
}
