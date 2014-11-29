package org.aljordan.ajmpdcontrol;

import java.util.Collection;
import javax.swing.table.AbstractTableModel;
import org.bff.javampd.MPDOutput;
import org.bff.javampd.exception.MPDConnectionException;
import org.bff.javampd.exception.MPDResponseException;

public class OutputsTableModel extends AbstractTableModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private boolean DEBUG = false;
	private JavaMPDWrapper library;
    private String[] columnNames = {"MPD Output", "Enable"};

    private Object[][] data;
    
    public OutputsTableModel(JavaMPDWrapper library) {
    	this.library = library;
    	convertCollectionToTableData();
    }
    
    
    private void convertCollectionToTableData() {
		Collection<MPDOutput> outputs;
		try {
			outputs = library.getConnection().getMPDAdmin().getOutputs();
			Object[] arrayOutputs = outputs.toArray();
			// create two dimension array out of outputs to load table
			data = new Object[outputs.size()][2];
			for (int counter = 0; counter < arrayOutputs.length; counter++) {
				data[counter][0] = ((MPDOutput)arrayOutputs[counter]).getName();
				data[counter][1] = ((MPDOutput)arrayOutputs[counter]).isEnabled();
			}
		} catch (MPDConnectionException e) {
			e.printStackTrace();
		} catch (MPDResponseException e) {
			e.printStackTrace();
		}			
    }
    
    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        return data.length;
    }

    public String getColumnName(int col) {
        return columnNames[col];
    }

    public Object getValueAt(int row, int col) {
        return data[row][col];
    }

    /*
     * JTable uses this method to determine the default renderer/
     * editor for each cell.  If we didn't implement this method,
     * then the last column would contain text ("true"/"false"),
     * rather than a check box.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public Class getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    public boolean isCellEditable(int row, int col) {
        //Note that the data/cell address is constant,
        //no matter where the cell appears on screen.
        if (col < 1) {
            return false;
        } else {
            return true;
        }
    }

    /*
     * Don't need to implement this method unless your table's
     * data can change.
     */
    public void setValueAt(Object value, int row, int col) {
        if (DEBUG) {
            System.out.println("Setting value at " + row + "," + col
                               + " to " + value
                               + " (an instance of "
                               + value.getClass() + ")");
        }

        data[row][col] = value;
        fireTableCellUpdated(row, col);

        if (DEBUG) {
            System.out.println("New value of data:");
            printDebugData();
        }
    }

    private void printDebugData() {
        int numRows = getRowCount();
        int numCols = getColumnCount();

        for (int i=0; i < numRows; i++) {
            System.out.print("    row " + i + ":");
            for (int j=0; j < numCols; j++) {
                System.out.print("  " + data[i][j]);
            }
            System.out.println();
        }
        System.out.println("--------------------------");
    }
}
