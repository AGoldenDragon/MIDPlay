import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

public class SmartPlaylistScreen extends BaseList {
  public SmartPlaylistScreen(Navigator navigator) {
    super(Lang.tr("menu.smart_playlists"), navigator);
    populateItems();
  }

  protected void populateItems() {
    deleteAll();
    String[] types = Configuration.ALL_SMART_PLAYLIST_TYPES;
    for (int i = 0; i < types.length; i++) {
      append(Lang.tr(types[i]), Configuration.playlistIcon);
    }
  }

  protected void handleSelection() {
    int selected = getSelectedIndex();
    if (selected < 0 || selected >= Configuration.ALL_SMART_PLAYLIST_TYPES.length) {
      return;
    }

    String type = Configuration.ALL_SMART_PLAYLIST_TYPES[selected];
    model.Tracks tracks = SmartPlaylistManager.getInstance().generateSmartPlaylist(type);

    if (tracks == null || tracks.getTracks() == null || tracks.getTracks().length == 0) {
      navigator.showAlert(Lang.tr("status.no_data"), javax.microedition.lcdui.AlertType.INFO);
      return;
    }

    TrackListScreen trackListScreen = new TrackListScreen(
        Lang.tr(type),
        tracks,
        navigator,
        false
    );
    navigator.forward(trackListScreen);
  }

  protected void handleCommand(Command c, Displayable d) {
    // No additional commands
  }
}
