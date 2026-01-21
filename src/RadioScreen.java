import java.util.Vector;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import model.Track;
import model.Tracks;

public class RadioScreen extends BaseList {
  private final RadioManager radioManager;
  private final Command addCommand;
  private final Command removeCommand;
  private Vector stations;

  public RadioScreen(Navigator navigator) {
    super(Lang.tr("menu.radio"), navigator);
    this.radioManager = RadioManager.getInstance();
    this.addCommand = new Command(Lang.tr("radio.add_station"), Command.SCREEN, 2);
    this.removeCommand = new Command(Lang.tr("radio.remove_station"), Command.SCREEN, 3);
    addCommand(addCommand);
    addCommand(removeCommand);
    populateItems();
  }

  protected void populateItems() {
    deleteAll();
    stations = radioManager.getRadioStations();

    if (stations.isEmpty()) {
      append(Lang.tr("radio.no_stations"), Configuration.folderIcon);
      return;
    }

    for (int i = 0; i < stations.size(); i++) {
      RadioManager.RadioStation station = (RadioManager.RadioStation) stations.elementAt(i);
      String displayText = station.name;
      if (station.genre != null && station.genre.length() > 0) {
        displayText = displayText + " - " + station.genre;
      }
      append(displayText, Configuration.folderIcon);
    }
  }

  protected void handleSelection() {
    if (stations.isEmpty()) {
      return;
    }

    int selected = getSelectedIndex();
    if (selected < 0 || selected >= stations.size()) {
      return;
    }

    RadioManager.RadioStation station = (RadioManager.RadioStation) stations.elementAt(selected);

    // Create a track from the radio station
    Track radioTrack = new Track();
    radioTrack.setKey(station.url);
    radioTrack.setName(station.name);
    radioTrack.setUrl(station.url);
    radioTrack.setDuration(0); // Radio is continuous
    radioTrack.setArtist(station.genre != null ? station.genre : "Radio");
    radioTrack.setImageUrl("");

    Tracks singleTrack = new Tracks();
    singleTrack.setTracks(new Track[] {radioTrack});
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
    if (c == addCommand) {
      AddRadioStationForm form = new AddRadioStationForm(navigator, radioManager);
      navigator.forward(form);
    } else if (c == removeCommand) {
      if (stations.isEmpty()) {
        navigator.showAlert(Lang.tr("radio.no_stations"), AlertType.WARNING);
        return;
      }

      int selected = getSelectedIndex();
      if (selected < 0 || selected >= stations.size()) {
        navigator.showAlert(Lang.tr("radio.select_station"), AlertType.WARNING);
        return;
      }

      RadioManager.RadioStation station = (RadioManager.RadioStation) stations.elementAt(selected);
      boolean confirmed = navigator.showConfirmation(
          Lang.tr("radio.confirm.remove") + " " + station.name + "?",
          AlertType.WARNING
      );

      if (confirmed) {
        if (radioManager.removeRadioStation(station.name)) {
          populateItems();
          navigator.showAlert(Lang.tr("radio.status.removed"), AlertType.INFO);
        } else {
          navigator.showAlert(Lang.tr("status.error"), AlertType.ERROR);
        }
      }
    }
  }
}
