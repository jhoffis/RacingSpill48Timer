package server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import elem.AI;
import startup.Main;

/**
 * Holds info about who is a part of this game. Also holds info about the cars
 * when racing.
 * 
 * På siste race så blir det ikke startet fordi rett etter at alle har komt inn
 * så sier serveren at det racet er "ferdig". Det er riktig, men det mistolkes
 * som en fullstedig ferdig game på siste race.
 * 
 * @author jonah
 *
 */

public class ServerInfo implements Runnable {

	private HashMap<String, PlayerInfo> players;
	private ArrayList<AI> ai;
	// TODO
	private HashMap<String, PlayerInfo> lostPlayers;
	private ConcurrentHashMap <String, Long> ping;
	private HashMap<PlayerInfo, Queue<String>> chat;
	private int started;
	private int amountFinished;
	private int length;
	private int races;
	private int raceLights;
	private long raceStartedTime;
	private final long waitTime = 1000;
	private long regulatingWaitTime = -1;
	private boolean running = true;
	private boolean greenLights;
	private boolean allFinished;
	private Random r;
	private int amountInTheRace;
	private String raceLobbyString;
	private boolean raceLobbyStringFinalized;
	private boolean leavingPlayerMutex;
	private Thread finishAI_thread;
	private String res;
	private ArrayList<PlayerInfo> winners;
	private String[] places;
	private String currentPlace;

	public ServerInfo(int amountOfAI, int diff) {
		players = new HashMap<String, PlayerInfo>();
		ping = new ConcurrentHashMap<String, Long>();
		chat = new HashMap<PlayerInfo, Queue<String>>();
		ai = new ArrayList<AI>();

		for (int i = 0; i < amountOfAI; i++) {
			AI ai = new AI(i, diff);
			this.ai.add(ai);
			players.put(ai.getName() + i, ai);
		}

		places = new String[4];
		places[0] = "Japan";
		places[1] = "America";
		places[2] = "Britain";
		places[3] = "Germany";
		
		r = new Random();
		races = -1;
		setRunning(true);
		
		prepareNextRace();
	}

	private int generateID() {
		return r.nextInt(1000);
	}

	public int getStarted() {
		return started;
	}

	public void setStarted(int started) {
		this.started = started;
	}

	/**
	 * input 1 = name input 2 = id input 3 = host boolean input 4 = carname
	 */

	public String joinLobby(String[] input) {

		PlayerInfo newPlayer = new PlayerInfo(input[1], input[2], input[3], input[4]);

		addChat(newPlayer.getName() + " joined the game.");

		players.put(input[1] + input[2], newPlayer);
		ping.put(input[1] + input[2], System.currentTimeMillis());
		chat.put(newPlayer, new ConcurrentLinkedQueue<String>());

		return updateLobby();
	}

	/**
	 * @return name#ready#car#...
	 */
	public String updateLobby() {
		String result = "";

		for (Entry<String, PlayerInfo> entry : players.entrySet()) {
			result += "#" + entry.getValue().getLobbyInfo() + "#" + started;
		}

		return result;
	}

	/**
	 * input 1 = name input 2 = id input 3 = sitsh
	 * 
	 * @return name#ready#car#...
	 */
	public String updateLobby(String[] input) {

		PlayerInfo player = getPlayer(input);
		if (player == null) {
			return null;
		}
		player.updateLobby(input);

		return updateLobby();
	}

	private PlayerInfo getPlayer(String[] input) {
		return getPlayer(input[1] + input[2]);
	}

	private PlayerInfo getPlayer(String input) {
		return players.get(input);
	}

	private void finishAI(AI player, long time) {
		player.setFinished(1);
		amountFinished++;
		player.setTime(time);
	}

	public void finishPlayer(String[] input) {
		PlayerInfo player = getPlayer(input);
		if (player == null) {
			return;
		}
		player.setFinished(1);
		amountFinished++;

		if (greenLights) {
			player.setTime(System.currentTimeMillis() - raceStartedTime);
		} else {
			player.setTime(-1);
			if (player.isIn() == false)
				inTheRace(input);
		}

		this.allFinished = amountFinished == players.size();
		
		if(isRaceOver()) {
			updateRace();
			setPlayerWithMostPoints();
		} else if (allFinished) {
			prepareNextRace();
		}
	}

	private void prepareNextRace() {
		length = randomizeLengthOfTrack();
		currentPlace = places[r.nextInt(places.length)];
	}

	private boolean isRaceOver() {
		return running = allFinished && races <= 0;
	}

