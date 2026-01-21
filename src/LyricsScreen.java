import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import model.Track;

public class LyricsScreen extends Form implements CommandListener, LyricsOperation.LyricsCallback {
  private final Navigator navigator;
  private final Track track;
  private final StringItem lyricsItem;

  public LyricsScreen(Navigator navigator, Track track) {
    super(Lang.tr("lyrics.title"));
    this.navigator = navigator;
    this.track = track;

    this.lyricsItem = new StringItem("", Lang.tr("status.loading"));
    append(lyricsItem);

    addCommand(Commands.back());
    setCommandListener(this);

    loadLyrics();
  }

  private void loadLyrics() {
    LyricsOperation operation = new LyricsOperation(navigator, track, this);
    MIDPlay.startOperation(operation);
  }

  public void onLyricsLoaded(String lyrics) {
    lyricsItem.setText(lyrics);
  }

  public void onLyricsError(String error) {
    lyricsItem.setText(Lang.tr("lyrics.error") + ": " + error);
  }

  public void commandAction(Command c, Displayable d) {
    if (c == Commands.back()) {
      MIDPlay.cancelOperation();
      navigator.back();
    }
  }
}
