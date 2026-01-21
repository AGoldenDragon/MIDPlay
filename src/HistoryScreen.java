import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import model.Track;
import model.Tracks;

public class HistoryScreen extends BaseList {
  private final HistoryManager historyManager;
  private Tracks history;
  private final Command clearCommand;

  public HistoryScreen(Navigator navigator) {
    super(Lang.tr("menu.history"), navigator);
    this.historyManager = HistoryManager.getInstance();
    this.clearCommand = new Command(Lang.tr("history.clear"), Command.SCREEN, 2);
    addCommand(clearCommand);
    populateItems();
  }

  protected void populateItems() {
    deleteAll();
    history = historyManager.getHistory();

    if (history == null || history.getTracks() == null || history.getTracks().length == 0) {
      append(Lang.tr("history.empty"), Configuration.musicIcon);
      return;
    }

    Track[] tracks = history.getTracks();
    for (int i = 0; i < tracks.length; i++) {
      Track track = tracks[i];
      String displayText = track.getName();
      if (track.getArtist() != null && track.getArtist().length() > 0) {
        displayText = displayText + " - " + track.getArtist();
      }
      append(displayText, Configuration.musicIcon);
    }
  }

  protected void handleSelection() {
    if (history == null || history.getTracks() == null || history.getTracks().length == 0) {
      return;
    }

    int selected = getSelectedIndex();
    if (selected < 0 || selected >= history.getTracks().length) {
      return;
    }

    Track selectedTrack = history.getTracks()[selected];
    Tracks singleTrack = new Tracks();
    singleTrack.setTracks(new Track[] {selectedTrack});
    singleTrack.setHasMore(false);

    PlayerScreen playerScreen = MIDPlay.getPlayerScreen();
    if (playerScreen == null) {
      playerScreen = new PlayerScreen(navigator);
      MIDPlay.setPlayerScreen(playerScreen);
    }

    playerScreen.getPlayer().setPlaylist(singleTrack, 0);
    playerScreen.getPlayer().play();
    navigator.forward(playerScreen);
  }

  protected void handleCommand(Command c, Displayable d) {
    if (c == clearCommand) {
      boolean confirmed =
          navigator.showConfirmation(
              Lang.tr("history.confirm.clear"), AlertType.WARNING);
      if (confirmed) {
        historyManager.clearHistory();
        populateItems();
        navigator.showAlert(Lang.tr("history.status.cleared"), AlertType.INFO);
      }
    }
  }
}
