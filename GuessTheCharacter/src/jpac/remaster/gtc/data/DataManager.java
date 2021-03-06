package jpac.remaster.gtc.data;

import java.util.List;

import jpac.remaster.gtc.logic.ButtonMetadata;
import jpac.remaster.gtc.logic.PuzzleManager;
import jpac.remaster.gtc.logic.UserData;
import jpac.remaster.gtc.util.Constants;
import jpac.remaster.gtc.util.ResourceManager;
import android.content.Context;
import android.content.SharedPreferences;

public class DataManager {

	private static Context contextRef;

	// ========================================================================
	// Version 1.0 Internal file compatibility
	// ========================================================================
	public static final int USER_DATA = 1;
	public static final int BUTTON_STATE = 2;
	public static final int SOLVED_PUZZLES = 3;

	private static UserData userData = new UserData();
	private static ButtonMetadata buttonMetadata = new ButtonMetadata();
	private static String puzzleMetadata = "";
	private static int socialData = Constants.NA;

	public static void setContextReference(Context context) {
		DataManager.contextRef = context;
	}

	public static void init() {
		userData = new UserData();
		buttonMetadata = new ButtonMetadata();
		socialData = Constants.NA;
		puzzleMetadata = "";

		for (int i = 1; i <= SOLVED_PUZZLES; i++) {
			checkInternalFile(i);
		}
		
		if (puzzleMetadata != null) {
			PuzzleManager.populateList(puzzleMetadata.split("\n"));
		}
		loadSharedPrefs();
	}
	
	private static void loadSharedPrefs() {
		SharedPreferences prefs = contextRef.getSharedPreferences(
				Constants.SHARED_PREFERENCES, Context.MODE_PRIVATE);
		
		// get userdata
		userData.setLevel(prefs.getInt("level", Constants.START_LEVEL));
		userData.setGold(prefs.getInt("gold", Constants.START_GOLD));
		userData.setCurrentPuzzle(prefs.getInt("current_puzzle", Constants.NA));
		
		// get social data
		socialData = prefs.getInt("posted_puzzle", Constants.NA);
		
		// get puzzle metadata
		puzzleMetadata = prefs.getString("puzzle_metadata", "");
		PuzzleManager.populateList(puzzleMetadata.split("\n"));
		
		// get button metadata
		String puzzleInfo = prefs.getString("button_puzzle_info", null);
		if (puzzleInfo != null) {
			String[] meta = puzzleInfo.split("@");
			buttonMetadata.updatePuzzle(Integer.valueOf(meta[0]), meta[1]);
		}
		
		String removed = prefs.getString("button_removed", null);
		if (removed != null) {
			String[] meta = removed.split(":");
			buttonMetadata.setRemovedMetadata(meta);
		}
		
		String locked = prefs.getString("button_locked", null);
		if (locked != null) {
			buttonMetadata.setLockedButton(locked);
		}
	}

	private static void checkInternalFile(int file) {
		String name = null;
		switch (file) {
		case USER_DATA:
			name = Constants.FILE_USER_DATA;
			break;
		case BUTTON_STATE:
			name = Constants.FILE_BUTTON_DATA;
			break;
		case SOLVED_PUZZLES:
			name = Constants.FILE_USER_DATA;
			break;
		}

		if (ResourceManager.isFileExist(name)) {
			String content = ResourceManager.loadData(name);
			processContent(file, content);
		}
		delete();
	}

	private static void delete() {
		contextRef.deleteFile(Constants.FILE_USER_DATA);
		contextRef.deleteFile(Constants.FILE_PUZZLE_DATA);
		contextRef.deleteFile(Constants.FILE_BUTTON_DATA);
	}

	private static void processContent(int file, String content) {
		switch (file) {
		case USER_DATA:
			restoreUserData(content);
			break;
		case BUTTON_STATE:
			restoreButtonMetadata(content);
			break;
		case SOLVED_PUZZLES:
			puzzleMetadata = content;
			updateStringPrefs("puzzle_metadata", puzzleMetadata);
			break;
		}
	}

	private static void restoreButtonMetadata(String content) {
		String[] metadata = content.split(":");

		if (metadata != null) {
			// update puzzle information
			String[] puzzleInfo = metadata[0].split("@");
			if (puzzleInfo != null) {
				buttonMetadata.updatePuzzle(Integer.valueOf(puzzleInfo[0]),
						puzzleInfo[1]);
			}
			updateStringPrefs("button_puzzle_info", metadata[0]);

			// update removed buttons
			String removed = "";
			String[] letters = metadata[1].split("@");
			if (letters != null) {
				int n = letters.length;
				for (int i = 0; i < n; i++) {
					if (i == 0) {
						removed = removed.concat(letters[i]);
					} else {
						removed = removed.concat(":" + letters[i]);
					}
				}
			}
			if (removed.length() > 0) {
				updateStringPrefs("button_removed", removed);
			}

			// update locked button state
			String locked = metadata[2];
			
			if (locked.length() > 0) {
				updateStringPrefs("button_locked", locked);
			}
		}
	}

