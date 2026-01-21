import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.TextField;

public class AddRadioStationForm extends BaseForm {
  private final RadioManager radioManager;
  private final TextField nameField;
  private final TextField urlField;
  private final TextField genreField;

  public AddRadioStationForm(Navigator navigator, RadioManager radioManager) {
    super(Lang.tr("radio.add_station"), navigator);
    this.radioManager = radioManager;

    this.nameField = new TextField(
        Lang.tr("radio.station_name"),
        "",
        100,
        TextField.ANY
    );
    this.urlField = new TextField(
        Lang.tr("radio.station_url"),
        "http://",
        300,
        TextField.URL
    );
    this.genreField = new TextField(
        Lang.tr("radio.station_genre"),
        "",
        50,
        TextField.ANY
    );

    append(nameField);
    append(urlField);
    append(genreField);

    addCommand(Commands.formSave());
    addCommand(Commands.formCancel());
  }

  protected void handleFormCommand(Command c, Displayable d) {
    if (c == Commands.formSave()) {
      String name = nameField.getString();
      String url = urlField.getString();
      String genre = genreField.getString();

      if (name == null || name.trim().length() == 0) {
        navigator.showAlert(Lang.tr("radio.error.empty_name"), AlertType.WARNING);
        return;
      }

      if (url == null || url.trim().length() == 0 || !url.startsWith("http")) {
        navigator.showAlert(Lang.tr("radio.error.invalid_url"), AlertType.WARNING);
        return;
      }

      RadioManager.RadioStation station = new RadioManager.RadioStation(
          name.trim(),
          url.trim(),
          genre.trim()
      );

      radioManager.addRadioStation(station);
      navigator.showAlert(Lang.tr("radio.status.added"), AlertType.INFO);
      navigator.back();
    } else if (c == Commands.formCancel()) {
      navigator.back();
    }
  }
}
