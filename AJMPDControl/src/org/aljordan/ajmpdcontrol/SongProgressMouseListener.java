package org.aljordan.ajmpdcontrol;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JProgressBar;

public class SongProgressMouseListener implements MouseListener {
	private Main mainProgramUI;

	public SongProgressMouseListener(Main programUI) {
		this.mainProgramUI = programUI;
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		System.out.println("X is: " + String.valueOf(e.getX()));
		Component bar = e.getComponent();
		
		// get a percentage of the length of the progress bar where the
		// user clicked.
		float percentage = ((float)e.getX() / (float)bar.getWidth());
		
		// get the maximum if the progress bar (which is the length of the song in seconds)
		JProgressBar progBar = (JProgressBar)e.getSource();
		float songLenth = (float)progBar.getMaximum();
		
		mainProgramUI.moveToSongTime((long)(songLenth * percentage));
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

}
