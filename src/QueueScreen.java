import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import model.Track;
import model.Tracks;

public class QueueScreen extends BaseList {
  private final Command clearCommand;
  private final Command removeCommand;
  private final Command playCommand;
  private Tracks queue;
  private int currentIndex;

  public QueueScreen(Navigator navigator) {
    super(Lang.tr("menu.queue"), navigator);
    this.clearCommand = new Command(Lang.tr("queue.clear"), Command.SCREEN, 2);
    this.removeCommand = new Command(Lang.tr("queue.remove"), Command.SCREEN, 3);
    this.playCommand = new Command(Lang.tr("queue.play_from_here"), Command.SCREEN, 4);
    addCommand(clearCommand);
    addCommand(removeCommand);
    addCommand(playCommand);
    populateItems();
  }

  protected void populateItems() {
    deleteAll();
    PlayerScreen playerScreen = MIDPlay.getPlayerScreen();
    if (playerScreen == null || playerScreen.getPlayer() == null) {
      append(Lang.tr("queue.empty"), Configuration.musicIcon);
      return;
    }

    queue = playerScreen.getPlayer().getPlaylist();
    currentIndex = playerScreen.getPlayer().getCurrentIndex();

    if (queue == null || queue.getTracks() == null || queue.getTracks().length == 0) {
      append(Lang.tr("queue.empty"), Configuration.musicIcon);
      return;
    }

    Track[] tracks = queue.getTracks();
    for (int i = 0; i < tracks.length; i++) {
      Track track = tracks[i];
      String prefix = (i == currentIndex) ? "> " : "  ";
      String displayText = prefix + track.getName();
      if (track.getArtist() != null && track.getArtist().length() > 0) {
        displayText = displayText + " - " + track.getArtist();
      }
      append(displayText, Configuration.musicIcon);
    }
  }

  protected void handleSelection() {
    if (queue == null || queue.getTracks() == null || queue.getTracks().length == 0) {
      return;
    }

    int selected = getSelectedIndex();
    if (selected < 0 || selected >= queue.getTracks().length) {
      return;
    }

    // Play selected track
    PlayerScreen playerScreen = MIDPlay.getPlayerScreen();
    if (playerScreen != null && playerScreen.getPlayer() != null) {
      playerScreen.getPlayer().setPlaylist(queue, selected);
      playerScreen.getPlayer().play();
      navigator.back();
    }
  }

  protected void handleCommand(Command c, Displayable d) {
    if (c == clearCommand) {
      boolean confirmed =
          navigator.showConfirmation(
              Lang.tr("queue.confirm.clear"), AlertType.WARNING);
      if (confirmed) {
        PlayerScreen playerScreen = MIDPlay.getPlayerScreen();
        if (playerScreen != null && playerScreen.getPlayer() != null) {
          playerScreen.getPlayer().cleanup();
        }
        navigator.showAlert(Lang.tr("queue.status.cleared"), AlertType.INFO);
        navigator.back();
      }
    } else if (c == removeCommand) {
      int selected = getSelectedIndex();
      if (queue == null || queue.getTracks() == null || selected < 0
          || selected >= queue.getTracks().length) {
        return;
      }

      if (selected == currentIndex) {
        navigator.showAlert(Lang.tr("queue.error.cannot_remove_current"), AlertType.WARNING);
        return;
      }

      Track[] tracks = queue.getTracks();
      Track[] newTracks = new Track[tracks.length - 1];
      int newIndex = 0;
      for (int i = 0; i < tracks.length; i++) {
        if (i != selected) {
          newTracks[newIndex++] = tracks[i];
        }
      }

      Tracks newQueue = new Tracks();
      newQueue.setTracks(newTracks);
      newQueue.setHasMore(false);

      int newCurrentIndex = currentIndex;
      if (selected < currentIndex) {
        newCurrentIndex--;
      }

      PlayerScreen playerScreen = MIDPlay.getPlayerScreen();
      if (playerScreen != null && playerScreen.getPlayer() != null) {
        boolean wasPlaying = playerScreen.getPlayer().isPlaying();
        playerScreen.getPlayer().setPlaylist(newQueue, newCurrentIndex);
        if (wasPlaying) {
          playerScreen.getPlayer().play();
        }
      }

      populateItems();
      navigator.showAlert(Lang.tr("queue.status.removed"), AlertType.INFO);
    } else if (c == playCommand) {
      handleSelection();
    }
  }
}
