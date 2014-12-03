package org.aljordan.ajmpdcontrol;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JTabbedPane;

import java.awt.GridBagConstraints;

import javax.swing.JLabel;

import java.awt.Insets;

import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.JButton;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JList;
import javax.swing.border.LineBorder;

import java.awt.Color;
import java.util.Collection;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.JToolTip;
import javax.swing.ListSelectionModel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;

import org.bff.javampd.MPDPlayer;
import org.bff.javampd.MPDPlaylist;
import org.bff.javampd.MPDPlayer.PlayerStatus;
import org.bff.javampd.exception.MPDConnectionException;
import org.bff.javampd.exception.MPDPlayerException;
import org.bff.javampd.exception.MPDPlaylistException;
import org.bff.javampd.exception.MPDResponseException;
import org.bff.javampd.objects.MPDAlbum;
import org.bff.javampd.objects.MPDArtist;
import org.bff.javampd.objects.MPDGenre;
import org.bff.javampd.objects.MPDSong;

import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.ImageIcon;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Font;

import javax.swing.JProgressBar;
import javax.swing.JTable;

import java.awt.GridLayout;
import javax.swing.JSlider;

public class Main extends JFrame {

	private static final long serialVersionUID = 1L;
	private String currentLetter; //Used to track which letter button has been pressed
	private MPDGenre currentGenre;  //Used to see if and which genre is selected
	private MPDPlayer player;  // MPD player to control playback.
	private MPDPlaylist initialMPDPlaylist; // store the currently playing MPD playlist;
	private enum ListView {ARTIST, ALBUM, SONG} // Used to track which view group radio button has been pressed
	private ListView currentListView; // See ListView enum notes
	private Options options; // Contains MPD Server information
	private JavaMPDWrapper library; // The tool that allows to get out music from MPD.
	private JPanel contentPane;
	private JTextField txtServer;
	private JTextField txtPort;
	private JTextField txtPassword;
	private JLabel lblStatus;
	private JLabel lblTitleInfo;
	private JLabel lblArtistInfo;
	private JLabel lblAlbumInfo;
	private JLabel lblYearInfo;
	private JLabel lblGenreInfo;
	private JLabel lblRateInfo;
	private JLabel lblSampleRateInfo;
	private JLabel lblBitDepthInfo;
	private JLabel lblElapsedTime;
	private JLabel lblTotalTime;
	private final ButtonGroup btnGrpLetterSelection = new ButtonGroup();
	private final ButtonGroup btnGroupArtistsAlbumsSongs = new ButtonGroup();
	private JList<MPDGenre> lstGenres;
	private JList<MPDArtist> lstArtists;
	private JList<MPDAlbum> lstAlbums;
	private JList<MPDSong> lstSongs;
	private JList<MPDSong> lstPlaylist;
	private JList<MPDSong> lstNowPlaying;
	private JScrollPane scrollPaneGenres;
	private JScrollPane scrollPaneArtists;
	private JScrollPane scrollPaneAlbums;
	private JScrollPane scrollPaneSongs;
	private JScrollPane scrollPanePlaylist;
	private JScrollPane scrollPaneNowPlaying;
	private JScrollPane scrollPaneOutputs;
	private JRadioButton rdbtnArtists;
	private JRadioButton rdbtnAll;
	private JButton btnClear;
	private JButton btnSave;
	private JButton btnLoad;
	private JButton btnAdd;
	private JButton btnRemove;
	private JButton btnShuffle;
	private JButton btnRepeat;
	private JButton btnPlay;
	private JButton btnPause;
	private JButton btnUpdateServerDatabase;
	private JButton btnLoop;
	private JProgressBar progbarSongTime;
	private JSlider sliderVolume;
	private JTabbedPane tabbedPaneMain;
	private JTable tblOutputs;
	private MPDChangeListener changeListener;
	private String currentPlaylistName; //if a user loads or saves a play list, used to prepopulate save dialog
	// following loop variables are for "repeat a-b" functionality
	private long loopStart;
	private long loopEnd;
	private boolean isLooping;
	private boolean isLoopStarting;
	

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Main frame = new Main();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	
	/**
	 * Create the frame.
	 */
	public Main() {
		options = new Options();
		currentLetter = ""; //Initialize current selected letter to empty string;
		currentGenre = null;
		initComponents();
		initComponentsFromOptions();
		attemptConnection(true);
		//pressInitialButtons();
		currentPlaylistName = "";
		resetLooping();
		progbarSongTime.addMouseListener(new SongProgressMouseListener(this));
	}
	
	public void moveToSongTime(long secondsToMoveTo) {
		try {
			player.seek(secondsToMoveTo);
		} catch (MPDPlayerException e) {
			e.printStackTrace();
		} catch (MPDConnectionException e) {
			e.printStackTrace();
		}
	}
	
	public void checkLoop() {
		try {
			if (isLooping && (player.getElapsedTime() > loopEnd)) {
				player.seek(loopStart);
			}
		} catch (MPDPlayerException e) {
			e.printStackTrace();
		} catch (MPDConnectionException e) {
			e.printStackTrace();
		}		
	}
	
	public void resetLooping() {
		loopStart = 0;
		loopEnd = 0;
		isLooping = false;	
		isLoopStarting = false;
		btnLoop.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/btn_loop.png")));
	}
	
	private void setUpLoop() {
		try {
			if (!isLooping && !isLoopStarting) {
				loopStart = player.getElapsedTime();
				btnLoop.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/btn_loop_starting.png")));
				isLoopStarting = true;
			} 
			else if (isLoopStarting) { //this button press means we end looping here
				loopEnd = player.getElapsedTime();
				btnLoop.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/btn_loop_looping.png")));
				player.seek(loopStart);
				isLooping = true;
				isLoopStarting = false;
			}
			else { //stop looping
				resetLooping();
			}
		} catch (MPDPlayerException e) {
			e.printStackTrace();
		} catch (MPDConnectionException e) {
			e.printStackTrace();
		}
	}
	
