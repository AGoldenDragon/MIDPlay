import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import model.Playlist;
import model.Playlists;

public class AlbumScreen extends BaseList implements AlbumsOperation.AlbumsCallback {
  private final Command loadMoreCommand;
  private Playlists albums;
  private int currentPage = 0;

  public AlbumScreen(Navigator navigator) {
    super(Lang.tr("menu.albums"), navigator);
    this.loadMoreCommand = new Command(Lang.tr("status.load_more"), Command.SCREEN, 2);
    addCommand(loadMoreCommand);
    loadAlbums();
  }

  private void loadAlbums() {
    AlbumsOperation operation = new AlbumsOperation(navigator, currentPage, this);
    MIDPlay.startOperation(operation);
  }

  protected void populateItems() {
    deleteAll();
    if (albums == null || albums.getPlaylists() == null || albums.getPlaylists().length == 0) {
      append(Lang.tr("status.no_data"), Configuration.folderIcon);
      return;
    }

    Playlist[] albumArray = albums.getPlaylists();
    for (int i = 0; i < albumArray.length; i++) {
      append(albumArray[i].getName(), Configuration.folderIcon);
    }

    if (albums.hasMore()) {
      append(Lang.tr("status.load_more"), null);
    }
  }

  protected void handleSelection() {
    if (albums == null || albums.getPlaylists() == null) {
      return;
    }

    int selected = getSelectedIndex();
    Playlist[] albumArray = albums.getPlaylists();

    if (selected == albumArray.length && albums.hasMore()) {
      currentPage++;
      loadAlbums();
      return;
    }

    if (selected < 0 || selected >= albumArray.length) {
      return;
    }

    Playlist selectedAlbum = albumArray[selected];
    TracksOperation operation = new TracksOperation(navigator, selectedAlbum);
    MIDPlay.startOperation(operation);
  }

  protected void handleCommand(Command c, Displayable d) {
    if (c == loadMoreCommand && albums != null && albums.hasMore()) {
      currentPage++;
      loadAlbums();
    }
  }

  public void onAlbumsLoaded(Playlists newAlbums) {
    if (currentPage == 0) {
      albums = newAlbums;
    } else {
      if (albums != null && albums.getPlaylists() != null && newAlbums != null
          && newAlbums.getPlaylists() != null) {
        Playlist[] oldArray = albums.getPlaylists();
        Playlist[] newArray = newAlbums.getPlaylists();
        Playlist[] combined = new Playlist[oldArray.length + newArray.length];
        for (int i = 0; i < oldArray.length; i++) {
          combined[i] = oldArray[i];
        }
        for (int i = 0; i < newArray.length; i++) {
          combined[oldArray.length + i] = newArray[i];
        }
        albums.setPlaylists(combined);
        albums.setHasMore(newAlbums.hasMore());
      }
    }
    populateItems();
  }

  public void onAlbumsError(String error) {
    populateItems();
  }
}
