package server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

import javax.swing.JOptionPane;

public class TCPEchoServer {

	private ServerSocket welcomeSocket;
	private ServerInfo info;
	
	
	public TCPEchoServer(ServerSocket welcomeSocket, ServerInfo info) {
		this.welcomeSocket = welcomeSocket;
		this.info = info;
	}

	public void process() {

		try {
			System.out.println("SERVER ACCEPTING");
			// sitter her og venter.
			Socket connectionSocket = welcomeSocket.accept();

			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			String text = inFromClient.readLine();
			System.out.println("SERVER RECEIVED: " + text);
			String outtext = understandRequest(text);

			DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

			int n = 0;
			do {

				System.out.println("SERVER SENDING: " + outtext);

				outToClient.write(outtext.getBytes());
				outToClient.flush();

				n = 1;
			} while (n == 0);

			outToClient.close();
			inFromClient.close();

			connectionSocket.close();

		} catch (IOException e) {
			// Dette skjer om man lukker connection til serveren
			System.out.println("TCPServer: " + e.getMessage());

		}
	}

	/**
	 * take the first word and run the rest to its responsible function. Like SQL.
	 * 
	 * JOIN#name+id#host-boolean
	 * LEAVE#name+id 
	 * CLOSE 
	 * UPDATELOBBY#name+id#ready
	 * UPDATERACE#name+id#mysitsh
	 * 
	 * @param text input from client
	 * @return answer based upon request
	 */
	public String understandRequest(String request) {
		String[] input = request.split("#");

		switch (input[0]) {
		
		case "JOIN":
			return join(input);
		case "LEAVE":
			return leave(input);
		case "CLOSE":
			return join(input);
		case "UPDATELOBBY":
			return updateLobby(input);
		case "UPDATERACE":
			return join(input);

		}

		return null;
	}

	private String join(String[] input) {
		return info.joinLobby(input);
	}
	
	private String leave(String[] input) {
		
		
		return null;
	}
	
	private String updateLobby(String[] input) {
		
		
		return null;
	}
	
	
	
	
	/*
	 * Getters and setters
	 */

	public ServerSocket getWelcomeSocket() {
		return welcomeSocket;
	}

	public void setWelcomeSocket(ServerSocket welcomeSocket) {
		this.welcomeSocket = welcomeSocket;
	}

}
