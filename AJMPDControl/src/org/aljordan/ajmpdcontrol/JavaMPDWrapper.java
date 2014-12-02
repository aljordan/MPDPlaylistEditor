package org.aljordan.ajmpdcontrol;

import java.net.UnknownHostException;
import java.util.*;

import org.bff.javampd.*;
import org.bff.javampd.exception.*;
import org.bff.javampd.objects.*;

public class JavaMPDWrapper {
	private MPD connection;
	private MPDDatabase db;
	private Options options;

	public JavaMPDWrapper(Options options) {
		this.setOptions(options);
	}
	
	//used only after connect has been called.  Implemented for Change Listeners
	public MPD getConnection() {
		return connection;
	}
	
	public String openConnection() {
		String results = connect(getOptions());
		if (isConnectedToMPD()) {
			db = connection.getMPDDatabase();
		}
		return results;
	}
	
	public void updateServerDatabase() {
		MPDAdmin admin = connection.getMPDAdmin();
		try {
			admin.updateDatabase();
		} catch (MPDAdminException e) {
			e.printStackTrace();
		} catch (MPDConnectionException e) {
			e.printStackTrace();
		}
	}
	
	public void closeConnection() {
		try {
			connection.close();
			System.out.println("MPD connection closed");
		} catch (MPDConnectionException e) {
			e.printStackTrace();
		} catch (MPDResponseException e) {
			e.printStackTrace();
		}
	}
	
