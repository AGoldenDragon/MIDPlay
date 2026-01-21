import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import model.Track;
import model.Tracks;

public final class QueueScreen extends BaseList {
  private final QueueManager queueManager;
  private Track[] queueTracks;
  private Track[] historyTracks;
  private boolean showingQueue = true;

  public QueueScreen(Navigator navigator) {
    super(Lang.tr("queue.title"), navigator);
    this.queueManager = QueueManager.getInstance();

    // Add commands
    addCommand(Commands.queueClear());
    addCommand(Commands.queueRemove());
    addCommand(Commands.details());

    // Initial load
    refresh();
  }

  protected void populateItems() {
    deleteAll();

    if (showingQueue) {
      // Show upcoming queue
      setTitle(Lang.tr("queue.upcoming"));
      if (queueTracks.length == 0) {
        append(Lang.tr("queue.empty"), null);
      } else {
        for (int i = 0; i < queueTracks.length; i++) {
          append(queueTracks[i].getName(), Configuration.musicIcon);
        }
      }
    } else {
      // Show history
      setTitle(Lang.tr("queue.history"));
      if (historyTracks.length == 0) {
        append(Lang.tr("queue.empty"), null);
      } else {
        for (int i = 0; i < historyTracks.length; i++) {
          append(historyTracks[i].getName(), Configuration.musicIcon);
        }
      }
    }
  }

  private void refresh() {
    queueTracks = queueManager.getQueue();
    historyTracks = queueManager.getHistory();
    int index = getSelectedIndex();
    populateItems();
    if (size() > 0) {
      if (index < 0) {
        index = 0;
      }
      if (index >= size()) {
        index = size() - 1;
      }
      setSelectedIndex(index, true);
    }
  }

  protected void handleSelection() {
    int index = getSelectedIndex();
    if (index < 0 || index >= size()) {
      return;
    }

    Track selectedTrack = null;
    if (showingQueue && queueTracks.length > 0 && index < queueTracks.length) {
      selectedTrack = queueTracks[index];
    } else if (!showingQueue && historyTracks.length > 0 && index < historyTracks.length) {
      selectedTrack = historyTracks[index];
    }

    if (selectedTrack == null) {
      return;
    }

    // Create a single-track playlist and play it
    Track[] tempTracks = new Track[] {selectedTrack};
    Tracks tempPlaylist = new Tracks();
    tempPlaylist.setTracks(tempTracks);
    tempPlaylist.setHasMore(false);

    PlayerScreen playerScreen = MIDPlay.getPlayerScreen();
    if (playerScreen != null) {
      playerScreen.change(Lang.tr("queue.title"), tempPlaylist, 0, navigator);
      Displayable previous = navigator.getPrevious();
      if (previous instanceof PlayerScreen) {
        navigator.back();
      } else {
        navigator.forward(playerScreen);
      }
    } else {
      playerScreen = new PlayerScreen(Lang.tr("queue.title"), tempPlaylist, 0, navigator);
      MIDPlay.setPlayerScreen(playerScreen);
      navigator.forward(playerScreen);
    }
  }

  protected void handleCommand(Command c, Displayable d) {
    if (c == Commands.queueClear()) {
      clearQueue();
    } else if (c == Commands.queueRemove()) {
      removeFromQueue();
    } else if (c == Commands.details()) {
      showTrackDetails();
    }
  }

  private void clearQueue() {
    if (showingQueue && queueTracks.length > 0) {
      navigator.showConfirmation(
          Lang.tr("queue.confirm.clear"),
          new Runnable() {
            public void run() {
              queueManager.clearQueue();
              navigator.showAlert(Lang.tr("queue.status.cleared"), AlertType.CONFIRMATION);
              refresh();
            }
          });
    } else if (!showingQueue && historyTracks.length > 0) {
      navigator.showConfirmation(
          Lang.tr("queue.confirm.clear"),
          new Runnable() {
            public void run() {
              queueManager.clearHistory();
              navigator.showAlert(Lang.tr("queue.status.cleared"), AlertType.CONFIRMATION);
              refresh();
            }
          });
    }
  }

  private void removeFromQueue() {
    if (!showingQueue) {
      navigator.showAlert(Lang.tr("status.error"), AlertType.ERROR);
      return;
    }

    int selectedIndex = getSelectedIndex();
    if (selectedIndex < 0 || selectedIndex >= queueTracks.length) {
      return;
    }

    if (queueManager.removeFromQueue(selectedIndex)) {
      navigator.showAlert(Lang.tr("queue.status.removed"), AlertType.CONFIRMATION);
      refresh();
    } else {
      navigator.showAlert(Lang.tr("status.error"), AlertType.ERROR);
    }
  }

  private void showTrackDetails() {
    int selectedIndex = getSelectedIndex();
    if (selectedIndex < 0) {
      return;
    }

    Track selectedTrack = null;
    if (showingQueue && selectedIndex < queueTracks.length) {
      selectedTrack = queueTracks[selectedIndex];
    } else if (!showingQueue && selectedIndex < historyTracks.length) {
      selectedTrack = historyTracks[selectedIndex];
    }

    if (selectedTrack != null) {
      DetailScreen detailScreen = new DetailScreen(selectedTrack, navigator);
      navigator.forward(detailScreen);
    }
  }

}