	private static void restoreUserData(String content) {
		if (content != null) {
			String[] data = content.split("\n");

			userData.setLevel(Integer.valueOf(data[1]));
			userData.setGold(Integer.valueOf(data[2]));
			userData.setCurrentPuzzle(Integer.valueOf(data[3]));

			updateStringPrefs("username", userData.getUsername());
			updateIntPrefs("level", userData.getLevel());
			updateIntPrefs("gold", userData.getGold());
			updateIntPrefs("current_puzzle", userData.getCurrentPuzzle());
		}
	}

	public static boolean checkIfPosted(int id) {
		return socialData == id;
	}

	public static void updatePosted(int id) {
		socialData = id;
		updateIntPrefs("posted_puzzle", id);
	}

	// ========================================================================
	// Metadata Operations
	// ========================================================================
	public static void updateSolvedPuzzle(int id) {
		if (puzzleMetadata.length() > 0) {
			puzzleMetadata += "\n" + id;
		} else {
			puzzleMetadata = String.valueOf(id);
		}
		updateStringPrefs("puzzle_metadata", puzzleMetadata);
	}

	public static String getPuzzleMetadata() {
		return puzzleMetadata;
	}

	public static void updatePuzzleInfo(int id, String answer) {
		buttonMetadata.updatePuzzle(id, answer);
		updateStringPrefs("button_puzzle_info", id + "@" + answer);
	}

	public static void addRemovedButton(String text) {
		buttonMetadata.addRemovedButton(text);
		List<String> removed = buttonMetadata.getRemovedButtons();
		updateStringPrefs("button_removed", parseRemovedButton(removed));
	}
	
	public static void updateLockState(String state) {
		buttonMetadata.setLockedButton(state);
		updateStringPrefs("button_locked", state);
	}

	private static String parseRemovedButton(List<String> removedButtons) {
		String removed = "";
		String[] letters = removedButtons.toArray(new String[removedButtons.size()]);
		if (letters != null) {
			int n = letters.length;
			for (int i = 0; i < n; i++) {
				if (i == 0) {
					removed = removed.concat(letters[i]);
				} else {
					removed = removed.concat(":" + letters[i]);
				}
			}
		}
		return removed;
	}
	
	public static String getRemovedButtons() {
		return parseRemovedButton(buttonMetadata.getRemovedButtons());
	}

	public static String getLockedButtons() {
		return buttonMetadata.getLockedButton();
	}
	
	public static String getPuzzleInfo() {
		return buttonMetadata.getPuzzleID() + "@" + buttonMetadata.getPuzzleAnswer();
	}

	// ========================================================================
	// User data Operations
	// ========================================================================
	public static void levelUp() {
		userData.setLevel(userData.getLevel() + 1);
		updateIntPrefs("level", userData.getLevel());
	}

	public static void earnGold(int amount) {
		userData.setGold(userData.getGold() + amount);
		updateIntPrefs("gold", userData.getGold());
	}

	public static void spendGold(int cost) {
		userData.setGold(userData.getGold() - cost);
		updateIntPrefs("gold", userData.getGold());
	}

	public static boolean isGoldEnough(int cost) {
		return userData.getGold() - cost >= 0;
	}

	public static void updatePuzzle(int puzzle) {
		userData.setCurrentPuzzle(puzzle);
		updateIntPrefs("current_puzzle", userData.getCurrentPuzzle());
	}

	public static int checkGold() {
		return userData.getGold();
	}

	public static int checkLevel() {
		return userData.getLevel();
	}

	public static int checkCurrentPuzzleId() {
		return Integer.valueOf(userData.getCurrentPuzzle());
	}

	private static void updateIntPrefs(String key, int value) {
		SharedPreferences prefs = contextRef.getSharedPreferences(
				Constants.SHARED_PREFERENCES, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();

		editor.putInt(key, value);
		editor.commit();
	}

	private static void updateStringPrefs(String key, String value) {		
		SharedPreferences prefs = contextRef.getSharedPreferences(
				Constants.SHARED_PREFERENCES, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();

		editor.putString(key, value);
		editor.commit();
	}

	public static void reset() {
		SharedPreferences prefs = contextRef.getSharedPreferences(
				Constants.SHARED_PREFERENCES, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.clear();

		userData = new UserData();
		editor.putString("username", userData.getUsername());
		editor.putInt("level", userData.getLevel());
		editor.putInt("gold", userData.getGold());
		editor.putInt("current_puzzle", userData.getCurrentPuzzle());

		editor.commit();
	}

	public static void clearButtonMetadata() {
		SharedPreferences prefs = contextRef.getSharedPreferences(
				Constants.SHARED_PREFERENCES, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		
		editor.remove("button_removed");
		editor.remove("button_locked");
		editor.commit();
	}
}
