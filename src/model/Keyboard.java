package model;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

class Keyboard {

	
	private static Map<String, Integer> letters;
	static
    {
        letters = new HashMap<String, Integer>();
        letters.put("a", KeyEvent.VK_A);
        letters.put("bee", KeyEvent.VK_B);
        letters.put("sea", KeyEvent.VK_C);
        letters.put("dee", KeyEvent.VK_D);
        letters.put("ee", KeyEvent.VK_E);
        letters.put("ef", KeyEvent.VK_F);
        letters.put("gee", KeyEvent.VK_G);
        letters.put("aitch", KeyEvent.VK_H);
        letters.put("eye", KeyEvent.VK_I);
        letters.put("jay", KeyEvent.VK_J);
        letters.put("kay", KeyEvent.VK_K);
        letters.put("ell", KeyEvent.VK_L);
        letters.put("em", KeyEvent.VK_M);
        letters.put("en", KeyEvent.VK_N);
        letters.put("oh", KeyEvent.VK_O);
        letters.put("pee", KeyEvent.VK_P);
        letters.put("cue", KeyEvent.VK_Q);
        letters.put("are", KeyEvent.VK_R);
        letters.put("ess", KeyEvent.VK_S);
        letters.put("tee", KeyEvent.VK_T);
        letters.put("you", KeyEvent.VK_U);
        letters.put("vee", KeyEvent.VK_V);
        letters.put("double you", KeyEvent.VK_W);
        letters.put("ex", KeyEvent.VK_X);
        letters.put("why", KeyEvent.VK_Y);
        letters.put("zee", KeyEvent.VK_Z);
    }
	
	private static Map<String, Integer> meta;
	static {
		meta = new HashMap<String, Integer>();
		meta.put("shift", KeyEvent.VK_SHIFT);
		meta.put("control", KeyEvent.VK_CONTROL);
		meta.put("alt", KeyEvent.VK_ALT);
		meta.put("tab", KeyEvent.VK_TAB);
		meta.put("escape", KeyEvent.VK_ESCAPE);
		meta.put("cap", KeyEvent.VK_CAPS_LOCK);
		meta.put("windowns", KeyEvent.VK_WINDOWS);
		meta.put("enter", KeyEvent.VK_ENTER);
		meta.put("backspace", KeyEvent.VK_BACK_SPACE);
		meta.put("delete", KeyEvent.VK_DELETE);
		meta.put("space", KeyEvent.VK_SPACE);
	}
	
	private static Map<String, Integer> commands;
	static {
		commands = new HashMap<String, Integer>();
		commands.put("press", 0);
		commands.put("hold", 1);
		commands.put("release", 2);
	}
	
	private static Map<Integer, Timer> timers;
	static {
		timers = new HashMap<Integer, Timer>();
	}
	
	private Robot keyPresser;
	
	private boolean mute = false;
	
	public Keyboard() {
		try {
	        keyPresser = new Robot();
	        // Simulate a mouse click
//	        robot.mousePress(InputEvent.BUTTON1_MASK);
//	        robot.mouseRelease(InputEvent.BUTTON1_MASK);
		} catch (AWTException e) {
		        e.printStackTrace();
		}
	}

	public void parseText(String input) {
		String[] tokens = input.split(" ");
		int[] keyPresses = new int[tokens.length];
		// Parse key presses
		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].equals("mute"))
				mute = !mute;
			if (mute)
				continue;
			if (tokens[i].equals("double")) {
				if (i < tokens.length - 1 && tokens[i + 1].equals("you")) {
					keyPresses[i] = letters.get(tokens[i] + " " + tokens[i + 1]);
				} else {
					continue;
				}
			} else if (tokens[i].equals("you") && i > 0 && tokens[i - 1].equals("double")){
				continue;
			} else if (letters.containsKey(tokens[i])) {
				keyPresses[i] = letters.get(tokens[i]);
			} else if (meta.containsKey(tokens[i])) {
				keyPresses[i] = meta.get(tokens[i]);
			} else if (commands.containsKey(tokens[i])) {
				keyPresses[i] = commands.get(tokens[i]);
			}
		}
		performKeystrokes(keyPresses);
		System.out.println(Arrays.toString(tokens));
		System.out.println(Arrays.toString(keyPresses));
	}
	
	private void performKeystrokes(int[] keyPresses) {
		boolean hold = false;
		boolean release = false;
		boolean press = false;
		// Perform key presses
		for (int i = 0; i < keyPresses.length; i++) {
			if (keyPresses[i] < 4) {
				switch (keyPresses[i]) {
					case 0: press = true; break;
					case 1: hold = true; break;
					case 2: release = true; break;
				}
				continue;
			}
			try {
				if (hold) {
					if (!timers.containsKey(keyPresses[i])) {
						final int keyToRepeat = keyPresses[i];
						Timer timer = new Timer();
						timer.schedule(new TimerTask() {
							@Override
							public void run() {
								keyPresser.keyPress(keyToRepeat);
								keyPresser.keyRelease(keyToRepeat);
							}
						}, 500, 33);
						timers.put(keyPresses[i], timer);
					}
					hold = false;
				} else if (release) {
					if (timers.containsKey(keyPresses[i])) {
						Timer old = timers.remove(keyPresses[i]);
						old.cancel();
					}
					release = false;
				} else {
					keyPresser.keyPress(keyPresses[i]);
					keyPresser.keyRelease(keyPresses[i]);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Invalid key: " + keyPresses[i]);
			}
		}
	}

}
