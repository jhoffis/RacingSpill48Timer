package scenes;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.imageio.ImageIO;

import adt.Visual;
import adt.VisualElement;
import elem.PlacedAnimation;
import elem.Player;

public class FinishVisual extends Visual {

	private Race race;
	private Player player;

	private Queue<PlacedAnimation> finishedPlayers;
	private BufferedImage resBackground;
	private PlacedAnimation resCar;
	private int resCarWidth;
	private int resCarHeight;
	private float resCarMovement;
	private PlacedAnimation[] animations;
	private ArrayList<VisualElement> visualElements;
	public FinishVisual(Player player, Race race) {
		this.race = race;
		this.player = player;

		resCarWidth = (int) (Race.WIDTH * 1.16f / 2);
		resCarHeight = (int) (Race.HEIGHT * 0.726f / 2);
		resCarMovement = Race.WIDTH / 8.5f;

		resCar = new PlacedAnimation("resCar", 4, -resCarWidth, (int) (Race.HEIGHT - (Race.HEIGHT / 1.9f)));

		finishedPlayers = new ConcurrentLinkedQueue<PlacedAnimation>();

		animations = new PlacedAnimation[0];
		
		visualElements = new ArrayList<VisualElement>();

		try {

			resBackground = ImageIO.read(RaceVisual.class.getResourceAsStream("/pics/back.jpg"));

		} catch (IOException e) {
			System.err.println("didn't find the picture you were looking for");
			e.printStackTrace();
		}

	}

	@Override
	public void tick() {
		animations = finishedPlayers.toArray(animations);

		for (PlacedAnimation ma : animations) {
			ma.moveX((int) resCarMovement);
			ma.incrementCurrentFrame();
			if (ma.getX() > Race.WIDTH)
				finishedPlayers.remove(ma);
		}
		
		for(int i = 0; i < visualElements.size(); i++) {
			visualElements.get(i).tick();
		}

	}

	@Override
	public void render(Graphics g) {
		try {
			bs = this.getBufferStrategy();
			if (bs == null) {
				this.createBufferStrategy(3);
				return;
			}
			g = bs.getDrawGraphics();

			Graphics2D g2d = (Graphics2D) g;
			g2d.drawImage(resBackground, 0, 0, Race.WIDTH, Race.HEIGHT, null);

			for(PlacedAnimation ma : animations) {
				g2d.drawImage(ma.getFrame(), ma.getX(), ma.getY(), resCarWidth, resCarHeight, null);
			}
			
			for(int i = 0; i < visualElements.size(); i++) {
				visualElements.get(i).render(g);
			}

		} finally {

			if (g != null) {
				g.dispose();
			}
		}
		bs.show();
		Toolkit.getDefaultToolkit().sync();
	}

	public void addFinish() {
		finishedPlayers.add(resCar);
	}

	@Override
	public void setRace(Race race) {
		this.race = race;
	}

	@Override
	public void setPlayer(Player player) {
		this.player = player;
	}

	@Override
	public boolean hasAnimationsRunning() {
		return !finishedPlayers.isEmpty();
	}

	@Override
	public void addButton(VisualElement btn) {
		visualElements.add(btn);
	}

}