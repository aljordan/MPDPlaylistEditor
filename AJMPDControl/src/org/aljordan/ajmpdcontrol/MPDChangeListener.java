package org.aljordan.ajmpdcontrol;

import org.bff.javampd.MPD;
import org.bff.javampd.events.PlayerBasicChangeEvent;
import org.bff.javampd.events.PlayerBasicChangeListener;
import org.bff.javampd.events.PlaylistBasicChangeEvent;
import org.bff.javampd.events.PlaylistBasicChangeListener;
import org.bff.javampd.events.TrackPositionChangeEvent;
import org.bff.javampd.events.TrackPositionChangeListener;
import org.bff.javampd.events.VolumeChangeEvent;
import org.bff.javampd.events.VolumeChangeListener;
import org.bff.javampd.monitor.MPDStandAloneMonitor;



public class MPDChangeListener implements TrackPositionChangeListener,
PlayerBasicChangeListener, PlaylistBasicChangeListener, VolumeChangeListener {
	
	private Thread th;
	private Main mainProgramUI;
	private MPDStandAloneMonitor mpdStandAloneMonitor;
    
	public MPDChangeListener(MPD mpd, Main mainUserInterface) {
		mainProgramUI = mainUserInterface;
        mpdStandAloneMonitor = new MPDStandAloneMonitor(mpd,1000);
        mpdStandAloneMonitor.addTrackPositionChangeListener(this);
        mpdStandAloneMonitor.addPlaylistChangeListener(this);
        mpdStandAloneMonitor.addPlayerChangeListener(this);
        mpdStandAloneMonitor.addVolumeChangeListener(this);
        th = new Thread(mpdStandAloneMonitor,"Stand_Alone_Monitor_Thread");
        th.start();
    }

	public void stop() {
		mpdStandAloneMonitor.removePlayerChangeListener(this);
		mpdStandAloneMonitor.removePlaylistStatusChangedListener(this);
		mpdStandAloneMonitor.stop();
	}
	
	@Override
	public void volumeChanged(VolumeChangeEvent event) {
		mainProgramUI.setVolumeSlider(event.getVolume());
	}
	
	@Override
	public void playlistBasicChange(PlaylistBasicChangeEvent event) {
        //System.out.println("Playlist event received:"+event.getId());
        switch(event.getId()) {
            case(PlaylistBasicChangeEvent.SONG_ADDED):
            	mainProgramUI.updatePlayingNowList();
            	break;
            case(PlaylistBasicChangeEvent.SONG_CHANGED):
            	mainProgramUI.hiliteCurrentSong();
            	mainProgramUI.resetLooping();
                break;
            case(PlaylistBasicChangeEvent.SONG_DELETED):
            	mainProgramUI.updatePlayingNowList();
                break;
            case(PlaylistBasicChangeEvent.PLAYLIST_ENDED):
                mainProgramUI.setPlayPauseButtons();
                break;
            case(PlaylistBasicChangeEvent.PLAYLIST_CHANGED):
            	mainProgramUI.updatePlayingNowList();
                break;

        }		
	}

	@Override
	public void playerBasicChange(PlayerBasicChangeEvent event) {
        switch(event.getId()) {
            case(PlayerBasicChangeEvent.PLAYER_STARTED):
            case(PlayerBasicChangeEvent.PLAYER_STOPPED):
            	mainProgramUI.setPlayPauseButtons();
            	break;
            case(PlayerBasicChangeEvent.PLAYER_PAUSED):
            case(PlayerBasicChangeEvent.PLAYER_UNPAUSED):
            	mainProgramUI.setPauseButtons();
                break;
             case(PlayerBasicChangeEvent.PLAYER_BITRATE_CHANGE):
            	mainProgramUI.updateBitRateInfo();
    			break;
        }		
	}

	@Override
	public void trackPositionChanged(TrackPositionChangeEvent event) {
		mainProgramUI.checkLoop();
		mainProgramUI.updateSongProgress((int) event.getElapsedTime());
	}

}
