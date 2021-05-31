package app;

import java.io.Serializable;
import java.util.Objects;

import servent.message.NetworkLocation;

/**
 * This is an immutable class that holds all the information for a servent.
 *
 * @author bmilojkovic
 */
public class ServentInfo implements Serializable {

	private static final long serialVersionUID = 5304170042791281555L;
	private final NetworkLocation networkLocation;
	private final String team;
	private final int chordId;
	
	public ServentInfo(String ipAddress, int listenerPort, String team) {
		this.networkLocation = new NetworkLocation(ipAddress, listenerPort);
		this.team = team;

//		if(team.equals("UNKNOWN")) { //Just in case when the first message is sent and we don't know the ID of receiver
//			this.chordId = -1;
//		}
//		else {
			this.chordId = ChordState.hashForFilePath(team + ipAddress + listenerPort);
//		}
		//hash(tim) ++ hash(ip:port)
		// hash(tim:(ip:port))
	}

	public NetworkLocation getNetworkLocation() {
		return networkLocation;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ServentInfo that = (ServentInfo) o;
		return chordId == that.chordId && networkLocation.equals(that.networkLocation) && team.equals(that.team);
	}

	@Override
	public int hashCode() {
		return Objects.hash(networkLocation, team, chordId);
	}

	public int getChordId() {
		return chordId;
	}
	
	@Override
	public String toString() {
		return "[" + chordId + "(" + team + ")" + "|" + networkLocation + "]";
	}

	public String getTeam() {
		return team;
	}
}
