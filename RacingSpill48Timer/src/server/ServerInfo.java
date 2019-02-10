package server;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Holds info about who is a part of this game. Also holds info about the cars when racing.
 * @author jonah
 *
 */

public class ServerInfo {

	private HashMap<String, PlayerInfo> players;
	
	public ServerInfo() {
		players = new HashMap<String, PlayerInfo>();
	}
	
	/**
	 * input 1 = name + id
	 * input 2 = host boolean
	 */
	
	public String joinLobby(String[] input) {
		
		PlayerInfo newPlayer = new PlayerInfo(input[1], input[2]);
		
		players.put(input[1], newPlayer);
		
		return updateLobby(newPlayer);
	}
	
	
	/**
	 * @return name#ready#car#... 
	 */
	public String updateLobby(PlayerInfo player) {
		String result = "";
		
		for (Entry<String, PlayerInfo> entry : players.entrySet()) 
		{ 
			result += "#" + entry.getValue().getLobbyInfo();
		}
		
		return result;
	}
	
	/**
	 * input 1 = name + id
	 * input 2 = sitsh
	 * 
	 * @return name#ready#car#... 
	 */
	public String updateLobby(String[] input) {
		
		PlayerInfo player = players.get(input[1]);
		player.updateLobby(input);
		
		return updateLobby(player);
	}
	
	public void leave(String[] input) {
		players.remove(input[1]);
	}
	
}