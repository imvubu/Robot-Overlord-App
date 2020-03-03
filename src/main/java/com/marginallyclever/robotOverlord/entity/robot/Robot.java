package com.marginallyclever.robotOverlord.entity.robot;

import java.util.ArrayList;

import javax.swing.JPanel;

import com.marginallyclever.communications.NetworkConnectionManager;
import com.marginallyclever.convenience.AnsiColors;
import com.jogamp.opengl.GL2;
import com.marginallyclever.communications.NetworkConnection;
import com.marginallyclever.communications.NetworkConnectionListener;
import com.marginallyclever.robotOverlord.RobotOverlord;
import com.marginallyclever.robotOverlord.entity.physicalObject.PhysicalObject;


/**
 * A robot visible with a physical presence in the World.  Assumed to have an NetworkConnection to a machine in real life.  
 * @author Dan Royer
 *
 */
public abstract class Robot extends PhysicalObject implements NetworkConnectionListener {
	// comms	
	protected transient NetworkConnection connection;
	protected transient boolean isReadyToReceive;

	// sending file to the robot
	private boolean running;
	private boolean paused;
	private long linesProcessed;
	private ArrayList<String> gcode;

	protected transient boolean isModelLoaded;
	
	protected transient RobotPanel robotPanel=null;
	
	
	public Robot() {
		super();
		isReadyToReceive=false;
		linesProcessed=0;
		paused=true;
		running=false;
		isModelLoaded=false;
	}
	

	public boolean isRunning() { return running; }
	public boolean isPaused() { return paused; }
	
	
	@Override
	public ArrayList<JPanel> getContextPanel(RobotOverlord gui) {
		ArrayList<JPanel> list = super.getContextPanel(gui);
		if(robotPanel == null) robotPanel = new RobotPanel(gui,this);
		list.add(robotPanel);
		
		return list;
	}
	
	public void closeConnection() {
		connection.closeConnection();
		connection.removeListener(this);
		connection=null;
	}
	
	public void openConnection() {
		NetworkConnection s = NetworkConnectionManager.requestNewConnection(null);
		if(s!=null) {
			connection = s;
			connection.addListener(this);
		}
	}
	
	
	public NetworkConnection getConnection() {
		return this.connection;
	}
	
	
	@Override
	public void dataAvailable(NetworkConnection arg0,String data) {
		if(arg0==connection && connection!=null) {
			if(data.startsWith(">")) {
				isReadyToReceive=true;
			}
		}
		
		if(isReadyToReceive) {
			sendFileCommand();
		}
		
		System.out.print(AnsiColors.GREEN+data+AnsiColors.RESET);
	}
	
	
	
	/**
	 * Take the next line from the file and send it to the robot, if permitted. 
	 */
	public void sendFileCommand() {
		if(!running || paused || linesProcessed>=gcode.size()) return;
		
		String line;
		do {			
			// are there any more commands?
			line=gcode.get((int)linesProcessed++).trim();
			//previewPane.setLinesProcessed(linesProcessed);
			//statusBar.SetProgress(linesProcessed, gcode.size());
			// loop until we find a line that gets sent to the robot, at which point we'll
			// pause for the robot to respond.  Also stop at end of file.
		} while(!sendCommand(line) && linesProcessed<gcode.size());

		isReadyToReceive=false;
		
		if(linesProcessed==gcode.size()) {
			// end of file
			halt();
		}
	}

	
	/**
	 * Stop sending commands to the robot.
	 */
	public void halt() {
		// TODO add an e-stop command?
		running=false;
		paused=false;
	    linesProcessed=0;
	}

	public void start() {
		paused=false;
		running=true;
		sendFileCommand();
	}
	
	public void startAt(int lineNumber) {
		if(!running) {
			linesProcessed=lineNumber;
			start();
		}
	}
	
	public void pause() {
		if(running) {
			if(paused) {
				paused=false;
				// TODO: if the robot is not ready to unpause, this might fail and the program would appear to hang.
				sendFileCommand();
			} else {
				paused=true;
			}
		}
	}
	
	// Must be called by subclass to loadModels on render.
	@Override
	public void render(GL2 gl2) {
		if(!isModelLoaded) {
			loadModels(gl2);
			isModelLoaded=true;
		}
		super.render(gl2);
	}
	
	// stub to be overridden by subclasses.
	protected void loadModels(GL2 gl2) {}
	
	/**
	 * Processes a single instruction meant for the robot.
	 * @param line command to send
	 * @return true if the command is sent to the robot.
	 */
	public boolean sendCommand(String line) {
		if(connection==null) return false;

		// contains a comment?  if so remove it
		int index=line.indexOf('(');
		if(index!=-1) {
			//String comment=line.substring(index+1,line.lastIndexOf(')'));
			//Log("* "+comment+NL);
			line=line.substring(0,index).trim();
			if(line.length()==0) {
				// entire line was a comment.
				return false;  // still ready to send
			}
		}

		// send relevant part of line to the robot
		try{
			connection.sendMessage(line);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return true;
	}

	@Override
	public void lineError(NetworkConnection arg0, int lineNumber) {
		// back up to the line that had an error and send again.
	}

	@Override
	public void sendBufferEmpty(NetworkConnection arg0) {
		// just because the buffer is empty does not mean the robot is ready to receive. 
	}
/*
	// pull the last connected port from prefs
	private void loadRecentPortFromPreferences() {
		recentPort = prefs.get("recent-port", "");
	}

	// update the prefs with the last port connected and refreshes the menus.
	public void setRecentPort(String portName) {
		prefs.put("recent-port", portName);
		recentPort = portName;
		//UpdateMenuBar();
	}
*/

	/**
	 * Each robot implementation should customize the keframe as needed. 
	 * @return an instance derived from RobotKeyframe
	 */
	public abstract RobotKeyframe createKeyframe();
	
	public void updatePose() {}
	
	
	@Override
	public void update(double dt) {
		super.update(dt);
		if(connection!=null) {
			connection.update();
		}
	}
	
	public boolean isReadyToReceive() {
		return isReadyToReceive;
	}
}
