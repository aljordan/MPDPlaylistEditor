package org.aljordan.ajmpdcontrol;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.DefaultListModel;
import javax.swing.JList;

import org.bff.javampd.MPDPlaylist;
import org.bff.javampd.exception.MPDConnectionException;
import org.bff.javampd.exception.MPDPlaylistException;

class ReorderListener extends MouseAdapter {

	private JList<Object> list;
	private int pressIndex = 0;
	private int releaseIndex = 0;
	private MPDPlaylist playlist;

	public ReorderListener(JList<Object> list, MPDPlaylist pList) {
		if (!(list.getModel() instanceof DefaultListModel)) {
			throw new IllegalArgumentException(
					"List must have a DefaultListModel");
		}
		this.list = list;
		this.playlist = pList;
	}

	@Override
	public void mousePressed(MouseEvent e) {
		pressIndex = list.locationToIndex(e.getPoint());
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		releaseIndex = list.locationToIndex(e.getPoint());
		if (releaseIndex != pressIndex && releaseIndex != -1) {
			reorder();
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		mouseReleased(e);
		pressIndex = releaseIndex;
	}

	private void reorder() {
		DefaultListModel<Object> model = (DefaultListModel<Object>) list.getModel();
		Object dragee = model.elementAt(pressIndex);
		model.removeElementAt(pressIndex);
		model.insertElementAt(dragee, releaseIndex);
		try {
			playlist.move(playlist.getSongList().get(pressIndex), releaseIndex);
		} catch (MPDPlaylistException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MPDConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
