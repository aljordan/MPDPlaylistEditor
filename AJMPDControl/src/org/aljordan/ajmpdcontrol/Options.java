package org.aljordan.ajmpdcontrol;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Options implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String server;
	private String password;
	private int port;
	
	public Options() {
        initOptions();
	}
	
	private void initOptions() {
        try {
            FileInputStream f_in = new FileInputStream("AJMPDControlOptions.data");
            ObjectInputStream obj_in = new ObjectInputStream (f_in);
            Object obj = obj_in.readObject();

            if (obj instanceof Options) {
                Options tempOptions = (Options)obj;
                this.server = tempOptions.getServer();
                this.port = tempOptions.getPort();
                this.password = tempOptions.getPassword();
            }
            //obj_in.close();
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println(e.getStackTrace());
            this.server = null;
            this.port = 6600;
            this.password = null;
        }		
	}
	
	
    public void saveOptions() {
        try {
            FileOutputStream f_out = new FileOutputStream("AJMPDControlOptions.data");
            ObjectOutputStream obj_out = new ObjectOutputStream (f_out);
            obj_out.writeObject(this);
            //obj_out.close();
        }
        catch (FileNotFoundException fe) {
            System.out.println(fe.getMessage());
            System.out.println(fe.getStackTrace());
        }
        catch (IOException ioe) {
            System.out.println(ioe.getMessage());
            System.out.println(ioe.getStackTrace());
        }
    }
	
	
	
	/**
	 * @return the MPD server
	 */
	public String getServer() {
		return server;
	}
	
	/**
	 * @param server the MPD server to set
	 */
	public void setServer(String server) {
		this.server = server;
	}
	
	/**
	 * @return the MPD server password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password the MPD server password to set if there is one
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the MPD server port
	 */
	public int getPort() {
		return port;
	}
	
	/**
	 * @param port the MPD server port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}
	
	

}
