import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import model.Playlist;
import model.Playlists;

public class ArtistScreen extends BaseList implements ArtistsOperation.ArtistsCallback {
  private final Command loadMoreCommand;
  private Playlists artists;
  private int currentPage = 0;

  public ArtistScreen(Navigator navigator) {
    super(Lang.tr("menu.artists"), navigator);
    this.loadMoreCommand = new Command(Lang.tr("status.load_more"), Command.SCREEN, 2);
    addCommand(loadMoreCommand);
    loadArtists();
  }

  private void loadArtists() {
    ArtistsOperation operation = new ArtistsOperation(navigator, currentPage, this);
    MIDPlay.startOperation(operation);
  }

  protected void populateItems() {
    deleteAll();
    if (artists == null || artists.getPlaylists() == null || artists.getPlaylists().length == 0) {
      append(Lang.tr("status.no_data"), Configuration.folderIcon);
      return;
    }

    Playlist[] artistArray = artists.getPlaylists();
    for (int i = 0; i < artistArray.length; i++) {
      append(artistArray[i].getName(), Configuration.folderIcon);
    }

    if (artists.hasMore()) {
      append(Lang.tr("status.load_more"), null);
    }
  }

  protected void handleSelection() {
    if (artists == null || artists.getPlaylists() == null) {
      return;
    }

    int selected = getSelectedIndex();
    Playlist[] artistArray = artists.getPlaylists();

    if (selected == artistArray.length && artists.hasMore()) {
      currentPage++;
      loadArtists();
      return;
    }

    if (selected < 0 || selected >= artistArray.length) {
      return;
    }

    Playlist selectedArtist = artistArray[selected];
    TracksOperation operation = new TracksOperation(navigator, selectedArtist);
    MIDPlay.startOperation(operation);
  }

  protected void handleCommand(Command c, Displayable d) {
    if (c == loadMoreCommand && artists != null && artists.hasMore()) {
      currentPage++;
      loadArtists();
    }
  }

  public void onArtistsLoaded(Playlists newArtists) {
    if (currentPage == 0) {
      artists = newArtists;
    } else {
      if (artists != null && artists.getPlaylists() != null && newArtists != null
          && newArtists.getPlaylists() != null) {
        Playlist[] oldArray = artists.getPlaylists();
        Playlist[] newArray = newArtists.getPlaylists();
        Playlist[] combined = new Playlist[oldArray.length + newArray.length];
        for (int i = 0; i < oldArray.length; i++) {
          combined[i] = oldArray[i];
        }
        for (int i = 0; i < newArray.length; i++) {
          combined[oldArray.length + i] = newArray[i];
        }
        artists.setPlaylists(combined);
        artists.setHasMore(newArtists.hasMore());
      }
    }
    populateItems();
  }

  public void onArtistsError(String error) {
    populateItems();
  }
}
