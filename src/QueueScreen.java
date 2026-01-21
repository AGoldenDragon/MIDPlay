import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import model.Track;
import model.Tracks;

public final class QueueScreen extends BaseList {
  private final QueueManager queueManager;
  private Track currentTrack;
  private Track[] queueTracks;
  private Track[] upNextTracks;
  private int nowPlayingIndex = -1;
  private int queueStartIndex = -1;
  private int upNextStartIndex = -1;

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

    // Get current player state
    PlayerScreen playerScreen = MIDPlay.getPlayerScreen();
    if (playerScreen != null) {
      PlayerGUI playerGUI = playerScreen.getPlayerGUI();
      currentTrack = playerGUI.getCurrentTrack();

      // Get upcoming tracks from playlist
      Tracks currentPlaylist = playerGUI.getCurrentTracks();
      if (currentPlaylist != null && currentPlaylist.getTracks() != null) {
        int currentIndex = playerGUI.getCurrentIndex();
        Track[] allTracks = currentPlaylist.getTracks();

        // Calculate how many upcoming tracks from playlist
        int remainingTracks = allTracks.length - currentIndex - 1;
        if (remainingTracks > 0) {
          upNextTracks = new Track[remainingTracks];
          for (int i = 0; i < remainingTracks; i++) {
            upNextTracks[i] = allTracks[currentIndex + 1 + i];
          }
        } else {
          upNextTracks = new Track[0];
        }
      } else {
        upNextTracks = new Track[0];
      }
    } else {
      currentTrack = null;
      upNextTracks = new Track[0];
    }

    // Section 1: Now Playing
    if (currentTrack != null) {
      append("--- " + Lang.tr("queue.now_playing") + " ---", null);
      nowPlayingIndex = 0;
      append(currentTrack.getName(), Configuration.musicIcon);
    }

    // Section 2: In Queue
    queueTracks = queueManager.getQueue();
    if (queueTracks.length > 0) {
      append("", null); // Separator
      append("--- " + Lang.tr("queue.in_queue") + " ---", null);
      queueStartIndex = size() - 1;
      for (int i = 0; i < queueTracks.length; i++) {
        append(queueTracks[i].getName(), Configuration.musicIcon);
      }
    }

    // Section 3: Up Next (from playlist)
    if (upNextTracks.length > 0) {
      append("", null); // Separator
      append("--- " + Lang.tr("queue.from_playlist") + " ---", null);
      upNextStartIndex = size() - 1;
      // Show first 10 tracks from playlist to avoid long lists
      int displayCount = Math.min(upNextTracks.length, 10);
      for (int i = 0; i < displayCount; i++) {
        append(upNextTracks[i].getName(), Configuration.musicIcon);
      }
      if (upNextTracks.length > 10) {
        append("... +" + (upNextTracks.length - 10) + " " + Lang.tr("status.load_more"), null);
      }
    }

    // If nothing to show
    if (size() == 0) {
      append(Lang.tr("queue.empty"), null);
    }
  }

  private void refresh() {
    int index = getSelectedIndex();
    populateItems();
    if (size() > 0) {
      if (index < 0) {
        index = 0;
      }
      if (index >= size()) {
        index = size() - 1;
      }
      // Skip header rows when selecting
      String text = getString(index);
      if (text.startsWith("---") || text.equals("")) {
        if (index < size() - 1) {
          index++;
        } else if (index > 0) {
          index--;
        }
      }
      setSelectedIndex(index, true);
    }
  }

  protected void handleSelection() {
    int index = getSelectedIndex();
    if (index < 0 || index >= size()) {
      return;
    }

    // Skip headers and separators
    String text = getString(index);
    if (text.startsWith("---") || text.equals("") || text.startsWith("...")) {
      return;
    }

    Track selectedTrack = getTrackAtIndex(index);
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

  private Track getTrackAtIndex(int index) {
    // Now Playing section
    if (nowPlayingIndex >= 0 && index == nowPlayingIndex + 1) {
      return currentTrack;
    }

    // In Queue section
    if (queueStartIndex >= 0 && index > queueStartIndex) {
      int queueIndex = index - queueStartIndex - 1;
      if (queueIndex >= 0 && queueIndex < queueTracks.length) {
        return queueTracks[queueIndex];
      }
    }

    // Up Next section
    if (upNextStartIndex >= 0 && index > upNextStartIndex) {
      int upNextIndex = index - upNextStartIndex - 1;
      if (upNextIndex >= 0 && upNextIndex < Math.min(upNextTracks.length, 10)) {
        return upNextTracks[upNextIndex];
      }
    }

    return null;
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
    if (queueTracks.length > 0) {
      navigator.showConfirmation(
          Lang.tr("queue.confirm.clear"),
          new Runnable() {
            public void run() {
              queueManager.clearQueue();
              navigator.showAlert(Lang.tr("queue.status.cleared"), AlertType.CONFIRMATION);
              refresh();
            }
          });
    } else {
      navigator.showAlert(Lang.tr("queue.empty"), AlertType.INFO);
    }
  }

  private void removeFromQueue() {
    int selectedIndex = getSelectedIndex();
    if (selectedIndex < 0) {
      return;
    }

    // Check if selected item is in the queue section
    if (queueStartIndex >= 0 && selectedIndex > queueStartIndex) {
      int queueIndex = selectedIndex - queueStartIndex - 1;
      if (queueIndex >= 0 && queueIndex < queueTracks.length) {
        if (queueManager.removeFromQueue(queueIndex)) {
          navigator.showAlert(Lang.tr("queue.status.removed"), AlertType.CONFIRMATION);
          refresh();
        } else {
          navigator.showAlert(Lang.tr("status.error"), AlertType.ERROR);
        }
      } else {
        navigator.showAlert(Lang.tr("status.error"), AlertType.ERROR);
      }
    } else {
      navigator.showAlert(Lang.tr("status.error"), AlertType.ERROR);
    }
  }

  private void showTrackDetails() {
    int selectedIndex = getSelectedIndex();
    if (selectedIndex < 0) {
      return;
    }

    Track selectedTrack = getTrackAtIndex(selectedIndex);
    if (selectedTrack != null) {
      DetailScreen detailScreen = new DetailScreen(selectedTrack, navigator);
      navigator.forward(detailScreen);
    }
  }
}