	public boolean isConnectedToMPD() {
		boolean returnValue = false;
		try {
			returnValue = connection.isConnected();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return returnValue;
	}
	
	public String getSampleRate() {
		try {
			MPDPlayer player = connection.getMPDPlayer();
			MPDAudioInfo info = player.getAudioDetails();
			return String.valueOf(info.getSampleRate());
		}
		catch (Exception e) {
				e.printStackTrace();
				return "";
			}
	}
	
	public String getBitDepth() {
		try {
			MPDPlayer player = connection.getMPDPlayer();
			MPDAudioInfo info = player.getAudioDetails();
			return String.valueOf(info.getBits());
		}
		catch (Exception e) {
				e.printStackTrace();
				return "";
			}
	}

	public Object[] getPlaylists() {
		Object[] returnValue = null;
		try {
			Collection<String> playlists = db.listPlaylists();
			returnValue = playlists.toArray();
		} catch (MPDDatabaseException e) {
			System.out.println("Database exception in getPlaylists");
			e.printStackTrace();
		} catch (MPDConnectionException e) {
			System.out.println("Database exception in getPlaylists");
			e.printStackTrace();
		}
		return returnValue;
	}
		
	public Collection<MPDSavedPlaylist> getSavedPlaylists() {
		Collection<MPDSavedPlaylist> playlists = null;
		try {
			playlists = db.listSavedPlaylists();
		} catch (MPDDatabaseException e) {
			System.out.println("Database exception in getSavedPlaylists");
			e.printStackTrace();
		} catch (MPDConnectionException e) {
			System.out.println("Database exception in getSavedPlaylists");
			e.printStackTrace();
		}
		return playlists;
	}
	
	
	public Collection<MPDSong> getSavedPlaylist(String playlistName) {
		Collection<MPDSong> songs = null;
		try {
			songs = db.listPlaylistSongs(playlistName);
		} catch (MPDDatabaseException e) {
			System.out.println("Database exception in getSavedPlaylist");
			e.printStackTrace();
		} catch (MPDConnectionException e) {
			System.out.println("Database exception in getSavedPlaylist");
			e.printStackTrace();
		}
		return songs;
	}
	
	public Collection<MPDSong> getPlaylist(String playlistName) {
		Collection<MPDSong> playlist = null;
		try {
			playlist = db.listPlaylistSongs(playlistName);
		} catch (MPDDatabaseException e) {
			System.out.println("Database exception in getPlaylist");
			e.printStackTrace();
		} catch (MPDConnectionException e) {
			System.out.println("Database exception in getPlaylist");
			e.printStackTrace();
		}
		return playlist;
	}
	
	
	public Collection<MPDAlbum> getAlbumsByArtist(MPDArtist artist) {
		Collection<MPDAlbum> albums = null;
		try {
			albums = db.listAlbumsByArtist(artist);
			Collections.sort((List<MPDAlbum>) albums);
		} catch (MPDResponseException e) {
			System.out.println("Database exception in getAlbumsByArtist");
			e.printStackTrace();
		} catch (MPDConnectionException e) {
			System.out.println("Connection exception in getAlbumsByArtist");
			e.printStackTrace();
		}
		return albums;		
	}
	
	public Collection<MPDAlbum> getAlbumsByGenre(MPDGenre genre) {
		Collection<MPDAlbum> albums = null;
		try {
			albums = db.listAlbumsByGenre(genre);
			Collections.sort((List<MPDAlbum>) albums);
		} catch (MPDResponseException e) {
			System.out.println("Database exception in getAlbumsByGenre");
			e.printStackTrace();
		} catch (MPDConnectionException e) {
			System.out.println("Connection exception in getAlbumsByGenre");
			e.printStackTrace();
		}
		return albums;				
	}

	
	public Collection<MPDSong> getAllSongs() {
		Collection<MPDSong> songs = null;
		try {
			songs = db.listAllSongs();
			Collections.sort((List<MPDSong>) songs);
		} catch (MPDResponseException e) {
			System.out.println("Database exception in getAllSongs");
			e.printStackTrace();
		} catch (MPDConnectionException e) {
			System.out.println("Connection exception in getAllSongs");
			e.printStackTrace();
		}
		return songs;				
	}
	
	public Collection<MPDSong> getSongsByGenre(MPDGenre genre) {
		Collection<MPDSong> songs = null;
		try {
			songs = db.findGenre(genre);
			Collections.sort((List<MPDSong>) songs);
		} catch (MPDResponseException e) {
			System.out.println("Database exception in getAllSongs");
			e.printStackTrace();
		} catch (MPDConnectionException e) {
			System.out.println("Connection exception in getAllSongs");
			e.printStackTrace();
		}
		return songs;						
	}
	
	public MPDPlayer getPlayer() {
		return connection.getMPDPlayer();
	}
	
	public MPDPlaylist getCurrentPlaylist() {
		MPDPlaylist playlist = null;
		try {
			playlist =  connection.getMPDPlaylist();
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			System.out.println(Arrays.toString(e.getStackTrace()));
		}
		return playlist;		
	}
	
	public MPDPlaylist getEmptyPlaylist() {
		MPDPlaylist playlist = getCurrentPlaylist();
		try {
			playlist.clearPlaylist();
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			System.out.println(Arrays.toString(e.getStackTrace()));
		}
		return playlist;
	}
	
	public Collection<MPDSong> getSongsByAlbum(MPDAlbum album) {
		Collection<MPDSong> songs = null;
		try {
			songs = db.findAlbum(album);
		} catch (MPDResponseException e) {
			System.out.println("Database exception in getSongsByAlbum");
			e.printStackTrace();
		} catch (MPDConnectionException e) {
			System.out.println("Connection exception in getSongsByAlbum");
			e.printStackTrace();
		}
		return songs;		
	}

	
	
	// Returns a subset of songs that start with a specific letter.
	public Collection<MPDSong> getSongsStartingWith(String letter) {
		Collection<MPDSong> filteredSongs = new ArrayList<MPDSong>();
		try {
			Collection<MPDSong> allSongs = getAllSongs();
			filteredSongs = filterSongsByStartingLetter(allSongs, letter);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}		
        return filteredSongs;
	}

	public Collection<MPDSong> getSongsByGenreStartingWith(MPDGenre genre, String letter) {
		return filterSongsByStartingLetter(getSongsByGenre(genre),letter);		
	}
	
	
	// Returns a subset of songs that start with a specific letter.
	public Collection<MPDSong> filterSongsByStartingLetter(Collection<MPDSong> coll, String letter) {
		List<MPDSong> filteredSongs = new ArrayList<MPDSong>();
		try {
	        for (MPDSong song : coll) {
	        	String name = song.getName().toLowerCase();
	        	if (name.startsWith(letter.toLowerCase())) {
        			filteredSongs.add(song);
	        	}
	        }
	        Collections.sort(filteredSongs);
        }
		catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}		
        return filteredSongs;
	}

	
	
	
	
	
	public Collection<MPDAlbum> getAllAlbums() {
		Collection<MPDAlbum> albums = null;
		try {
			albums = db.listAllAlbums();
			Collections.sort((List<MPDAlbum>) albums);
		} catch (MPDConnectionException e) {
			System.out.println("Connection exception in getAllAlbums");
			e.printStackTrace();
		} catch (MPDDatabaseException e) {
			System.out.println("Database exception in getAllAlbums");
			e.printStackTrace();
		}
		return albums;
		
	}
	
	
	public Collection<MPDArtist> getAllArtists() {
		Collection<MPDArtist> artists = null;
		try {
			artists = db.listAllArtists();
			Collections.sort((List<MPDArtist>) artists);
		} catch (MPDConnectionException e) {
			System.out.println("Connection exception in getAllArtists");
			e.printStackTrace();
		} catch (MPDDatabaseException e) {
			System.out.println("Database exception in getAllArtists");
			e.printStackTrace();
		}
		return artists;
	}
	