	private void initComponentsFromLibrary() {
		if (library != null && library.isConnectedToMPD()) {
			Collection<MPDGenre> genres = library.getGenres();
//			lstGenres = new JList(genres.toArray());
			lstGenres = new JList<MPDGenre>(genres.toArray(new MPDGenre[genres.size()]));
			scrollPaneGenres.setViewportView(lstGenres);
			lstGenres.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			
			lstGenres.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent arg0) {
					lstGenresValueChanged(arg0);
				}
			});
			
			
			if (player == null) {
				player = library.getPlayer();				
			}			

			// set shuffle and repeat buttons
			try {
				if (player.isRandom()) {
					btnShuffle.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/img_btn_shuffle_pressed_small.png")));
				}
				if (player.isRepeat()) {
					btnRepeat.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/img_btn_repeat_pressed_small.png")));					
				}
			} catch (MPDPlayerException e) {
				e.printStackTrace();
			} catch (MPDConnectionException e) {
				e.printStackTrace();
			} 
			
			//fill in the MPD outputs table
			initOutputsTable();
			
			try {
				sliderVolume.setValue(player.getVolume());
			} catch (Exception e) {
				//e.printStackTrace();
				sliderVolume.setValue(0);
			}

		}
	}
	
	private void initOutputsTable(){
		tblOutputs = new JTable(new OutputsTableModel(library));
		tblOutputs.getModel().addTableModelListener(new OutputsTableListener(library));
		scrollPaneOutputs.setViewportView(tblOutputs);
	}
	
	
	private void pressInitialButtons() {
		rdbtnArtists.doClick();
		rdbtnAll.doClick();
	}
	
	private String convertTimeInSecondsToString(int time) {
		String minutes;
		String seconds;
		if (time / 60 < 10)
			minutes = "0" + Integer.toString(time / 60);
		else
			minutes = Integer.toString(time / 60);
		if (time % 60 < 10)
			seconds = "0" + Integer.toString(time % 60);
		else
			seconds = Integer.toString(time % 60);

		return minutes + ":" + seconds;
		
	}
	
	public void updateSongProgress(int elapsedSeconds) {
		progbarSongTime.setValue(elapsedSeconds);
		lblElapsedTime.setText(convertTimeInSecondsToString(elapsedSeconds));

	}

	
	public void setVolumeSlider(int volume) {
		sliderVolume.setValue(volume);
	}
	
	
	private void setUpPlaylistEditorListBox() {
		try {
			// model for drag and drop list box
		    DefaultListModel<MPDSong> model = new DefaultListModel<MPDSong>();
			for (MPDSong s : initialMPDPlaylist.getSongList().toArray(new MPDSong[initialMPDPlaylist.getSongList().size()]))
				model.addElement(s);

			// load play list into play list editor
			lstPlaylist = new JList<MPDSong>(model);
		    MouseAdapter listener = new ReorderListener(lstPlaylist, initialMPDPlaylist);
		    lstPlaylist.addMouseListener(listener);
		    lstPlaylist.addMouseMotionListener(listener);

		    lstPlaylist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		    lstPlaylist.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
		        public void valueChanged(javax.swing.event.ListSelectionEvent e) {
		            lstPlaylistValueChanged(e);
		        }
		    });
		    lstPlaylist.setToolTipText("Drag songs up and down to reorder playlist");
		    scrollPanePlaylist.setViewportView(lstPlaylist);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();			
		}		
	}
	
	private void loadCurrentPlaylist() {
		if (library == null || !library.isConnectedToMPD()) {
			return;
		}
		initialMPDPlaylist = library.getCurrentPlaylist();
		setUpPlaylistEditorListBox();
	}
	
	private void checkConnection() {
		if (!library.isConnectedToMPD()) {
			attemptConnection(false);
			if (player == null) {
				player = library.getPlayer();				
			}			
			loadCurrentPlaylist();
		}
	}
	
	public void setPlayPauseButtons() {
		try {
			PlayerStatus ps = player.getStatus();
			if (ps == PlayerStatus.STATUS_PLAYING) {
				btnPlay.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/btn_play_pressed.png")));
				btnPause.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/btn_pause.png")));
				btnPlay.requestFocus();
			}
			else if (ps == PlayerStatus.STATUS_PAUSED) {
				btnPlay.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/btn_play_1.png")));
				btnPause.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/btn_pause_pressed.png")));
			}
			else if (ps == PlayerStatus.STATUS_STOPPED) {
				btnPlay.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/btn_play_1.png")));
				btnPause.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/btn_pause.png")));
			}
		} catch (MPDResponseException e) {
			e.printStackTrace();
		} catch (MPDConnectionException e) {
			e.printStackTrace();
		}
	}
	
	// Needed because playerStatus is always STATUS_STOPPED when player is paused
	public void setPauseButtons() {
		try {
			PlayerStatus ps = player.getStatus();
			if (ps == PlayerStatus.STATUS_PLAYING) {
				btnPlay.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/btn_play_pressed.png")));
				btnPause.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/btn_pause.png")));
				btnPlay.requestFocus();
			}
			else {
				btnPlay.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/btn_play_1.png")));
				btnPause.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/btn_pause_pressed.png")));
				btnPause.requestFocus();
			}
		} catch (MPDResponseException e) {
			e.printStackTrace();
		} catch (MPDConnectionException e) {
			e.printStackTrace();
		}
		
	}
	
	
	private String padRight(String s, int n) {
	    return String.format("%1$-" + n + "s", s);
	}
	
	public void hiliteCurrentSong() {
		MPDSong currentSong;
		try {
			currentSong = initialMPDPlaylist.getCurrentSong();
			lstNowPlaying.setSelectedValue(currentSong, true);
			// give title a minimum width in spaces so gridbag doesn't bounce around so much
			if (currentSong.getTitle().length() < 85)
				lblTitleInfo.setText(padRight(currentSong.getTitle(),85));
			else
				lblTitleInfo.setText(currentSong.getTitle());
			lblArtistInfo.setText(currentSong.getArtist().getName());
			lblAlbumInfo.setText(currentSong.getAlbum().getName());
			lblGenreInfo.setText(currentSong.getGenre());
			lblSampleRateInfo.setText(library.getSampleRate() + " Hz");
			lblBitDepthInfo.setText(library.getBitDepth() + " bits");
			if (currentSong.getYear().equals("No Year")) {
				lblYearInfo.setText("");
			}
			else {
				lblYearInfo.setText(currentSong.getYear());
			}
			lblTotalTime.setText(convertTimeInSecondsToString(currentSong.getLength()));
			progbarSongTime.setMinimum(0);
			progbarSongTime.setMaximum(currentSong.getLength());
		} catch (MPDPlaylistException e) {
			e.printStackTrace();
		} catch (MPDConnectionException e) {
			e.printStackTrace();
		}
	}
	
	public void updateBitRateInfo() {
		try {
			lblRateInfo.setText(String.valueOf(player.getBitrate()) + " kbps");
		} catch (Exception e) {
			lblRateInfo.setText("");
		}
	}
	
	private void lstNowPlayingClicked(MouseEvent e) {
	    JList npList = (JList) e.getSource();
	    if (e.getClickCount() == 2) { // Double-click
	    	MPDSong songToBePlayed = (MPDSong)npList.getSelectedValue();
	    	playThisSong(songToBePlayed);
	    }
	}
		
		
	public void updatePlayingNowList() {
		try {
			lstNowPlaying = new JList<MPDSong>(initialMPDPlaylist.getSongList().toArray(new MPDSong[initialMPDPlaylist.getSongList().size()]));
		    lstNowPlaying.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			lstNowPlaying.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					lstNowPlayingClicked(e);
				}
			});
			scrollPaneNowPlaying.setViewportView(lstNowPlaying);		    
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	private void loadPlaylistToPlayistEditor(String playlistName) {
		try {
			initialMPDPlaylist.clearPlaylist();
			initialMPDPlaylist.loadPlaylist(playlistName);
			// load play list into play list editor
			setUpPlaylistEditorListBox();
		} catch (MPDPlaylistException e) {
			e.printStackTrace();
		} catch (MPDConnectionException e) {
			e.printStackTrace();
		}
	}
	
	
	private void play() {
		try {
			if (player == null) {
				player = library.getPlayer();				
			}
			player.play();
			setPlayPauseButtons();
		} catch (MPDPlayerException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} catch (MPDConnectionException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}		
	}
	
	private void playThisSong(MPDSong song) {
		try {
			checkConnection();
			player.playId(song);
		} catch (MPDPlayerException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} catch (MPDConnectionException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}		
	}

	private void pause() {
		try {
			if (player == null) {
				player = library.getPlayer();				
			}
			player.pause();
			setPauseButtons();
		} catch (MPDPlayerException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} catch (MPDConnectionException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void stop() {
		try {
			if (player == null) {
				player = library.getPlayer();				
			}
			player.stop();
			setPlayPauseButtons();
		} catch (MPDPlayerException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} catch (MPDConnectionException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void next() {
		try {
			if (player == null) {
				player = library.getPlayer();				
			}
			player.playNext();
			btnPlay.requestFocus();
			resetLooping();
		} catch (MPDPlayerException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} catch (MPDConnectionException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void previous() {
		try {
			if (player == null) {
				player = library.getPlayer();				
			}
			player.playPrev();
			btnPlay.requestFocus();
			resetLooping();
		} catch (MPDPlayerException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} catch (MPDConnectionException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	

    private void setClearButton() {
        int listCount = lstPlaylist.getModel().getSize();
        if (listCount > 0) {
            btnClear.setEnabled(true);
        } else {
            btnClear.setEnabled(false);
        }
    }
	
    // Event handler for all letter button presses
    private void onLetterSelect(String letter) {
    	currentLetter = letter;
    	onViewGroupSelect(currentListView);
    }
    
    private void updateArtistList() {
    	Collection<MPDArtist> artists;
    	if (currentLetter.equalsIgnoreCase("all")) {
    		if (currentGenre != null) {
    			artists = library.getArtistsByGenre(currentGenre);
    		}
    		else {
    			artists = library.getAllArtists();
    		}
    	}
    	else {
    		if (currentGenre != null) {
    			artists = library.getArtistsByGenreStartingWith(currentGenre,currentLetter);
    		}
    		else {
    			artists = library.getArtistsStartingWith(currentLetter);
    		}
    	}
	    lstArtists = new JList<MPDArtist>(artists.toArray(new MPDArtist[artists.size()]));
	    scrollPaneArtists.setViewportView(lstArtists);
	    lstArtists.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	    lstArtists.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
	        public void valueChanged(javax.swing.event.ListSelectionEvent arg0) {
	            lstArtistsValueChanged(arg0);
	        }
	    });
	    
	    // Empty out Album list and song list
	    lstAlbums = new JList<MPDAlbum>();
	    scrollPaneAlbums.setViewportView(lstAlbums);
	    lstSongs = new JList<MPDSong>();
	    scrollPaneSongs.setViewportView(lstSongs);
    }
    
    private void updateAlbumList() {
    	Collection<MPDAlbum> albums;
    	if (currentLetter.equalsIgnoreCase("all")) {
    		if (currentGenre != null) {
    			albums = library.getAlbumsByGenre(currentGenre);
    		}
    		else {
    			albums = library.getAllAlbums();
    		}
    	}
    	else {
       		if (currentGenre != null) {
    			albums = library.getAlbumsByGenreStartingWith(currentGenre, currentLetter);
    		}
    		else {
    			albums = library.getAlbumsStartingWith(currentLetter);
    		}
    	}
	    lstAlbums = new JList<MPDAlbum>(albums.toArray(new MPDAlbum[albums.size()]));
	    scrollPaneAlbums.setViewportView(lstAlbums);
	    lstAlbums.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	    lstAlbums.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
	        public void valueChanged(javax.swing.event.ListSelectionEvent arg0) {
	            lstAlbumsValueChanged(arg0);
	        }
	    });    	
	    // Empty out Artists list and songs list
	    lstArtists = new JList<MPDArtist>();
	    scrollPaneArtists.setViewportView(lstArtists);
	    lstSongs = new JList<MPDSong>();
	    scrollPaneSongs.setViewportView(lstSongs);
    }

    private void updateSongList() {
    	Collection<MPDSong> songs;
    	if (currentLetter.equalsIgnoreCase("all")) {
    		if (currentGenre != null) {
    			songs = library.getSongsByGenre(currentGenre);
    		}
    		else {
    			songs = library.getAllSongs();
    		}
    	}
    	else {
       		if (currentGenre != null) {
       			songs = library.getSongsByGenreStartingWith(currentGenre, currentLetter);
    		}
    		else {
    			songs = library.getSongsStartingWith(currentLetter);
    		}
    	}
	    lstSongs = new JList<MPDSong>(songs.toArray(new MPDSong[songs.size()]));
		lstSongs.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
			    JList tList = (JList) e.getSource();
			    if (e.getClickCount() == 2) { // Double-click
			        btnAdd.doClick();
			        tList.clearSelection();
			    }
			}
		});
	    scrollPaneSongs.setViewportView(lstSongs);
	    lstSongs.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	    // Empty out Artists list and Albums list
	    lstArtists = new JList<MPDArtist>();
	    scrollPaneArtists.setViewportView(lstArtists);
	    lstAlbums = new JList<MPDAlbum>();
	    scrollPaneAlbums.setViewportView(lstAlbums);    	
    }
    
    private void updateListsByGenre() {
		switch (currentListView) {
    		case ARTIST: {
    			updateArtistList();
    			break;
    		}
    		case ALBUM: {
    			updateAlbumList();
    			break;
    		}
    		case SONG: {
    			updateSongList();
    			break;
    		}
		}
    }
    
    private void onViewGroupSelect(ListView lView) {
    	if (currentLetter.isEmpty() || (library == null || !library.isConnectedToMPD())) {  //Don't do much if no letter filter selection has yet happened
    		currentListView = lView;
    	}
    	else { // Search and filter proper JList
    		currentListView = lView;
    		switch (lView) {
	    		case ARTIST: {
	    			updateArtistList();
	    			break;
	    		}
	    		case ALBUM: {
	    			updateAlbumList();
	    			break;
	    		}
	    		case SONG: {
	    			updateSongList();
	    			break;
	    		}
    		}
    	}
    }


    private void lstGenresValueChanged(javax.swing.event.ListSelectionEvent evt) { 
        if (evt.getValueIsAdjusting()) {
        	return;
	    }
	    JList genreList = (JList)evt.getSource();
	    if (genreList.getSelectedIndex() == -1) {
	    	currentGenre = null;
	    }
	    else {
	    	currentGenre = (MPDGenre)genreList.getSelectedValue();
	    }
	    updateListsByGenre();
    }                                      

    
	private void lstArtistsValueChanged(javax.swing.event.ListSelectionEvent evt) {                                       
        if (evt.getValueIsAdjusting()) {
        	return;
	    }
	    JList artistList = (JList)evt.getSource();
	    MPDArtist artist = (MPDArtist)artistList.getSelectedValue();
	
	    Collection<MPDAlbum> albums = library.getAlbumsByArtist(artist);
	    lstAlbums = new JList<MPDAlbum>(albums.toArray(new MPDAlbum[albums.size()]));
	    scrollPaneAlbums.setViewportView(lstAlbums);
	    lstAlbums.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	    lstAlbums.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
	        public void valueChanged(javax.swing.event.ListSelectionEvent arg0) {
	            lstAlbumsValueChanged(arg0);
	        }
	    });
	    // Clean out Song list
	    lstSongs = new JList<MPDSong>();
	    scrollPaneSongs.setViewportView(lstSongs);
    }                                      

	
	private void lstAlbumsValueChanged(javax.swing.event.ListSelectionEvent evt) {                                       
        if (evt.getValueIsAdjusting()) {
        	return;
	    }
	    JList albumList = (JList)evt.getSource();
	    MPDAlbum album = (MPDAlbum)albumList.getSelectedValue();
	
	    Collection<MPDSong> songs = library.getSongsByAlbum(album);
	    lstSongs = new JList<MPDSong>(songs.toArray(new MPDSong[songs.size()]));
		lstSongs.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
			    JList tList = (JList) e.getSource();
			    if (e.getClickCount() == 2) { // Double-click
			        btnAdd.doClick();
			        tList.clearSelection();
			    }
			}
		});
	    lstSongs.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	    scrollPaneSongs.setViewportView(lstSongs);
    }                                      

	
	private void initComponentsFromOptions() {
		txtServer.setText(options.getServer());
		txtPort.setText(String.valueOf(options.getPort()));
		txtPassword.setText(options.getPassword());		
	}
	
	private void attemptConnection(boolean initLibraryComponents) {
		if (txtServer.getText().trim().length() > 0) {
			try {
				changeListener.stop();
				library.closeConnection();
			} catch (Exception e) {
				//do nothing
			}
			
			try {
				library = new JavaMPDWrapper(options);
				lblStatus.setText(library.openConnection());
				if (initLibraryComponents) {
					initComponentsFromLibrary();
					loadCurrentPlaylist();
					changeListener = new MPDChangeListener(library.getConnection(), this);
					pressInitialButtons();
					btnUpdateServerDatabase.setEnabled(true);					
				}
				else  // don't register change listeners first time through because not everything is initialized yet. 
					changeListener = new MPDChangeListener(library.getConnection(), this);
				}
			catch (Exception ex) {
				ex.printStackTrace();
				btnUpdateServerDatabase.setEnabled(false);
			}
		}
	}
	
	private void lstPlaylistValueChanged(javax.swing.event.ListSelectionEvent evt) {
		if (evt.getValueIsAdjusting()) {
	        return;
	    }
	    JList playlistList = (JList) evt.getSource();
	    if (playlistList.isSelectionEmpty()) {
	        btnRemove.setEnabled(false);
	    } else {
	        btnRemove.setEnabled(true);
	    }

	    setClearButton();
	}


	private void initComponents() {
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent arg0) {
				// Close the connection to the MPD server 
				// before exiting the application 
				if (library != null) {
					try {
						library.closeConnection();
						changeListener.stop();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				options.saveOptions();
				System.exit(0);
			}
		});
		
		
		setTitle("MPD Playlist Manager");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setBounds(100, 100, 800, 600);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[]{300, 0};
		gbl_contentPane.rowHeights = new int[]{512, 29, 0};
		gbl_contentPane.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_contentPane.rowWeights = new double[]{1.0, 0.0, Double.MIN_VALUE};
		contentPane.setLayout(gbl_contentPane);
		lstPlaylist = new JList<MPDSong>();
		lstPlaylist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		lstPlaylist.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
		    public void valueChanged(javax.swing.event.ListSelectionEvent e) {
		        lstPlaylistValueChanged(e);
		    }
		});
		//scrollPanePlaylist.setViewportView(lstPlaylist);
		
		tabbedPaneMain = new JTabbedPane(JTabbedPane.TOP);
		GridBagConstraints gbc_tabbedPaneMain = new GridBagConstraints();
		gbc_tabbedPaneMain.fill = GridBagConstraints.BOTH;
		gbc_tabbedPaneMain.insets = new Insets(0, 0, 5, 0);
		gbc_tabbedPaneMain.gridx = 0;
		gbc_tabbedPaneMain.gridy = 0;
		contentPane.add(tabbedPaneMain, gbc_tabbedPaneMain);
		
		JPanel pnlPlayer = new JPanel();
		tabbedPaneMain.addTab("Player", null, pnlPlayer, null);
		tabbedPaneMain.setEnabledAt(0, true);
		GridBagLayout gbl_pnlPlayer = new GridBagLayout();
		gbl_pnlPlayer.columnWidths = new int[]{300, 125, 0};
		gbl_pnlPlayer.rowHeights = new int[]{16, 300, 35, 0, 0, 0, 11, 0};
		gbl_pnlPlayer.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_pnlPlayer.rowWeights = new double[]{0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		pnlPlayer.setLayout(gbl_pnlPlayer);
		
		JLabel lblNowPlaying = new JLabel("Now Playing");
		GridBagConstraints gbc_lblNowPlaying = new GridBagConstraints();
		gbc_lblNowPlaying.insets = new Insets(0, 0, 5, 5);
		gbc_lblNowPlaying.gridx = 0;
		gbc_lblNowPlaying.gridy = 0;
		pnlPlayer.add(lblNowPlaying, gbc_lblNowPlaying);
		
		scrollPaneNowPlaying = new JScrollPane();
		scrollPaneNowPlaying.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPaneNowPlaying.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		GridBagConstraints gbc_scrollPaneNowPlaying = new GridBagConstraints();
		gbc_scrollPaneNowPlaying.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneNowPlaying.insets = new Insets(0, 0, 5, 5);
		gbc_scrollPaneNowPlaying.gridx = 0;
		gbc_scrollPaneNowPlaying.gridy = 1;
		pnlPlayer.add(scrollPaneNowPlaying, gbc_scrollPaneNowPlaying);
		
		lstNowPlaying = new JList<MPDSong>();
		lstNowPlaying.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				lstNowPlayingClicked(arg0);
			}
		});
		scrollPaneNowPlaying.setViewportView(lstNowPlaying);
		
		JPanel pnlNowPlayingInfo = new JPanel();
		GridBagConstraints gbc_pnlNowPlayingInfo = new GridBagConstraints();
		gbc_pnlNowPlayingInfo.gridheight = 3;
		gbc_pnlNowPlayingInfo.insets = new Insets(0, 0, 5, 0);
		gbc_pnlNowPlayingInfo.fill = GridBagConstraints.BOTH;
		gbc_pnlNowPlayingInfo.gridx = 1;
		gbc_pnlNowPlayingInfo.gridy = 1;
		pnlPlayer.add(pnlNowPlayingInfo, gbc_pnlNowPlayingInfo);
		GridBagLayout gbl_pnlNowPlayingInfo = new GridBagLayout();
		gbl_pnlNowPlayingInfo.columnWidths = new int[]{0, 0, 0, 0};
		gbl_pnlNowPlayingInfo.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		gbl_pnlNowPlayingInfo.columnWeights = new double[]{0.0, 0.0, 1.0, Double.MIN_VALUE};
		gbl_pnlNowPlayingInfo.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		pnlNowPlayingInfo.setLayout(gbl_pnlNowPlayingInfo);
		
		JLabel lblTitle = new JLabel("Title:");
		lblTitle.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblTitle = new GridBagConstraints();
		gbc_lblTitle.anchor = GridBagConstraints.WEST;
		gbc_lblTitle.insets = new Insets(0, 0, 5, 5);
		gbc_lblTitle.gridx = 0;
		gbc_lblTitle.gridy = 0;
		pnlNowPlayingInfo.add(lblTitle, gbc_lblTitle);
		
		lblTitleInfo = new JLabel("");
		lblTitleInfo.setForeground(Color.BLUE);
		lblTitleInfo.setFont(new Font("Tahoma", Font.PLAIN, 11));
		GridBagConstraints gbc_lblTitleInfo = new GridBagConstraints();
		gbc_lblTitleInfo.insets = new Insets(0, 0, 5, 0);
		gbc_lblTitleInfo.anchor = GridBagConstraints.WEST;
		gbc_lblTitleInfo.gridx = 2;
		gbc_lblTitleInfo.gridy = 0;
		pnlNowPlayingInfo.add(lblTitleInfo, gbc_lblTitleInfo);
		
		JLabel lblArtist = new JLabel("Artist:");
		lblArtist.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblArtist = new GridBagConstraints();
		gbc_lblArtist.anchor = GridBagConstraints.WEST;
		gbc_lblArtist.insets = new Insets(0, 0, 5, 5);
		gbc_lblArtist.gridx = 0;
		gbc_lblArtist.gridy = 1;
		pnlNowPlayingInfo.add(lblArtist, gbc_lblArtist);
		
		lblArtistInfo = new JLabel("");
		lblArtistInfo.setFont(new Font("Tahoma", Font.PLAIN, 11));
		lblArtistInfo.setForeground(Color.BLUE);
		GridBagConstraints gbc_lblArtistInfo = new GridBagConstraints();
		gbc_lblArtistInfo.anchor = GridBagConstraints.WEST;
		gbc_lblArtistInfo.insets = new Insets(0, 0, 5, 0);
		gbc_lblArtistInfo.gridx = 2;
		gbc_lblArtistInfo.gridy = 1;
		pnlNowPlayingInfo.add(lblArtistInfo, gbc_lblArtistInfo);
		
		JLabel lblAlbum = new JLabel("Album:");
		lblAlbum.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblAlbum = new GridBagConstraints();
		gbc_lblAlbum.anchor = GridBagConstraints.WEST;
		gbc_lblAlbum.insets = new Insets(0, 0, 5, 5);
		gbc_lblAlbum.gridx = 0;
		gbc_lblAlbum.gridy = 2;
		pnlNowPlayingInfo.add(lblAlbum, gbc_lblAlbum);
		
		lblAlbumInfo = new JLabel("");
		lblAlbumInfo.setFont(new Font("Tahoma", Font.PLAIN, 11));
		lblAlbumInfo.setForeground(Color.BLUE);
		GridBagConstraints gbc_lblAlbumInfo = new GridBagConstraints();
		gbc_lblAlbumInfo.anchor = GridBagConstraints.WEST;
		gbc_lblAlbumInfo.insets = new Insets(0, 0, 5, 0);
		gbc_lblAlbumInfo.gridx = 2;
		gbc_lblAlbumInfo.gridy = 2;
		pnlNowPlayingInfo.add(lblAlbumInfo, gbc_lblAlbumInfo);
		
		JLabel lblYear = new JLabel("Year:");
		lblYear.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblYear = new GridBagConstraints();
		gbc_lblYear.anchor = GridBagConstraints.WEST;
		gbc_lblYear.insets = new Insets(0, 0, 5, 5);
		gbc_lblYear.gridx = 0;
		gbc_lblYear.gridy = 3;
		pnlNowPlayingInfo.add(lblYear, gbc_lblYear);
		
		lblYearInfo = new JLabel("");
		lblYearInfo.setFont(new Font("Tahoma", Font.PLAIN, 11));
		lblYearInfo.setForeground(Color.BLUE);
		GridBagConstraints gbc_lblYearInfo = new GridBagConstraints();
		gbc_lblYearInfo.anchor = GridBagConstraints.WEST;
		gbc_lblYearInfo.insets = new Insets(0, 0, 5, 0);
		gbc_lblYearInfo.gridx = 2;
		gbc_lblYearInfo.gridy = 3;
		pnlNowPlayingInfo.add(lblYearInfo, gbc_lblYearInfo);
		
		JLabel lblGenre = new JLabel("Genre:");
		lblGenre.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblGenre = new GridBagConstraints();
		gbc_lblGenre.anchor = GridBagConstraints.WEST;
		gbc_lblGenre.insets = new Insets(0, 0, 5, 5);
		gbc_lblGenre.gridx = 0;
		gbc_lblGenre.gridy = 4;
		pnlNowPlayingInfo.add(lblGenre, gbc_lblGenre);
		
		lblGenreInfo = new JLabel("");
		lblGenreInfo.setFont(new Font("Tahoma", Font.PLAIN, 11));
		lblGenreInfo.setForeground(Color.BLUE);
		GridBagConstraints gbc_lblGenreInfo = new GridBagConstraints();
		gbc_lblGenreInfo.insets = new Insets(0, 0, 5, 0);
		gbc_lblGenreInfo.anchor = GridBagConstraints.WEST;
		gbc_lblGenreInfo.gridx = 2;
		gbc_lblGenreInfo.gridy = 4;
		pnlNowPlayingInfo.add(lblGenreInfo, gbc_lblGenreInfo);
		
		JLabel lblSampleRate = new JLabel("Sample rate:");
		lblSampleRate.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblSampleRate = new GridBagConstraints();
		gbc_lblSampleRate.anchor = GridBagConstraints.WEST;
		gbc_lblSampleRate.insets = new Insets(0, 0, 5, 5);
		gbc_lblSampleRate.gridx = 0;
		gbc_lblSampleRate.gridy = 5;
		pnlNowPlayingInfo.add(lblSampleRate, gbc_lblSampleRate);
		
		lblSampleRateInfo = new JLabel("");
		lblSampleRateInfo.setForeground(Color.BLUE);
		lblSampleRateInfo.setFont(new Font("Tahoma", Font.PLAIN, 11));
		GridBagConstraints gbc_lblSampleRateInfo = new GridBagConstraints();
		gbc_lblSampleRateInfo.anchor = GridBagConstraints.WEST;
		gbc_lblSampleRateInfo.insets = new Insets(0, 0, 5, 0);
		gbc_lblSampleRateInfo.gridx = 2;
		gbc_lblSampleRateInfo.gridy = 5;
		pnlNowPlayingInfo.add(lblSampleRateInfo, gbc_lblSampleRateInfo);
		
		JLabel lblBitDepth = new JLabel("Bit depth: ");
		lblBitDepth.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblBitDepth = new GridBagConstraints();
		gbc_lblBitDepth.anchor = GridBagConstraints.WEST;
		gbc_lblBitDepth.insets = new Insets(0, 0, 5, 5);
		gbc_lblBitDepth.gridx = 0;
		gbc_lblBitDepth.gridy = 6;
		pnlNowPlayingInfo.add(lblBitDepth, gbc_lblBitDepth);
		
		lblBitDepthInfo = new JLabel("");
		lblBitDepthInfo.setForeground(Color.BLUE);
		lblBitDepthInfo.setFont(new Font("Tahoma", Font.PLAIN, 11));
		GridBagConstraints gbc_lblBitDepthInfo = new GridBagConstraints();
		gbc_lblBitDepthInfo.insets = new Insets(0, 0, 5, 0);
		gbc_lblBitDepthInfo.anchor = GridBagConstraints.WEST;
		gbc_lblBitDepthInfo.gridx = 2;
		gbc_lblBitDepthInfo.gridy = 6;
		pnlNowPlayingInfo.add(lblBitDepthInfo, gbc_lblBitDepthInfo);
		
		JLabel lblRate = new JLabel("Bit rate:");
		lblRate.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblRate = new GridBagConstraints();
		gbc_lblRate.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblRate.insets = new Insets(0, 0, 5, 5);
		gbc_lblRate.gridx = 0;
		gbc_lblRate.gridy = 7;
		pnlNowPlayingInfo.add(lblRate, gbc_lblRate);
		
		lblRateInfo = new JLabel("");
		lblRateInfo.setForeground(Color.BLUE);
		lblRateInfo.setFont(new Font("Tahoma", Font.PLAIN, 11));
		GridBagConstraints gbc_lblRateInfo = new GridBagConstraints();
		gbc_lblRateInfo.insets = new Insets(0, 0, 5, 0);
		gbc_lblRateInfo.anchor = GridBagConstraints.WEST;
		gbc_lblRateInfo.gridx = 2;
		gbc_lblRateInfo.gridy = 7;
		pnlNowPlayingInfo.add(lblRateInfo, gbc_lblRateInfo);
		
		JPanel pnlPlayerButtons = new JPanel();
		GridBagConstraints gbc_pnlPlayerButtons = new GridBagConstraints();
		gbc_pnlPlayerButtons.insets = new Insets(0, 0, 5, 5);
		gbc_pnlPlayerButtons.fill = GridBagConstraints.VERTICAL;
		gbc_pnlPlayerButtons.gridx = 0;
		gbc_pnlPlayerButtons.gridy = 2;
		pnlPlayer.add(pnlPlayerButtons, gbc_pnlPlayerButtons);
		
		JButton btnPrevious = new JButton("");
		btnPrevious.setToolTipText("Previous");
		btnPrevious.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				previous();
			}
		});
		pnlPlayerButtons.add(btnPrevious);
		btnPrevious.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/btn_previous.png")));
		
		btnPlay = new JButton("");
		btnPlay.setToolTipText("Play");
		pnlPlayerButtons.add(btnPlay);
		btnPlay.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				play();
			}
		});
		btnPlay.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/btn_play_1.png")));
		
		btnPause = new JButton("");
		btnPause.setToolTipText("Pause");
		btnPause.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pause();
			}
		});
		btnPause.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/btn_pause.png")));
		pnlPlayerButtons.add(btnPause);
		
		JButton btnStop = new JButton("");
		btnStop.setToolTipText("Stop");
		btnStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				stop();
			}
		});
		btnStop.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/btn_stop_new.png")));
		pnlPlayerButtons.add(btnStop);
		
		JButton btnNext = new JButton("");
		btnNext.setToolTipText("Next");
		btnNext.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				next();
			}
		});
		pnlPlayerButtons.add(btnNext);
		btnNext.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/btn_next.png")));
		
		JPanel pnlPlayerButtonsLower = new JPanel();
		GridBagConstraints gbc_pnlPlayerButtonsLower = new GridBagConstraints();
		gbc_pnlPlayerButtonsLower.insets = new Insets(0, 0, 5, 5);
		gbc_pnlPlayerButtonsLower.gridx = 0;
		gbc_pnlPlayerButtonsLower.gridy = 3;
		pnlPlayer.add(pnlPlayerButtonsLower, gbc_pnlPlayerButtonsLower);
		GridBagLayout gbl_pnlPlayerButtonsLower = new GridBagLayout();
		gbl_pnlPlayerButtonsLower.columnWidths = new int[]{52, 0, 52, 0};
		gbl_pnlPlayerButtonsLower.rowHeights = new int[]{48, 0};
		gbl_pnlPlayerButtonsLower.columnWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_pnlPlayerButtonsLower.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		pnlPlayerButtonsLower.setLayout(gbl_pnlPlayerButtonsLower);
		
		btnShuffle = new JButton("");
		btnShuffle.setToolTipText("Shuffle");
		btnShuffle.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					if (player.isRandom()) {
						player.setRandom(false);
						btnShuffle.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/img_btn_shuffle_small.png")));					
					}
					else {
						player.setRandom(true);
						btnShuffle.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/img_btn_shuffle_pressed_small.png")));					
					}
				} catch (MPDPlayerException exc) {
					exc.printStackTrace();
				} catch (MPDConnectionException exc) {
					exc.printStackTrace();
				}
			}
		});
		
		btnRepeat = new JButton("");
		btnRepeat.setToolTipText("Repeat");
		btnRepeat.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					if (player.isRepeat()) {
						player.setRepeat(false);
						btnRepeat.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/img_btn_repeat_small.png")));					
					}
					else {
						player.setRepeat(true);
						btnRepeat.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/img_btn_repeat_pressed_small.png")));					
					}
				} catch (MPDPlayerException e) {
					e.printStackTrace();
				} catch (MPDConnectionException e) {
					e.printStackTrace();
				}
			}
		});
		btnRepeat.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/img_btn_repeat_small.png")));
		GridBagConstraints gbc_btnRepeat = new GridBagConstraints();
		gbc_btnRepeat.insets = new Insets(0, 0, 0, 5);
		gbc_btnRepeat.gridx = 0;
		gbc_btnRepeat.gridy = 0;
		pnlPlayerButtonsLower.add(btnRepeat, gbc_btnRepeat);
		
		btnLoop = new JButton("");
		btnLoop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setUpLoop();
			}
		});
		btnLoop.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/btn_loop.png")));
		btnLoop.setToolTipText("Loop within song");
		GridBagConstraints gbc_btnLoop = new GridBagConstraints();
		gbc_btnLoop.insets = new Insets(0, 0, 0, 5);
		gbc_btnLoop.gridx = 1;
		gbc_btnLoop.gridy = 0;
		pnlPlayerButtonsLower.add(btnLoop, gbc_btnLoop);
		btnShuffle.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/img_btn_shuffle_small.png")));
		GridBagConstraints gbc_btnShuffle = new GridBagConstraints();
		gbc_btnShuffle.gridx = 2;
		gbc_btnShuffle.gridy = 0;
		pnlPlayerButtonsLower.add(btnShuffle, gbc_btnShuffle);
		
		sliderVolume = new JSlider(JSlider.HORIZONTAL,0,100,100);
		sliderVolume.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent arg0) {
				try {
					player.setVolume(sliderVolume.getValue());
				} catch (MPDResponseException e) {
					sliderVolume.setValue(0);					
				} catch (Exception e) {
					sliderVolume.setValue(0);
				}
			}
		});
		sliderVolume.setToolTipText("Volume");
		GridBagConstraints gbc_sliderVolume = new GridBagConstraints();
		gbc_sliderVolume.fill = GridBagConstraints.HORIZONTAL;
		gbc_sliderVolume.insets = new Insets(0, 0, 5, 5);
		gbc_sliderVolume.gridx = 0;
		gbc_sliderVolume.gridy = 4;
		pnlPlayer.add(sliderVolume, gbc_sliderVolume);
		
		JPanel pnlTimeLabels = new JPanel();
		GridBagConstraints gbc_pnlTimeLabels = new GridBagConstraints();
		gbc_pnlTimeLabels.insets = new Insets(0, 0, 5, 5);
		gbc_pnlTimeLabels.fill = GridBagConstraints.HORIZONTAL;
		gbc_pnlTimeLabels.gridx = 0;
		gbc_pnlTimeLabels.gridy = 5;
		pnlPlayer.add(pnlTimeLabels, gbc_pnlTimeLabels);
		pnlTimeLabels.setLayout(new GridLayout(0, 2, 0, 0));
		
		lblElapsedTime = new JLabel("Elapsed");
		lblElapsedTime.setFont(new Font("Lucida Grande", Font.PLAIN, 10));
		lblElapsedTime.setVerticalAlignment(SwingConstants.BOTTOM);
		pnlTimeLabels.add(lblElapsedTime);
		
		lblTotalTime = new JLabel("Total");
		lblTotalTime.setFont(new Font("Lucida Grande", Font.PLAIN, 10));
		lblTotalTime.setVerticalAlignment(SwingConstants.BOTTOM);
		lblTotalTime.setHorizontalAlignment(SwingConstants.TRAILING);
		pnlTimeLabels.add(lblTotalTime);
		
		//JProgress bar with a custom tool-tip to show where in a song
		// clicking would scroll to.
		progbarSongTime = new JProgressBar() {
			private static final long serialVersionUID = 1L;

			public JToolTip createToolTip() {
				return super.createToolTip();
		    }
			
			public boolean contains(int x, int y) {
				boolean isPlaying = false;
				try {
					PlayerStatus ps = player.getStatus();
					if (ps == PlayerStatus.STATUS_PLAYING) 
						isPlaying = true;
				} catch (MPDResponseException e) {
					//e.printStackTrace();
				} catch (MPDConnectionException e) {
					//e.printStackTrace();
				} catch (NullPointerException e) {
					//e.printStackTrace();
				}
				
				if (isPlaying) {
					float percentage = ((float)x / (float)this.getWidth());
					setToolTipText(convertTimeInSecondsToString((int)(progbarSongTime.getMaximum() * percentage)));
				} else {
					setToolTipText("Click to go to location in song");
				}
				return super.contains(x, y);
			}
		};
		
		GridBagConstraints gbc_progbarSongTime = new GridBagConstraints();
		gbc_progbarSongTime.insets = new Insets(0, 0, 0, 5);
		gbc_progbarSongTime.fill = GridBagConstraints.HORIZONTAL;
		gbc_progbarSongTime.gridx = 0;
		gbc_progbarSongTime.gridy = 6;
		pnlPlayer.add(progbarSongTime, gbc_progbarSongTime);

		
		JPanel pnlPlaylistEditor = new JPanel();
		tabbedPaneMain.addTab("Playlist Editor", null, pnlPlaylistEditor, null);
		GridBagLayout gbl_pnlPlaylistEditor = new GridBagLayout();
		gbl_pnlPlaylistEditor.columnWidths = new int[]{763, 0};
		gbl_pnlPlaylistEditor.rowHeights = new int[]{175, 0, 0, 0};
		gbl_pnlPlaylistEditor.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_pnlPlaylistEditor.rowWeights = new double[]{0.0, 0.0, 1.0, Double.MIN_VALUE};
		pnlPlaylistEditor.setLayout(gbl_pnlPlaylistEditor);
		
		JPanel pnlUpperPlaylist = new JPanel();
		GridBagConstraints gbc_pnlUpperPlaylist = new GridBagConstraints();
		gbc_pnlUpperPlaylist.insets = new Insets(0, 0, 5, 0);
		gbc_pnlUpperPlaylist.fill = GridBagConstraints.BOTH;
		gbc_pnlUpperPlaylist.gridx = 0;
		gbc_pnlUpperPlaylist.gridy = 0;
		pnlPlaylistEditor.add(pnlUpperPlaylist, gbc_pnlUpperPlaylist);
		GridBagLayout gbl_pnlUpperPlaylist = new GridBagLayout();
		gbl_pnlUpperPlaylist.columnWidths = new int[]{335, 0, 0, 0};
		gbl_pnlUpperPlaylist.rowHeights = new int[]{175, 0};
		gbl_pnlUpperPlaylist.columnWeights = new double[]{0.0, 0.0, 1.0, Double.MIN_VALUE};
		gbl_pnlUpperPlaylist.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		pnlUpperPlaylist.setLayout(gbl_pnlUpperPlaylist);
		
		JPanel pnlArtistSelection = new JPanel();
		GridBagConstraints gbc_pnlArtistSelection = new GridBagConstraints();
		gbc_pnlArtistSelection.fill = GridBagConstraints.BOTH;
		gbc_pnlArtistSelection.insets = new Insets(0, 0, 0, 5);
		gbc_pnlArtistSelection.gridx = 0;
		gbc_pnlArtistSelection.gridy = 0;
		pnlUpperPlaylist.add(pnlArtistSelection, gbc_pnlArtistSelection);
		pnlArtistSelection.setBorder(new LineBorder(new Color(0, 0, 0)));
		GridBagLayout gbl_pnlArtistSelection = new GridBagLayout();
		gbl_pnlArtistSelection.columnWidths = new int[]{85, 33, 31, 33, 33, 31, 31, 33, 0};
		gbl_pnlArtistSelection.rowHeights = new int[]{23, 0, 0, 0, 0, 0};
		gbl_pnlArtistSelection.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_pnlArtistSelection.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		pnlArtistSelection.setLayout(gbl_pnlArtistSelection);
		
		JLabel lblSelection = new JLabel("Select");
		GridBagConstraints gbc_lblSelection = new GridBagConstraints();
		gbc_lblSelection.insets = new Insets(0, 0, 5, 5);
		gbc_lblSelection.gridx = 0;
		gbc_lblSelection.gridy = 0;
		pnlArtistSelection.add(lblSelection, gbc_lblSelection);
		
		JRadioButton rdbtnA = new JRadioButton("A");
		rdbtnA.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    onLetterSelect(e.getActionCommand());
			}
		});
		btnGrpLetterSelection.add(rdbtnA);
		GridBagConstraints gbc_rdbtnA = new GridBagConstraints();
		gbc_rdbtnA.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnA.gridx = 1;
		gbc_rdbtnA.gridy = 0;
		pnlArtistSelection.add(rdbtnA, gbc_rdbtnA);
		
		JRadioButton rdbtnB = new JRadioButton("B");
		rdbtnB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    onLetterSelect(e.getActionCommand());
			    }
		});
		btnGrpLetterSelection.add(rdbtnB);
		GridBagConstraints gbc_rdbtnB = new GridBagConstraints();
		gbc_rdbtnB.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnB.gridx = 2;
		gbc_rdbtnB.gridy = 0;
		pnlArtistSelection.add(rdbtnB, gbc_rdbtnB);
		
		JRadioButton rdbtnC = new JRadioButton("C");
		rdbtnC.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    onLetterSelect(e.getActionCommand());
			}
		});
		btnGrpLetterSelection.add(rdbtnC);
		GridBagConstraints gbc_rdbtnC = new GridBagConstraints();
		gbc_rdbtnC.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnC.gridx = 3;
		gbc_rdbtnC.gridy = 0;
		pnlArtistSelection.add(rdbtnC, gbc_rdbtnC);
		
		JRadioButton rdbtnD = new JRadioButton("D");
		rdbtnD.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    onLetterSelect(e.getActionCommand());
			}
		});
		btnGrpLetterSelection.add(rdbtnD);
		GridBagConstraints gbc_rdbtnD = new GridBagConstraints();
		gbc_rdbtnD.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnD.gridx = 4;
		gbc_rdbtnD.gridy = 0;
		pnlArtistSelection.add(rdbtnD, gbc_rdbtnD);
		
		JRadioButton rdbtnE = new JRadioButton("E");
		rdbtnE.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    onLetterSelect(e.getActionCommand());
			}
		});
		btnGrpLetterSelection.add(rdbtnE);
		GridBagConstraints gbc_rdbtnE = new GridBagConstraints();
		gbc_rdbtnE.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnE.gridx = 5;
		gbc_rdbtnE.gridy = 0;
		pnlArtistSelection.add(rdbtnE, gbc_rdbtnE);
		
		JRadioButton rdbtnF = new JRadioButton("F");
		rdbtnF.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    onLetterSelect(e.getActionCommand());
			}
		});
		btnGrpLetterSelection.add(rdbtnF);
		GridBagConstraints gbc_rdbtnF = new GridBagConstraints();
		gbc_rdbtnF.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnF.gridx = 6;
		gbc_rdbtnF.gridy = 0;
		pnlArtistSelection.add(rdbtnF, gbc_rdbtnF);
		
		JRadioButton rdbtnG = new JRadioButton("G");
		rdbtnG.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    onLetterSelect(e.getActionCommand());
			}
		});
		btnGrpLetterSelection.add(rdbtnG);
		GridBagConstraints gbc_rdbtnG = new GridBagConstraints();
		gbc_rdbtnG.insets = new Insets(0, 0, 5, 0);
		gbc_rdbtnG.gridx = 7;
		gbc_rdbtnG.gridy = 0;
		pnlArtistSelection.add(rdbtnG, gbc_rdbtnG);
		
		rdbtnArtists = new JRadioButton("Artists");
		rdbtnArtists.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onViewGroupSelect(ListView.ARTIST);
			}
		});
		btnGroupArtistsAlbumsSongs.add(rdbtnArtists);
		GridBagConstraints gbc_rdbtnArtists = new GridBagConstraints();
		gbc_rdbtnArtists.anchor = GridBagConstraints.WEST;
		gbc_rdbtnArtists.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnArtists.gridx = 0;
		gbc_rdbtnArtists.gridy = 1;
		pnlArtistSelection.add(rdbtnArtists, gbc_rdbtnArtists);
		
		JRadioButton rdbtnH = new JRadioButton("H");
		rdbtnH.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    onLetterSelect(e.getActionCommand());
			}
		});
		btnGrpLetterSelection.add(rdbtnH);
		GridBagConstraints gbc_rdbtnH = new GridBagConstraints();
		gbc_rdbtnH.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnH.gridx = 1;
		gbc_rdbtnH.gridy = 1;
		pnlArtistSelection.add(rdbtnH, gbc_rdbtnH);
		
		JRadioButton rdbtnI = new JRadioButton("I");
		rdbtnI.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    onLetterSelect(e.getActionCommand());
			}
		});
		btnGrpLetterSelection.add(rdbtnI);
		GridBagConstraints gbc_rdbtnI = new GridBagConstraints();
		gbc_rdbtnI.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnI.gridx = 2;
		gbc_rdbtnI.gridy = 1;
		pnlArtistSelection.add(rdbtnI, gbc_rdbtnI);
		
		JRadioButton rdbtnJ = new JRadioButton("J");
		rdbtnJ.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    onLetterSelect(e.getActionCommand());
			}
		});
		btnGrpLetterSelection.add(rdbtnJ);
		GridBagConstraints gbc_rdbtnJ = new GridBagConstraints();
		gbc_rdbtnJ.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnJ.gridx = 3;
		gbc_rdbtnJ.gridy = 1;
		pnlArtistSelection.add(rdbtnJ, gbc_rdbtnJ);
		
		JRadioButton rdbtnK = new JRadioButton("K");
		rdbtnK.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    onLetterSelect(e.getActionCommand());
			}
		});
		btnGrpLetterSelection.add(rdbtnK);
		GridBagConstraints gbc_rdbtnK = new GridBagConstraints();
		gbc_rdbtnK.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnK.gridx = 4;
		gbc_rdbtnK.gridy = 1;
		pnlArtistSelection.add(rdbtnK, gbc_rdbtnK);
		
		JRadioButton rdbtnL = new JRadioButton("L");
		rdbtnL.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    onLetterSelect(e.getActionCommand());
			}
		});
		btnGrpLetterSelection.add(rdbtnL);
		GridBagConstraints gbc_rdbtnL = new GridBagConstraints();
		gbc_rdbtnL.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnL.gridx = 5;
		gbc_rdbtnL.gridy = 1;
		pnlArtistSelection.add(rdbtnL, gbc_rdbtnL);
		
		JRadioButton rdbtnM = new JRadioButton("M");
		rdbtnM.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    onLetterSelect(e.getActionCommand());
			}
		});
		btnGrpLetterSelection.add(rdbtnM);
		GridBagConstraints gbc_rdbtnM = new GridBagConstraints();
		gbc_rdbtnM.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnM.gridx = 6;
		gbc_rdbtnM.gridy = 1;
		pnlArtistSelection.add(rdbtnM, gbc_rdbtnM);
		
		JRadioButton rdbtnN = new JRadioButton("N");
		rdbtnN.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    onLetterSelect(e.getActionCommand());
			}
		});
		btnGrpLetterSelection.add(rdbtnN);
		GridBagConstraints gbc_rdbtnN = new GridBagConstraints();
		gbc_rdbtnN.insets = new Insets(0, 0, 5, 0);
		gbc_rdbtnN.gridx = 7;
		gbc_rdbtnN.gridy = 1;
		pnlArtistSelection.add(rdbtnN, gbc_rdbtnN);
		
		JRadioButton rdbtnAlbums = new JRadioButton("Albums");
		rdbtnAlbums.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onViewGroupSelect(ListView.ALBUM);
			}
		});
		btnGroupArtistsAlbumsSongs.add(rdbtnAlbums);
		GridBagConstraints gbc_rdbtnAlbums = new GridBagConstraints();
		gbc_rdbtnAlbums.anchor = GridBagConstraints.WEST;
		gbc_rdbtnAlbums.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnAlbums.gridx = 0;
		gbc_rdbtnAlbums.gridy = 2;
		pnlArtistSelection.add(rdbtnAlbums, gbc_rdbtnAlbums);
		
		JRadioButton rdbtnO = new JRadioButton("O");
		rdbtnO.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    onLetterSelect(e.getActionCommand());
			}
		});
		btnGrpLetterSelection.add(rdbtnO);
		GridBagConstraints gbc_rdbtnO = new GridBagConstraints();
		gbc_rdbtnO.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnO.gridx = 1;
		gbc_rdbtnO.gridy = 2;
		pnlArtistSelection.add(rdbtnO, gbc_rdbtnO);
		
		JRadioButton rdbtnP = new JRadioButton("P");
		rdbtnP.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    onLetterSelect(e.getActionCommand());
			}
		});
		btnGrpLetterSelection.add(rdbtnP);
		GridBagConstraints gbc_rdbtnP = new GridBagConstraints();
		gbc_rdbtnP.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnP.gridx = 2;
		gbc_rdbtnP.gridy = 2;
		pnlArtistSelection.add(rdbtnP, gbc_rdbtnP);
		
		JRadioButton rdbtnQ = new JRadioButton("Q");
		rdbtnQ.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    onLetterSelect(e.getActionCommand());
			}
		});
		btnGrpLetterSelection.add(rdbtnQ);
		GridBagConstraints gbc_rdbtnQ = new GridBagConstraints();
		gbc_rdbtnQ.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnQ.gridx = 3;
		gbc_rdbtnQ.gridy = 2;
		pnlArtistSelection.add(rdbtnQ, gbc_rdbtnQ);
		
		JRadioButton rdbtnR = new JRadioButton("R");
		rdbtnR.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    onLetterSelect(e.getActionCommand());
			}
		});
		btnGrpLetterSelection.add(rdbtnR);
		GridBagConstraints gbc_rdbtnR = new GridBagConstraints();
		gbc_rdbtnR.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnR.gridx = 4;
		gbc_rdbtnR.gridy = 2;
		pnlArtistSelection.add(rdbtnR, gbc_rdbtnR);
		
		JRadioButton rdbtnS = new JRadioButton("S");
		rdbtnS.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    onLetterSelect(e.getActionCommand());
			}
		});
		btnGrpLetterSelection.add(rdbtnS);
		GridBagConstraints gbc_rdbtnS = new GridBagConstraints();
		gbc_rdbtnS.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnS.gridx = 5;
		gbc_rdbtnS.gridy = 2;
		pnlArtistSelection.add(rdbtnS, gbc_rdbtnS);
		
		JRadioButton rdbtnT = new JRadioButton("T");
		rdbtnT.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    onLetterSelect(e.getActionCommand());
			}
		});
		btnGrpLetterSelection.add(rdbtnT);
		GridBagConstraints gbc_rdbtnT = new GridBagConstraints();
		gbc_rdbtnT.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnT.gridx = 6;
		gbc_rdbtnT.gridy = 2;
		pnlArtistSelection.add(rdbtnT, gbc_rdbtnT);
		
		JRadioButton rdbtnU = new JRadioButton("U");
		rdbtnU.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    onLetterSelect(e.getActionCommand());
			}
		});
		btnGrpLetterSelection.add(rdbtnU);
		GridBagConstraints gbc_rdbtnU = new GridBagConstraints();
		gbc_rdbtnU.insets = new Insets(0, 0, 5, 0);
		gbc_rdbtnU.gridx = 7;
		gbc_rdbtnU.gridy = 2;
		pnlArtistSelection.add(rdbtnU, gbc_rdbtnU);
		
		JRadioButton rdbtnSongs = new JRadioButton("Songs");
		rdbtnSongs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onViewGroupSelect(ListView.SONG);
			}
		});
		btnGroupArtistsAlbumsSongs.add(rdbtnSongs);
		GridBagConstraints gbc_rdbtnSongs = new GridBagConstraints();
		gbc_rdbtnSongs.anchor = GridBagConstraints.WEST;
		gbc_rdbtnSongs.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnSongs.gridx = 0;
		gbc_rdbtnSongs.gridy = 3;
		pnlArtistSelection.add(rdbtnSongs, gbc_rdbtnSongs);
		
		JRadioButton rdbtnV = new JRadioButton("V");
		rdbtnV.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    onLetterSelect(e.getActionCommand());
			}
		});
		btnGrpLetterSelection.add(rdbtnV);
		GridBagConstraints gbc_rdbtnV = new GridBagConstraints();
		gbc_rdbtnV.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnV.gridx = 1;
		gbc_rdbtnV.gridy = 3;
		pnlArtistSelection.add(rdbtnV, gbc_rdbtnV);
		
		JRadioButton rdbtnW = new JRadioButton("W");
		rdbtnW.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    onLetterSelect(e.getActionCommand());
			}
		});
		btnGrpLetterSelection.add(rdbtnW);
		GridBagConstraints gbc_rdbtnW = new GridBagConstraints();
		gbc_rdbtnW.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnW.gridx = 2;
		gbc_rdbtnW.gridy = 3;
		pnlArtistSelection.add(rdbtnW, gbc_rdbtnW);
		
		JRadioButton rdbtnX = new JRadioButton("X");
		rdbtnX.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    onLetterSelect(e.getActionCommand());
			}
		});
		btnGrpLetterSelection.add(rdbtnX);
		GridBagConstraints gbc_rdbtnX = new GridBagConstraints();
		gbc_rdbtnX.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnX.gridx = 3;
		gbc_rdbtnX.gridy = 3;
		pnlArtistSelection.add(rdbtnX, gbc_rdbtnX);
		
		JRadioButton rdbtnY = new JRadioButton("Y");
		rdbtnY.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    onLetterSelect(e.getActionCommand());
			}
		});
		btnGrpLetterSelection.add(rdbtnY);
		GridBagConstraints gbc_rdbtnY = new GridBagConstraints();
		gbc_rdbtnY.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnY.gridx = 4;
		gbc_rdbtnY.gridy = 3;
		pnlArtistSelection.add(rdbtnY, gbc_rdbtnY);
		
		JRadioButton rdbtnZ = new JRadioButton("Z");
		rdbtnZ.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    onLetterSelect(e.getActionCommand());
			}
		});
		btnGrpLetterSelection.add(rdbtnZ);
		GridBagConstraints gbc_rdbtnZ = new GridBagConstraints();
		gbc_rdbtnZ.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnZ.gridx = 5;
		gbc_rdbtnZ.gridy = 3;
		pnlArtistSelection.add(rdbtnZ, gbc_rdbtnZ);
		
		rdbtnAll = new JRadioButton("All");
		rdbtnAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onLetterSelect(e.getActionCommand());
			}
		});
		btnGrpLetterSelection.add(rdbtnAll);
		GridBagConstraints gbc_rdbtnAll = new GridBagConstraints();
		gbc_rdbtnAll.anchor = GridBagConstraints.WEST;
		gbc_rdbtnAll.gridwidth = 2;
		gbc_rdbtnAll.insets = new Insets(0, 0, 5, 0);
		gbc_rdbtnAll.gridx = 6;
		gbc_rdbtnAll.gridy = 3;
		pnlArtistSelection.add(rdbtnAll, gbc_rdbtnAll);
		
		JPanel pnlGenres = new JPanel();
		GridBagConstraints gbc_pnlGenres = new GridBagConstraints();
		gbc_pnlGenres.fill = GridBagConstraints.BOTH;
		gbc_pnlGenres.gridx = 2;
		gbc_pnlGenres.gridy = 0;
		pnlUpperPlaylist.add(pnlGenres, gbc_pnlGenres);
		pnlGenres.setBorder(new LineBorder(new Color(0, 0, 0)));
		GridBagLayout gbl_pnlGenres = new GridBagLayout();
		gbl_pnlGenres.columnWidths = new int[]{0, 0, 0};
		gbl_pnlGenres.rowHeights = new int[]{0, 0};
		gbl_pnlGenres.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_pnlGenres.rowWeights = new double[]{1.0, Double.MIN_VALUE};
		pnlGenres.setLayout(gbl_pnlGenres);
		
		JLabel lblGenres = new JLabel("Genres");
		GridBagConstraints gbc_lblGenres = new GridBagConstraints();
		gbc_lblGenres.anchor = GridBagConstraints.NORTH;
		gbc_lblGenres.insets = new Insets(0, 0, 0, 5);
		gbc_lblGenres.gridx = 0;
		gbc_lblGenres.gridy = 0;
		pnlGenres.add(lblGenres, gbc_lblGenres);
		
		scrollPaneGenres = new JScrollPane();
		scrollPaneGenres.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPaneGenres.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		GridBagConstraints gbc_scrollPaneGenres = new GridBagConstraints();
		gbc_scrollPaneGenres.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneGenres.gridx = 1;
		gbc_scrollPaneGenres.gridy = 0;
		pnlGenres.add(scrollPaneGenres, gbc_scrollPaneGenres);
		
		lstGenres = new JList<MPDGenre>();
		lstGenres.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent arg0) {
				lstGenresValueChanged(arg0);
			}
		});
		scrollPaneGenres.setViewportView(lstGenres);
		lstGenres.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		JPanel pnlLowerPlaylist = new JPanel();
		GridBagConstraints gbc_pnlLowerPlaylist = new GridBagConstraints();
		gbc_pnlLowerPlaylist.fill = GridBagConstraints.BOTH;
		gbc_pnlLowerPlaylist.gridx = 0;
		gbc_pnlLowerPlaylist.gridy = 2;
		pnlPlaylistEditor.add(pnlLowerPlaylist, gbc_pnlLowerPlaylist);
		GridBagLayout gbl_pnlLowerPlaylist = new GridBagLayout();
		gbl_pnlLowerPlaylist.columnWidths = new int[]{0, 0, 0, 38, 0, 0, 0};
		gbl_pnlLowerPlaylist.rowHeights = new int[]{0, 0, 0};
		gbl_pnlLowerPlaylist.columnWeights = new double[]{1.0, 1.0, 1.0, 0.0, 1.0, 0.0, Double.MIN_VALUE};
		gbl_pnlLowerPlaylist.rowWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		pnlLowerPlaylist.setLayout(gbl_pnlLowerPlaylist);
		
		JLabel lblArtists = new JLabel("Artists");
		GridBagConstraints gbc_lblArtists = new GridBagConstraints();
		gbc_lblArtists.insets = new Insets(0, 0, 5, 5);
		gbc_lblArtists.gridx = 0;
		gbc_lblArtists.gridy = 0;
		pnlLowerPlaylist.add(lblArtists, gbc_lblArtists);
		
		JLabel lblAlbums = new JLabel("Albums");
		GridBagConstraints gbc_lblAlbums = new GridBagConstraints();
		gbc_lblAlbums.insets = new Insets(0, 0, 5, 5);
		gbc_lblAlbums.gridx = 1;
		gbc_lblAlbums.gridy = 0;
		pnlLowerPlaylist.add(lblAlbums, gbc_lblAlbums);
		
		JLabel lblSongs = new JLabel("Songs");
		GridBagConstraints gbc_lblSongs = new GridBagConstraints();
		gbc_lblSongs.insets = new Insets(0, 0, 5, 5);
		gbc_lblSongs.gridx = 2;
		gbc_lblSongs.gridy = 0;
		pnlLowerPlaylist.add(lblSongs, gbc_lblSongs);
		
		JLabel lblPlaylist = new JLabel("Playlist");
		GridBagConstraints gbc_lblPlaylist = new GridBagConstraints();
		gbc_lblPlaylist.insets = new Insets(0, 0, 5, 5);
		gbc_lblPlaylist.gridx = 4;
		gbc_lblPlaylist.gridy = 0;
		pnlLowerPlaylist.add(lblPlaylist, gbc_lblPlaylist);
		
		scrollPaneArtists = new JScrollPane();
		scrollPaneArtists.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPaneArtists.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		GridBagConstraints gbc_scrollPaneArtists = new GridBagConstraints();
		gbc_scrollPaneArtists.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneArtists.insets = new Insets(0, 0, 0, 5);
		gbc_scrollPaneArtists.gridx = 0;
		gbc_scrollPaneArtists.gridy = 1;
		pnlLowerPlaylist.add(scrollPaneArtists, gbc_scrollPaneArtists);
		
		lstArtists = new JList<MPDArtist>();
		lstArtists.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scrollPaneArtists.setViewportView(lstArtists);
		
		scrollPaneAlbums = new JScrollPane();
		scrollPaneAlbums.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPaneAlbums.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		GridBagConstraints gbc_scrollPaneAlbums = new GridBagConstraints();
		gbc_scrollPaneAlbums.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneAlbums.insets = new Insets(0, 0, 0, 5);
		gbc_scrollPaneAlbums.gridx = 1;
		gbc_scrollPaneAlbums.gridy = 1;
		pnlLowerPlaylist.add(scrollPaneAlbums, gbc_scrollPaneAlbums);
		
		lstAlbums = new JList<MPDAlbum>();
		lstAlbums.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scrollPaneAlbums.setViewportView(lstAlbums);
		
		scrollPaneSongs = new JScrollPane();
		scrollPaneSongs.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPaneSongs.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		GridBagConstraints gbc_scrollPaneSongs = new GridBagConstraints();
		gbc_scrollPaneSongs.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneSongs.insets = new Insets(0, 0, 0, 5);
		gbc_scrollPaneSongs.gridx = 2;
		gbc_scrollPaneSongs.gridy = 1;
		pnlLowerPlaylist.add(scrollPaneSongs, gbc_scrollPaneSongs);
		
		lstSongs = new JList<MPDSong>();
		lstSongs.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
			    JList tList = (JList) e.getSource();
			    if (e.getClickCount() == 2) { // Double-click
			        btnAdd.doClick();
			        tList.clearSelection();
			    }
			}
		});
		
		scrollPaneSongs.setViewportView(lstSongs);
		
		JPanel pnlAddRemoveButtons = new JPanel();
		GridBagConstraints gbc_pnlAddRemoveButtons = new GridBagConstraints();
		gbc_pnlAddRemoveButtons.insets = new Insets(0, 0, 0, 5);
		gbc_pnlAddRemoveButtons.fill = GridBagConstraints.BOTH;
		gbc_pnlAddRemoveButtons.gridx = 3;
		gbc_pnlAddRemoveButtons.gridy = 1;
		pnlLowerPlaylist.add(pnlAddRemoveButtons, gbc_pnlAddRemoveButtons);
		GridBagLayout gbl_pnlAddRemoveButtons = new GridBagLayout();
		gbl_pnlAddRemoveButtons.columnWidths = new int[]{33, 0};
		gbl_pnlAddRemoveButtons.rowHeights = new int[]{60, 37, 35, 37, 0, 0};
		gbl_pnlAddRemoveButtons.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gbl_pnlAddRemoveButtons.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		pnlAddRemoveButtons.setLayout(gbl_pnlAddRemoveButtons);
		
		btnAdd = new JButton("");
		btnAdd.setToolTipText("Add songs to playlist");
		btnAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			    //Object[] selectedSongs = lstSongs.getSelectedValues();
				List<MPDSong> selectedSongs = lstSongs.getSelectedValuesList();
			    for (MPDSong s : selectedSongs) {
			    	try {
						initialMPDPlaylist.addSong(s);
					} catch (MPDPlaylistException e1) {
						e1.printStackTrace();
					} catch (MPDConnectionException e1) {
						e1.printStackTrace();
					}
			    }
			    setUpPlaylistEditorListBox();
			    
			    btnSave.setEnabled(true);
			    btnClear.setEnabled(true);
			}
		});
		btnAdd.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/right-arrow17.gif")));
		GridBagConstraints gbc_btnAdd = new GridBagConstraints();
		gbc_btnAdd.insets = new Insets(0, 0, 5, 0);
		gbc_btnAdd.gridx = 0;
		gbc_btnAdd.gridy = 2;
		pnlAddRemoveButtons.add(btnAdd, gbc_btnAdd);
		
		btnRemove = new JButton("");
		btnRemove.setToolTipText("Remove songs from playlist");
		btnRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    List<MPDSong> songs = lstPlaylist.getSelectedValuesList();
			    for (MPDSong song : songs) {
			    	try {
						initialMPDPlaylist.removeSong(song);
						//TODO: Scroll to nearby song in playlist list
					} catch (MPDPlaylistException e1) {
						e1.printStackTrace();
					} catch (MPDConnectionException e1) {
						e1.printStackTrace();
					}
			    }
			    setUpPlaylistEditorListBox();
			    
			    if (lstPlaylist.getModel().getSize() > 0) {
			        btnSave.setEnabled(true);
			        btnClear.setEnabled(true);
			    } else {
			        btnSave.setEnabled(false);
			        btnClear.setEnabled(false);
			    }
			}
		});
		btnRemove.setIcon(new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/larrow17.gif")));
		GridBagConstraints gbc_btnRemove = new GridBagConstraints();
		gbc_btnRemove.gridx = 0;
		gbc_btnRemove.gridy = 4;
		pnlAddRemoveButtons.add(btnRemove, gbc_btnRemove);
		
		scrollPanePlaylist = new JScrollPane();
		scrollPanePlaylist.setToolTipText("");
		scrollPanePlaylist.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPanePlaylist.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		GridBagConstraints gbc_scrollPanePlaylist = new GridBagConstraints();
		gbc_scrollPanePlaylist.insets = new Insets(0, 0, 0, 5);
		gbc_scrollPanePlaylist.fill = GridBagConstraints.BOTH;
		gbc_scrollPanePlaylist.gridx = 4;
		gbc_scrollPanePlaylist.gridy = 1;
		pnlLowerPlaylist.add(scrollPanePlaylist, gbc_scrollPanePlaylist);
		
		lstPlaylist = new JList<MPDSong>();
		lstPlaylist.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				lstPlaylistValueChanged(e);
			}
		});
		scrollPanePlaylist.setViewportView(lstPlaylist);
		
		JPanel pnlPlaylistButtons = new JPanel();
		GridBagConstraints gbc_pnlPlaylistButtons = new GridBagConstraints();
		gbc_pnlPlaylistButtons.fill = GridBagConstraints.BOTH;
		gbc_pnlPlaylistButtons.gridx = 5;
		gbc_pnlPlaylistButtons.gridy = 1;
		pnlLowerPlaylist.add(pnlPlaylistButtons, gbc_pnlPlaylistButtons);
		GridBagLayout gbl_pnlPlaylistButtons = new GridBagLayout();
		gbl_pnlPlaylistButtons.columnWidths = new int[]{76, 0};
		gbl_pnlPlaylistButtons.rowHeights = new int[]{26, 26, 37, 29, 38, 29, 29, 29, 0};
		gbl_pnlPlaylistButtons.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gbl_pnlPlaylistButtons.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		pnlPlaylistButtons.setLayout(gbl_pnlPlaylistButtons);
		
		btnLoad = new JButton("Load");
		btnLoad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final ImageIcon icon = new ImageIcon(Main.class.getResource("/org/aljordan/ajmpdcontrol/folder.png"));				
				Object[] playlists = library.getPlaylists();
				String playlistName = (String)JOptionPane.showInputDialog(
				                    null,
				                    "Choose playlist:",
				                    "Load Server Playlist",
				                    JOptionPane.PLAIN_MESSAGE,
				                    icon,
				                    playlists,
				                    null);

				if ((playlistName != null) && (playlistName.length() > 0)) {
				    loadPlaylistToPlayistEditor(playlistName);
				    currentPlaylistName = playlistName;
				    return;
				}
				//If you're here, the return value was null/empty.
			    System.out.println("Didn't get a playlist.");
			}
		});
		GridBagConstraints gbc_btnLoad = new GridBagConstraints();
		gbc_btnLoad.insets = new Insets(0, 0, 5, 0);
		gbc_btnLoad.gridx = 0;
		gbc_btnLoad.gridy = 1;
		pnlPlaylistButtons.add(btnLoad, gbc_btnLoad);
		
		btnClear = new JButton("Clear");
		btnClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					initialMPDPlaylist.clearPlaylist();
				} catch (MPDPlaylistException e1) {
					e1.printStackTrace();
				} catch (MPDConnectionException e1) {
					e1.printStackTrace();
				}
				setUpPlaylistEditorListBox();
				setClearButton();
			}
		});
		
		btnSave = new JButton("Save");
		btnSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String playlistName = JOptionPane.showInputDialog("Enter playlist name:", currentPlaylistName);
				if(playlistName != null && !playlistName.trim().isEmpty()) {
					try {
						initialMPDPlaylist.savePlaylist(playlistName);
					    currentPlaylistName = playlistName;
					} 
					catch (MPDResponseException e) {
						// does the play list already saved on the server?  
						// If so, delete and re-save
						if (e.getMessage().contains("Playlist already exists")) {
							try {
								int overwrite = JOptionPane.showConfirmDialog(null,
								    "Playlist already exists. Overwrite?",
								    "Overwrite existing playlist?",
								    JOptionPane.YES_NO_OPTION);
								if (overwrite == JOptionPane.YES_OPTION) {
									MPDPlaylist pl = library.getConnection().getMPDPlaylist();
									pl.deletePlaylist(playlistName);
									initialMPDPlaylist.savePlaylist(playlistName);
								    currentPlaylistName = playlistName;
								}
								else {
									btnSave.doClick();
								}
							}
							catch (Exception ex) {
								ex.printStackTrace();
							}
						}
						else {	
							e.printStackTrace();
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}	
				else
					JOptionPane.showMessageDialog(null, "Playlist not saved");
			}
		});
		GridBagConstraints gbc_btnSave = new GridBagConstraints();
		gbc_btnSave.insets = new Insets(0, 0, 5, 0);
		gbc_btnSave.gridx = 0;
		gbc_btnSave.gridy = 2;
		pnlPlaylistButtons.add(btnSave, gbc_btnSave);
		GridBagConstraints gbc_btnClear = new GridBagConstraints();
		gbc_btnClear.insets = new Insets(0, 0, 5, 0);
		gbc_btnClear.gridx = 0;
		gbc_btnClear.gridy = 4;
		pnlPlaylistButtons.add(btnClear, gbc_btnClear);
		
		JPanel pnlSettings = new JPanel();
		tabbedPaneMain.addTab("Settings", null, pnlSettings, null);
		GridBagLayout gbl_pnlSettings = new GridBagLayout();
		gbl_pnlSettings.columnWidths = new int[]{187, 417, 0};
		gbl_pnlSettings.rowHeights = new int[]{20, 20, 20, 23, 0, 0, 0};
		gbl_pnlSettings.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_pnlSettings.rowWeights = new double[]{0.0, 0.0, 0.0, 1.0, 1.0, 0.0, Double.MIN_VALUE};
		pnlSettings.setLayout(gbl_pnlSettings);
		
		JLabel lblServer = new JLabel("Server:");
		lblServer.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_lblServer = new GridBagConstraints();
		gbc_lblServer.anchor = GridBagConstraints.EAST;
		gbc_lblServer.insets = new Insets(0, 0, 5, 5);
		gbc_lblServer.gridx = 0;
		gbc_lblServer.gridy = 0;
		pnlSettings.add(lblServer, gbc_lblServer);
		
		txtServer = new JTextField();
		GridBagConstraints gbc_txtServer = new GridBagConstraints();
		gbc_txtServer.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtServer.insets = new Insets(0, 0, 5, 0);
		gbc_txtServer.gridx = 1;
		gbc_txtServer.gridy = 0;
		pnlSettings.add(txtServer, gbc_txtServer);
		txtServer.setColumns(20);
		
		JLabel lblPort = new JLabel("Port:");
		GridBagConstraints gbc_lblPort = new GridBagConstraints();
		gbc_lblPort.anchor = GridBagConstraints.EAST;
		gbc_lblPort.insets = new Insets(0, 0, 5, 5);
		gbc_lblPort.gridx = 0;
		gbc_lblPort.gridy = 1;
		pnlSettings.add(lblPort, gbc_lblPort);
		
		txtPort = new JTextField();
		GridBagConstraints gbc_txtPort = new GridBagConstraints();
		gbc_txtPort.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtPort.insets = new Insets(0, 0, 5, 0);
		gbc_txtPort.gridx = 1;
		gbc_txtPort.gridy = 1;
		pnlSettings.add(txtPort, gbc_txtPort);
		txtPort.setColumns(20);
		
		JLabel lblPassword = new JLabel("Password:");
		GridBagConstraints gbc_lblPassword = new GridBagConstraints();
		gbc_lblPassword.anchor = GridBagConstraints.EAST;
		gbc_lblPassword.insets = new Insets(0, 0, 5, 5);
		gbc_lblPassword.gridx = 0;
		gbc_lblPassword.gridy = 2;
		pnlSettings.add(lblPassword, gbc_lblPassword);
		
		txtPassword = new JTextField();
		GridBagConstraints gbc_txtPassword = new GridBagConstraints();
		gbc_txtPassword.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtPassword.insets = new Insets(0, 0, 5, 0);
		gbc_txtPassword.gridx = 1;
		gbc_txtPassword.gridy = 2;
		pnlSettings.add(txtPassword, gbc_txtPassword);
		txtPassword.setColumns(20);
		
		JPanel pnlConnectButton = new JPanel();
		GridBagConstraints gbc_pnlConnectButton = new GridBagConstraints();
		gbc_pnlConnectButton.fill = GridBagConstraints.BOTH;
		gbc_pnlConnectButton.insets = new Insets(0, 0, 5, 0);
		gbc_pnlConnectButton.gridx = 1;
		gbc_pnlConnectButton.gridy = 3;
		pnlSettings.add(pnlConnectButton, gbc_pnlConnectButton);
		GridBagLayout gbl_pnlConnectButton = new GridBagLayout();
		gbl_pnlConnectButton.columnWidths = new int[]{237, 237, 0};
		gbl_pnlConnectButton.rowHeights = new int[]{23, 0};
		gbl_pnlConnectButton.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_pnlConnectButton.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		pnlConnectButton.setLayout(gbl_pnlConnectButton);
		
		JButton btnConnect = new JButton("Connect");
		GridBagConstraints gbc_btnConnect = new GridBagConstraints();
		gbc_btnConnect.anchor = GridBagConstraints.WEST;
		gbc_btnConnect.fill = GridBagConstraints.VERTICAL;
		gbc_btnConnect.insets = new Insets(0, 0, 0, 5);
		gbc_btnConnect.gridx = 0;
		gbc_btnConnect.gridy = 0;
		pnlConnectButton.add(btnConnect, gbc_btnConnect);
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				options.setServer(txtServer.getText().trim());
				options.setPort(Integer.parseInt(txtPort.getText().trim()));
				options.setPassword(txtPassword.getText().trim());
				attemptConnection(true);
			}
		});
		
		lblStatus = new JLabel("");
		GridBagConstraints gbc_lblStatus = new GridBagConstraints();
		gbc_lblStatus.anchor = GridBagConstraints.WEST;
		gbc_lblStatus.fill = GridBagConstraints.VERTICAL;
		gbc_lblStatus.gridx = 1;
		gbc_lblStatus.gridy = 0;
		pnlConnectButton.add(lblStatus, gbc_lblStatus);
		
		btnUpdateServerDatabase = new JButton("Update Server Database");
		btnUpdateServerDatabase.setToolTipText("Update MPD music server database");
		btnUpdateServerDatabase.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				library.updateServerDatabase();
			}
		});
		
		JLabel lblOutputs = new JLabel("Outputs");
		GridBagConstraints gbc_lblOutputs = new GridBagConstraints();
		gbc_lblOutputs.anchor = GridBagConstraints.EAST;
		gbc_lblOutputs.insets = new Insets(0, 0, 5, 5);
		gbc_lblOutputs.gridx = 0;
		gbc_lblOutputs.gridy = 4;
		pnlSettings.add(lblOutputs, gbc_lblOutputs);
		
		scrollPaneOutputs = new JScrollPane();
		GridBagConstraints gbc_scrollPaneOutputs = new GridBagConstraints();
		gbc_scrollPaneOutputs.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneOutputs.insets = new Insets(0, 0, 5, 0);
		gbc_scrollPaneOutputs.gridx = 1;
		gbc_scrollPaneOutputs.gridy = 4;
		pnlSettings.add(scrollPaneOutputs, gbc_scrollPaneOutputs);
		
		tblOutputs = new JTable();
		scrollPaneOutputs.setViewportView(tblOutputs);
		btnUpdateServerDatabase.setEnabled(false);
		GridBagConstraints gbc_btnUpdateServerDatabase = new GridBagConstraints();
		gbc_btnUpdateServerDatabase.anchor = GridBagConstraints.SOUTH;
		gbc_btnUpdateServerDatabase.gridx = 1;
		gbc_btnUpdateServerDatabase.gridy = 5;
		pnlSettings.add(btnUpdateServerDatabase, gbc_btnUpdateServerDatabase);
	}

}