	private void updateRaceStatus() {

//		if (started != 1)
//			return;

		greenLights = updateRaceLights();
		// Update time per player

	}

	private boolean updateRaceLights() {
		// Racelights green
		if (raceLights == 4)
			return true;

		// Everyone in the race
		if (amountInTheRace == players.size()) {

			started = 0;

			// Wait for 3 secounds before the race starts && wait for each racelight
			if (raceStartedTime + regulatingWaitTime < System.currentTimeMillis()) {
				regulatingWaitTime = waitTime - 300 + r.nextInt(1200);
				raceStartedTime = System.currentTimeMillis();
				raceLights++;
			}
		} else {
			raceStartedTime = System.currentTimeMillis();
		}

		// Racelights red
		return false;

	}

	public String getRaceLightsStatus() {
		return String.valueOf(raceLights);
	}

	private void stopRace() {
		amountInTheRace = 0;
		amountFinished = 0;
		started = 0;
	}

	/**
	 * input[2] -> 1 = race started. 0 = race ready to start
	 */
	public void startRace(String[] input) {
		// host?
		if (Integer.valueOf(input[1]) == 1) {
			if (Integer.valueOf(input[2]) == 1) {
				races--;

				for (Entry<String, PlayerInfo> entry : players.entrySet()) {
					entry.getValue().newRace();
				}

				raceStartedTime = System.currentTimeMillis();
				regulatingWaitTime = waitTime * 3;
				raceLobbyStringFinalized = false;

				amountInTheRace += ai.size();

				finishAI_thread = new Thread(() -> {
					for (AI ai : this.ai) {
						finishAI(ai, ai.calculateRace(length));
					}
				});
				finishAI_thread.start();

			} else {
				stopRace();
			}
			started = Integer.valueOf(input[2]);
			raceLights = 0;
		}
	}

	/**
	 * Creates a new racetrack somewhere in the world and with some length of some
	 * type.
	 * 
	 * @return length of the track
	 */
	public int randomizeLengthOfTrack() {
		return 500 * (r.nextInt(4) + 1) / (500 * (Main.DEBUG ? 1 : 0) + 1);
	}

	public void leave(String[] input) {
		leave(input[1] + input[2]);
	}

	private void leave(String nameid) {
		System.out.println("Making " + nameid + " leave");

		PlayerInfo player = getPlayer(nameid);
		if (player != null) {
			chat.remove(player);
			players.remove(nameid);
			ping.remove(nameid);
			addChat(player.getName() + " left the game.");
		}
	}

	public String getTrackLength() {
		return String.valueOf(length);
	}

	public void inTheRace(String[] input) {
		getPlayer(input).setIn(true);
		amountInTheRace++;
	}

	/**
	 * UPDATERACE#name#id#finished(0-1)#longtimemillis
	 * 
	 * F�rste gang f�r alle 10 andre gang f�r ingen poeng?
	 */
	public String updateRace() {

		// If racing, finished and is first time telling that it has finished
		if (allFinished) {

			for (Entry<String, PlayerInfo> entry : players.entrySet()) {
				PlayerInfo player = entry.getValue();
				int place = 0;
				long thisTime = player.getTime();

				if (thisTime == -1) {

					player.addPointsAndMoney(-1, -1);

				} else {

					for (Entry<String, PlayerInfo> otherEntry : players.entrySet()) {

						if (otherEntry.getKey() != entry.getKey()) {

							long otherTime = otherEntry.getValue().getTime();
							if (thisTime > otherTime && otherTime != -1) {
								place++;
							}
						}
					}

					player.addPointsAndMoney(players.size(), place);
				}
			}

			allFinished = false;
			raceLobbyString = updateRaceLobby(true);
			raceLobbyStringFinalized = true;
			
		} else if (!raceLobbyStringFinalized) {
			raceLobbyString = updateRaceLobby(false);
		}

		return raceLobbyString;
	}

