package scenes;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import adt.Scene;
import elem.Player;
import handlers.SceneHandler;
import handlers.ServerHandler;

public class Lobby extends Scene implements Runnable {

	private static final long serialVersionUID = -5861049279182231248L;
	private JLabel label;
	private JButton ready;
	private JButton fixCar;
	private JButton start;
	private JButton goBack;
	private JButton options;
	private Player player;
	private boolean running;
	private ServerHandler server;
	private JScrollPane scrollPane;
	private boolean everyoneReady;
	private Race race;
	private FixCar fixCarScene;
	private Thread thread;
	private boolean gameEnded;
	private Thread lobbyThread;
	private boolean started;
	private JLabel ipLabel;

	public Lobby(Race race, FixCar fixCarScene) {

		// Init shit
		this.race = race;
		this.fixCarScene = fixCarScene;
		label = new JLabel("HELLLLLLLLLLLLLLLLOOOOO?!", SwingConstants.CENTER);
		ready = new JButton("Ready?");
		fixCar = new JButton("Upgrade or fix my car");
		start = new JButton("Start race");
		goBack = new JButton("Go back to main menu");
		options = new JButton("Options");
		scrollPane = new JScrollPane(label);
		scrollPane.setPreferredSize(new Dimension(500, 300));
		ipLabel = new JLabel("IPs: ");

		String ipLabelText = "<html> IPs: ";
		try {
			Enumeration enumIP = NetworkInterface.getNetworkInterfaces();

			while (enumIP.hasMoreElements()) {
				NetworkInterface n = (NetworkInterface) enumIP.nextElement();
				Enumeration ee = n.getInetAddresses();
				while (ee.hasMoreElements()) {
					InetAddress i = (InetAddress) ee.nextElement();
					ipLabelText += "<br/>" + i.getHostAddress();
				}
			}
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		ipLabelText += "</html>";
		ipLabel.setText(ipLabelText);
		
		// ActionListeners
		ready.addActionListener((ActionEvent e) -> player.setReady((player.getReady() + 1) % 2));
		fixCar.addActionListener((ActionEvent e) -> {
			SceneHandler.instance.changeScene(2);
			fixCarScene.init(player);
		});
		goBack.addActionListener((ActionEvent e) -> {
			if (server != null) {
				server.join();
				server = null;
			}
			player.leaveServer();
			SceneHandler.instance.changeScene(0);
		});

		start.addActionListener((ActionEvent e) -> {
			if (everyoneReady) {
				// start the race
				player.startRace();
			}
		});

		options.addActionListener((ActionEvent e) -> SceneHandler.instance.changeScene(4));

		// Add to JPanel
		add(scrollPane);
		add(goBack, BorderLayout.SOUTH);
		add(options, BorderLayout.SOUTH);
		add(ready, BorderLayout.SOUTH);
		add(fixCar, BorderLayout.SOUTH);
		add(start, BorderLayout.SOUTH);
		add(ipLabel);
	}

	/**
	 * 
	 * @param string - outtext from server
	 */
	public void update(String string) {
		if(string == null)
			return;

		everyoneReady = true;
		String[] outputs = string.split("#");

		int racesLeft = Integer.valueOf(player.getRacesLeft());

		String result = "<html> Races left: " + racesLeft + "<br/><br/>" + "Players: <br/>";
		int n = 0;
		for (int i = 1; i < outputs.length; i++) {
			n++;

			switch (n) {

			case 1:
				result += outputs[i] + ", ";
				break;
			case 2:
				if (Integer.valueOf(outputs[i]) == 1) {
					result += "Ready, ";
				} else {
					result += "Not ready, ";
					everyoneReady = false;
				}
				break;
			case 3:
				if (Integer.valueOf(outputs[i]) == 1)
					result += "Host, ";
				break;
			case 4:
				result += outputs[i] + ", ";
				break;

			case 5:
				result += "Points: " + outputs[i] + "<br/>";
				break;
			case 6:
				if (Integer.valueOf(outputs[i]) == 1) {
					raceStarted();
				}
				n = 0;
				break;
			}

		}
		result += "</html>";

		if (racesLeft == 0 && Integer.valueOf(outputs[6]) != 1) {
			endGame();
			return;
		}
		// Show all players on screen
		label.setText(result);

		// Disable start game button
		if (everyoneReady)
			start.setEnabled(true);
		else
			start.setEnabled(false);

		// Disable fix car button
		if (player.getReady() == 1)
			fixCar.setEnabled(false);
		else
			fixCar.setEnabled(true);

	}

	private void endGame() {
		player.getCar().reset();
		gameEnded = true;
		start.setEnabled(false);
		fixCar.setEnabled(false);
		ready.setEnabled(false);
		label.setText("And the winner is: " + player.getWinner());
		player.leaveServer();
	}

	/**
	 * @param name - username
	 * @param host - int value (0,1) represents boolean
	 */
	public void createNewLobby(String name, int host, String car, ServerHandler server, Thread lobbyThread) {
		this.lobbyThread = lobbyThread;
		gameEnded = false;
		player = new Player(name, host, car);
		player.createNewRaces();
		update(player.joinServer());
		initButtonState();
		this.server = server;
		SceneHandler.instance.addClosingListener(player);
	}

	/**
	 * @param name - username
	 * @param host - int value (0,1) represents boolean
	 * @param ip
	 */
	public void joinNewLobby(String name, int host, String car, String ip, Thread lobbyThread) {
		this.lobbyThread = lobbyThread;
		gameEnded = false;
		player = new Player(name, host, car, ip);
		update(player.joinServer());
		initButtonState();
		SceneHandler.instance.addClosingListener(player);
	}

	/**
	 * GUI for player
	 */
	private void initButtonState() {

		if (player.isHost()) {
			start.setVisible(true);
			ipLabel.setVisible(true);
		}
		else {
			start.setVisible(false);
			ipLabel.setVisible(false);
		}
		
	}

	private void raceStarted() {
		if (!started) {
			started = true;
			SceneHandler.instance.changeScene(3);
			race.setPlayer(player);
			race.setLobby(this);
			race.setLobbyThread(lobbyThread);
			race.setCurrentLength();
			player.setReady(0);
			player.updateLobbyFromServer();
			thread = new Thread(race);
			System.err.println("starting next thread");
			thread.start();
		}
	}

	/**
	 * Merely to update lobby 5 per second
	 */
	@Override
	public void run() {
		started = false;
		long lastTime = System.nanoTime();
		double amountOfTicks = 5.0;
		double ns = 1000000000 / amountOfTicks;
		double delta = 0;
		long timer = System.currentTimeMillis();
		int frames = 0;
		while ((SceneHandler.instance.getCurrentScene().getClass().equals(Lobby.class)
				|| SceneHandler.instance.getCurrentScene().getClass().equals(FixCar.class)
				|| SceneHandler.instance.getCurrentScene().getClass().equals(Options.class)
				|| (SceneHandler.instance.getCurrentScene().getClass().equals(Race.class) && !started)) && !gameEnded) {
			long now = System.nanoTime();
			delta += (now - lastTime) / ns;
			lastTime = now;
			while (delta >= 1) {
				
				player.pingServer();
				
				if (!gameEnded && !started)
					update(player.updateLobbyFromServer());
				delta--;
				frames++;
			}

			if (System.currentTimeMillis() - timer > 1000) {
				timer += 1000;
				System.out.println("FPS: " + frames);
				frames = 0;
			}
		}

	}

	/*
	 * Getters and setters
	 */
	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	public boolean isStarted() {
		return started;
	}

	public void setStarted(boolean started) {
		this.started = started;
	}
}
