package scenes;

import adt.Scene;
import adt.Visual;
import audio.Audio;
import main.RaceKeysSettings;
import main.RegularSettings;
import scenes.visual.OptionsVisual;

public class OptionsScene extends Scene {

	public OptionsScene() {
		super(new OptionsVisual(), "options");
	}

	public void init(RegularSettings settings, RaceKeysSettings raceKeysSettings, Audio audio) {
		
	}

	@Override
	public void tick(double delta) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyInput(int keycode, int action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseButtonInput(int button, int action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePosInput(double x, double y) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseScrollInput(double x, double y) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEnterWindowInput(boolean entered) {
		// TODO Auto-generated method stub
		
	}
	

}
