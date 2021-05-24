package app;

import java.io.Serializable;

/**
 * This is an immutable class that holds all the information for a servent.
 *
 * @author bmilojkovic
 */
public class ServentInfo implements Serializable {

	private static final long serialVersionUID = 5304170042791281555L;
	private final String ipAddress;
	private final int listenerPort;
	private final String team;
	private final int chordId;
	
	public ServentInfo(String ipAddress, int listenerPort, String team) {
		this.ipAddress = ipAddress;
		this.listenerPort = listenerPort;
		this.team = team;

		this.chordId = ChordState.chordHash(listenerPort); // hash(ip:port) //TODO: Ne radi
		//hash(tim) ++ hash(ip:port)
		// hash(tim:(ip:port))
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public int getListenerPort() {
		return listenerPort;
	}

	public int getChordId() {
		return chordId;
	}
	
	@Override
	public String toString() {
		return "[" + chordId + "|" + ipAddress + "|" + listenerPort + "|" + team + "]";
	}

	public String getTeam() {
		return team;
	}
}
