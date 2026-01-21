import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

public class EqualizerScreen extends BaseList {
  private final SettingsManager settingsManager;
  private int currentPreset;

  public EqualizerScreen(Navigator navigator) {
    super(Lang.tr("menu.equalizer"), navigator);
    this.settingsManager = SettingsManager.getInstance();
    this.currentPreset = settingsManager.getCurrentEqualizerPreset();
    populateItems();
  }

  protected void populateItems() {
    deleteAll();
    String[] presets = Configuration.EQUALIZER_PRESETS;
    for (int i = 0; i < presets.length; i++) {
      String prefix = (i == currentPreset) ? "> " : "  ";
      append(prefix + presets[i], Configuration.settingsIcon);
    }
  }

  protected void handleSelection() {
    int selected = getSelectedIndex();
    if (selected < 0 || selected >= Configuration.EQUALIZER_PRESETS.length) {
      return;
    }

    currentPreset = selected;
    try {
      settingsManager.saveEqualizerPreset(currentPreset);
      populateItems();
      navigator.showAlert(
          Lang.tr("equalizer.status.changed") + " " + Configuration.EQUALIZER_PRESETS[selected],
          AlertType.INFO
      );
    } catch (Exception e) {
      navigator.showAlert(Lang.tr("status.error"), AlertType.ERROR);
    }
  }

  protected void handleCommand(Command c, Displayable d) {
    // No additional commands
  }
}