	// Returns a subset of artists that start with a specific letter.
	public Collection<MPDArtist> getArtistsStartingWith(String letter) {
		Collection<MPDArtist> filteredArtists = new ArrayList<MPDArtist>();
		try {
			Collection<MPDArtist> allArtists = getAllArtists();
			filteredArtists = filterArtistsByStartingLetter(allArtists, letter);
        }
		catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}		
        return filteredArtists;
	}
	
	// Returns a subset of artists that start with a specific letter.
	// If the Artists name starts with the word "The ", it will return
	// the artist if the second word starts with the letter. "T" returns
	// all artists starting with "The " as well as "T".
	public Collection<MPDArtist> filterArtistsByStartingLetter(Collection<MPDArtist> coll, String letter) {
		List<MPDArtist> filteredArtists = new ArrayList<MPDArtist>();
		try {
	        for (MPDArtist artist : coll) {
	        	String name = artist.getName().toLowerCase();
	        	if (name.startsWith(letter.toLowerCase()) || name.startsWith("the ")) {
	        		if (!name.startsWith("the ")) {
	        			filteredArtists.add(artist);
	        		}
	        		else {
	        			String subName = name.substring(4).toLowerCase();
	        			if (subName.startsWith(letter.toLowerCase()) || letter.equals("T")) {
	        				filteredArtists.add(artist);
	        			}
	        		}
	        	}
	        }
	        Collections.sort(filteredArtists);
        }
		catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}		
        return filteredArtists;
	}
	
	// Returns a subset of albums that start with a specific letter.
	public Collection<MPDAlbum> getAlbumsStartingWith(String letter) {
		Collection<MPDAlbum> filteredAlbums = new ArrayList<MPDAlbum>();
		try {
			Collection<MPDAlbum> allAlbums = getAllAlbums();
			filteredAlbums = filterAlbumsByStartingLetter(allAlbums, letter);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}		
        return filteredAlbums;
	}

	public Collection<MPDAlbum> getAlbumsByGenreStartingWith(MPDGenre genre, String letter) {
		return filterAlbumsByStartingLetter(getAlbumsByGenre(genre),letter);		
	}
	
	
	// Returns a subset of albums that start with a specific letter.
	public Collection<MPDAlbum> filterAlbumsByStartingLetter(Collection<MPDAlbum> coll, String letter) {
		List<MPDAlbum> filteredAlbums = new ArrayList<MPDAlbum>();
		try {
	        for (MPDAlbum album : coll) {
	        	String name = album.getName().toLowerCase();
	        	if (name.startsWith(letter.toLowerCase())) {
        			filteredAlbums.add(album);
	        	}
	        }
	        Collections.sort(filteredAlbums);
        }
		catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}		
        return filteredAlbums;
	}

		
	public Collection<MPDArtist> getArtistsByGenre(MPDGenre genre) {
		Collection<MPDArtist> artists = null;
		try {
			artists = db.listArtistsByGenre(genre);
			Collections.sort((List<MPDArtist>) artists);
		} catch (MPDConnectionException e) {
			System.out.println("Connection exception in getArtistByGenres");
			e.printStackTrace();
		} catch (MPDDatabaseException e) {
			System.out.println("Database exception in getArtistByGenres");
			e.printStackTrace();
		}
		return artists;
	}
	
	public Collection<MPDArtist> getArtistsByGenreStartingWith(MPDGenre genre, String letter) {
		return filterArtistsByStartingLetter(getArtistsByGenre(genre),letter);		
	}
	
	
	
	public Collection<MPDGenre> getGenres() {
		Collection<MPDGenre> genres = null;
		try {
			genres = db.listAllGenres();
			Collections.sort((List<MPDGenre>) genres);
		} catch (MPDConnectionException e) {
			System.out.println("Connection exception in getGenres");
			e.printStackTrace();
		} catch (MPDDatabaseException e) {
			System.out.println("Database exception in getGenres");
			e.printStackTrace();
		}
		return genres;
	}
	
	
	private String connect(Options options) {
		try {
			if (options.getPassword().trim().length() > 0) {
				connection = new MPD(options.getServer(), options.getPort(), options.getPassword());
			}
			else {
				connection = new MPD(options.getServer(), options.getPort());
			}
			System.out.println("Connection to MPD server success");
			return "Connection succeeded";
		} catch (MPDConnectionException e) {
			System.out.println("Could not connect to MPD server");
			e.printStackTrace();
			return "Connection failed";
		} catch (UnknownHostException e) {
			System.out.println("Could not connect to MPD server");
			e.printStackTrace();
			return "Connection failed: Unknown Host";
		}
	}

	/**
	 * @return the server connections options
	 */
	public Options getOptions() {
		return options;
	}

	/**
	 * @param options the server connection options to set
	 */
	public void setOptions(Options options) {
		this.options = options;
	}

	
}
