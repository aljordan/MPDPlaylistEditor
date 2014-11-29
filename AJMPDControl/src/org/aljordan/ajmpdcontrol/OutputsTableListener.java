package org.aljordan.ajmpdcontrol;

import java.util.Collection;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.bff.javampd.MPDAdmin;
import org.bff.javampd.MPDOutput;
import org.bff.javampd.exception.MPDConnectionException;
import org.bff.javampd.exception.MPDResponseException;

public class OutputsTableListener implements TableModelListener {
	private JavaMPDWrapper library; 
	
	public OutputsTableListener(JavaMPDWrapper library) {
		this.library = library;
	}

	@Override
	public void tableChanged(TableModelEvent e) {
		int row = e.getFirstRow();
        int column = e.getColumn();
        TableModel model = (TableModel)e.getSource();
        Object data = model.getValueAt(row, column);		
       	System.out.println("row: " + String.valueOf(row));
       	System.out.println(data.toString());
       	
       	//get proper output from collection
		Collection<MPDOutput> outputs;
		try {
			MPDAdmin admin = library.getConnection().getMPDAdmin();
			outputs = admin.getOutputs();
	       	Object[] arrOutputs = outputs.toArray();
	       	MPDOutput output = (MPDOutput)arrOutputs[row];
	       	//toggle output
	       	if (Boolean.valueOf(data.toString())) {
	       		output.setEnabled(true);
	       		admin.enableOutput(output);
	       	}
	       	else {
	       		output.setEnabled(false);
	       		admin.disableOutput(output);
	       	}

		} catch (MPDConnectionException e1) {
			e1.printStackTrace();
		} catch (MPDResponseException e1) {
			e1.printStackTrace();
		}       	
	}

}
