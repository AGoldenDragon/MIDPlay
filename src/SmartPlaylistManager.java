import java.util.Vector;
import model.Track;
import model.Tracks;

public class SmartPlaylistManager {
  private static SmartPlaylistManager instance;
  private static final int DEFAULT_LIMIT = 25;

  public static SmartPlaylistManager getInstance() {
    if (instance == null) {
      instance = new SmartPlaylistManager();
    }
    return instance;
  }

  private SmartPlaylistManager() {}

  public Tracks generateSmartPlaylist(String type) {
    if (Configuration.SMART_PLAYLIST_MOST_PLAYED.equals(type)) {
      return getMostPlayedPlaylist();
    } else if (Configuration.SMART_PLAYLIST_RECENTLY_PLAYED.equals(type)) {
      return getRecentlyPlayedPlaylist();
    } else if (Configuration.SMART_PLAYLIST_RECENTLY_ADDED.equals(type)) {
      return getRecentlyAddedPlaylist();
    } else if (Configuration.SMART_PLAYLIST_TOP_RATED.equals(type)) {
      return getTopRatedPlaylist();
    }
    return new Tracks();
  }

  private Tracks getMostPlayedPlaylist() {
    Vector stats = StatisticsManager.getInstance().getMostPlayedTracks(DEFAULT_LIMIT);
    return statsVectorToTracks(stats);
  }

  private Tracks getRecentlyPlayedPlaylist() {
    Tracks history = HistoryManager.getInstance().getHistory();
    if (history == null || history.getTracks() == null) {
      return new Tracks();
    }
    // History is already sorted by most recent first
    Track[] tracks = history.getTracks();
    int limit = Math.min(DEFAULT_LIMIT, tracks.length);
    Track[] limitedTracks = new Track[limit];
    for (int i = 0; i < limit; i++) {
      limitedTracks[i] = tracks[i];
    }
    Tracks result = new Tracks();
    result.setTracks(limitedTracks);
    result.setHasMore(false);
    return result;
  }

  private Tracks getRecentlyAddedPlaylist() {
    // Get tracks from favorites sorted by when they were added
    FavoritesManager favManager = FavoritesManager.getInstance();
    Vector allTracks = new Vector();

    model.Playlists playlists = favManager.getPlaylists();
    if (playlists != null && playlists.getPlaylists() != null) {
      model.Playlist[] playlistArray = playlists.getPlaylists();
      for (int i = 0; i < playlistArray.length; i++) {
        model.Playlist playlist = playlistArray[i];
        if (playlist.isCustom()) {
          Tracks tracks = favManager.getCustomPlaylistTracks(playlist);
          if (tracks != null && tracks.getTracks() != null) {
            Track[] trackArray = tracks.getTracks();
            for (int j = 0; j < trackArray.length; j++) {
              // Add if not already in list
              boolean found = false;
              for (int k = 0; k < allTracks.size(); k++) {
                Track existing = (Track) allTracks.elementAt(k);
                if (existing.isSame(trackArray[j])) {
                  found = true;
                  break;
                }
              }
              if (!found) {
                allTracks.addElement(trackArray[j]);
              }
            }
          }
        }
      }
    }

    // Reverse to show most recent first
    Vector reversed = new Vector();
    for (int i = allTracks.size() - 1; i >= 0 && reversed.size() < DEFAULT_LIMIT; i--) {
      reversed.addElement(allTracks.elementAt(i));
    }

    return vectorToTracks(reversed);
  }

  private Tracks getTopRatedPlaylist() {
    Vector ratings = RatingManager.getInstance().getTopRatedTracks(4, DEFAULT_LIMIT);
    return ratingsVectorToTracks(ratings);
  }

  private Tracks statsVectorToTracks(Vector stats) {
    Tracks result = new Tracks();
    if (stats.isEmpty()) {
      result.setTracks(new Track[0]);
      result.setHasMore(false);
      return result;
    }
    Track[] tracks = new Track[stats.size()];
    for (int i = 0; i < stats.size(); i++) {
      StatisticsManager.TrackStats stat = (StatisticsManager.TrackStats) stats.elementAt(i);
      tracks[i] = stat.track;
    }
    result.setTracks(tracks);
    result.setHasMore(false);
    return result;
  }

  private Tracks ratingsVectorToTracks(Vector ratings) {
    Tracks result = new Tracks();
    if (ratings.isEmpty()) {
      result.setTracks(new Track[0]);
      result.setHasMore(false);
      return result;
    }
    Track[] tracks = new Track[ratings.size()];
    for (int i = 0; i < ratings.size(); i++) {
      RatingManager.TrackRating rating = (RatingManager.TrackRating) ratings.elementAt(i);
      tracks[i] = rating.track;
    }
    result.setTracks(tracks);
    result.setHasMore(false);
    return result;
  }

  private Tracks vectorToTracks(Vector trackVector) {
    Tracks result = new Tracks();
    if (trackVector.isEmpty()) {
      result.setTracks(new Track[0]);
      result.setHasMore(false);
      return result;
    }
    Track[] tracks = new Track[trackVector.size()];
    for (int i = 0; i < trackVector.size(); i++) {
      tracks[i] = (Track) trackVector.elementAt(i);
    }
    result.setTracks(tracks);
    result.setHasMore(false);
    return result;
  }
}