	/**
	 * @return name#ready#car#...
	 */
	public String updateRaceLobby(boolean allFinished) {
		String result = "";

		if (!allFinished) {
			// Hent spillere i hvilken som helst rekkefølge og sett de inn i returnstrengen

			result += 3;

			for (Entry<String, PlayerInfo> entry : players.entrySet()) {
				result += "#" + entry.getValue().getRaceInfo(allFinished);
			}
		} else {

			result += 6;

			LinkedList<PlayerInfo> sortedByTime = new LinkedList<PlayerInfo>();

			// Sorter alle spillere etter alle har fullført racet
			sortedByTime.addAll(players.values());
			Collections.sort(sortedByTime, new Comparator<PlayerInfo>() {
				@Override
				public int compare(PlayerInfo o1, PlayerInfo o2) {

					int result = 0;

					if (o1.getTime() < o2.getTime()) {
						if (o1.getTime() != -1)
							result = -1;
						else
							result = 1;

					} else if (o1.getTime() > o2.getTime()) {
						if (o2.getTime() != -1)
							result = 1;
						else
							result = -1;
					}
					return result;
				}
			});

			// Legg de inn i strengen
			for (int i = 0; i < sortedByTime.size(); i++) {
				
				String str = null;
				if(sortedByTime.get(i).getClass().equals(AI.class)) {
					AI p = (AI) sortedByTime.get(i);
					str = p.getRaceInfo(allFinished);
				} else {
					str = sortedByTime.get(i).getRaceInfo(allFinished);
				}
				
				result += "#" + (i + 1) + ": " + str;
			}
		}

		return result;
	}

	public void setPointsMoney(String[] input) {
		PlayerInfo player = getPlayer(input);
		;
		if (player == null) {
			return;
		}
		player.setPoints(Integer.valueOf(input[3]));
		player.setMoney(Integer.valueOf(input[4]));
	}

	public String getPointsMoney(String[] input) {
		PlayerInfo player = getPlayer(input);

		String res = null;
		try {
			res = player.getPoints() + "#" + player.getMoney();
		} catch (NullPointerException e) {
			System.err.println("Player " + input[1] + input[2] + " timed out");
			checkPings();
		}
		return res;
	}

	public void newRaces() {
		if (!Main.DEBUG)
			races = 9;
		else
			races = 2;
	}

	public String getRacesLeft() {
		return String.valueOf(races);
	}

	private void setPlayerWithMostPoints() {
		winners = new ArrayList<PlayerInfo>();

		for (Entry<String, PlayerInfo> entry : players.entrySet()) {
			PlayerInfo other = entry.getValue();
			if (winners.size() == 0 || other.getPoints() == winners.get(0).getPoints()) {
				winners.add(other);
			} else if(other.getPoints() > winners.get(0).getPoints()) {
				winners.clear();
				winners.add(other);
			}
		}

		if(winners.size() == 1) {
			res = "And the winner is: " + winners.get(0).getName() + ", " + winners.get(0).getCarName();
		} else {
			res = "And the winners are: ";
			for(PlayerInfo player : winners) {
				res += "<br/>" + player.getName() + ", " + player.getCarName();
			}
		}
	}
	
	public String getPlayerWithMostPoints() {
		return res;
	}

	public void ping(String[] input) {
		ping.put(input[1] + input[2], System.currentTimeMillis());
	}

	public boolean validPing(long ping) {
		return ping > System.currentTimeMillis() - 20000;
	}

	public void checkPings() {
		if (!leavingPlayerMutex)
			for (Entry<String, Long> entry : ping.entrySet()) {
				PlayerInfo player = getPlayer(entry.getKey());

				if (!ai.contains(player) && !validPing(entry.getValue())) {
					leavingPlayerMutex = true;
					if (player == null)
						return;

					addChat(player.getName() + " has too high ping!");
					leave(entry.getKey());
					leavingPlayerMutex = false;
				}
			}
	}

	@Override
	public void run() {

		long lastTime = System.nanoTime();
		double amountOfTicks = 5.0;
		double ns = 1000000000 / amountOfTicks;
		double delta = 0;

		while (isRunning()) {
			long now = System.nanoTime();
			delta += (now - lastTime) / ns;
			lastTime = now;
			while (delta >= 1) {
				if (!Main.DEBUG && !leavingPlayerMutex)
					checkPings();
				updateRaceStatus();
				delta--;
			}

		}
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	private void addChat(String str) {
		for (Queue<String> q : chat.values()) {
			q.offer(str);
		}
	}

	public void addChat(String[] input) {
		if (input.length <= 2)
			return;

		String str = input[1] + ": ";
		for (int i = 2; i < input.length; i++) {
			str += input[i];
			if (i + 1 < input.length)
				str += "#";
		}

		for (Queue<String> q : chat.values()) {
			q.offer(str);
		}

	}

	public String getChat(String[] input) {
		return chat.get(getPlayer(input)).poll();
	}

	public String getCurrentPlace() {
		return currentPlace;
	}

	public void setCurrentPlace(String currentPlace) {
		this.currentPlace = currentPlace;
	}

}
