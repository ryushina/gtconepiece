package jpac.remaster.gtc;

import jpac.remaster.gtc.core.GTCActivity;
import jpac.remaster.gtc.logic.ButtonManager;
import jpac.remaster.gtc.logic.Puzzle;
import jpac.remaster.gtc.logic.PuzzleManager;
import jpac.remaster.gtc.logic.UserActionListener;
import jpac.remaster.gtc.logic.UserDataManager;
import jpac.remaster.gtc.util.Constants;
import jpac.remaster.gtc.util.ResourceManager;
import jpac.remaster.gtc.util.Util;
import jpac.remaster.gtc.util.social.GTCAuthAdapter;
import jpac.remaster.gtc.util.social.SocialDataManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class InGamePage extends GTCActivity implements UserActionListener {

	private static final int REQUEST_SHOW_CONFIRM_BACK = 0;
	private static final int REQUEST_SHOW_ACKNOWLEDGE = 1;
	private static final int REQUEST_CONFIRM_REVEAL = 2;
	private static final int REQUEST_CONFIRM_REMOVE = 3;
	private static final int REQUEST_CONFIRM_SOLVE = 4;
	private static final int REQUEST_SHARE_FACEBOOK = 5;
	private static final int REQUEST_PUBLISH_FEED = 6;

	private Puzzle puzzle;

	private ButtonManager buttonManager;

	private boolean alreadyPosted = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.game_page);

		setOnClickListener(R.id.backButton, new OnClickListener() {

			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

		buttonManager = ButtonManager.getInstance();

		if (PuzzleManager.isFinished()) {
			puzzle = null;
		} else if (UserDataManager.checkCurrentPuzzleId() == -1) {
			puzzle = PuzzleManager.getNextPuzzle();
		} else {
			puzzle = PuzzleManager.getPuzzleById(UserDataManager
					.checkCurrentPuzzleId());
		}
		PuzzleManager.currentPuzzle = puzzle;

		updateLevel();
		updateGold();

		if (puzzle == null) {
			startActivity(new Intent(this, GameFinishedPage.class));
			finish();
		} else {
			if (SocialDataManager.checkIfPosted(puzzle.getId())) {
				alreadyPosted = true;
			}
			UserDataManager.updatePuzzle("" + puzzle.getId());

			buttonManager.init(this, puzzle.getAnswer());
			buttonManager.setChoices(PuzzleManager.createChoiceSet(
					puzzle.getAnswer(), puzzle.getRandomSeed()));
			buttonManager.initAnswerField(this, puzzle.getRawAnswer());
			buttonManager.setAnswerDoneListener(this);
			buttonManager.loadData(this);

			try {
				setImage(
						R.id.puzzleImage1,
						ResourceManager.loadBitmapFromAsset(puzzle.getImageId()));
			} catch (RuntimeException e) {
				finish();
			}
			updatePuzzleDifficulty();

			setTypeface(R.id.categoryLabel, ResourceManager.getFont("digitalstrip.ttf"));
			setText(R.id.categoryLabel, puzzle.getCategory());
		}
	}

	private void updateLevel() {
		setText(R.id.currLevelLabel, "" + UserDataManager.checkLevel());
		setTypeface(R.id.currLevelLabel, ResourceManager.getFont("digitalstrip.ttf"));
		setTypeface(R.id.levelLabel, ResourceManager.getFont("digitalstrip.ttf"));
	}

	private void updateGold() {
		setText(R.id.amountLabel, "" + UserDataManager.checkGold());
		setTypeface(R.id.amountLabel, ResourceManager.getFont("digitalstrip.ttf"));
		setTypeface(R.id.goldLabel, ResourceManager.getFont("digitalstrip.ttf"));
	}

	private void updatePuzzleDifficulty() {
		int level = puzzle.getDifficulty();
		int drawable = R.drawable.level_full;
		
		switch (level) {
		case 5:
			setImageResource(R.id.level5, drawable);
		case 4:
			setImageResource(R.id.level4, drawable);
		case 3:
			setImageResource(R.id.level3, drawable);
		case 2:
			setImageResource(R.id.level2, drawable);
		case 1:
			setImageResource(R.id.level1, drawable);
		}
	}

	public void removeLetter(View v) {
		if (UserDataManager.isGoldEnough(Constants.REMOVE_COST)) {
			showConfirmUseHint("Pay " + Constants.REMOVE_COST
					+ " gold to remove an incorrect letter from the choices.",
					REQUEST_CONFIRM_REMOVE);
		} else {
			showInsufficientGold(Constants.REMOVE_COST);
		}
	}

	public void revealLetter(View v) {
		if (UserDataManager.isGoldEnough(Constants.REVEAL_COST)) {
			showConfirmUseHint("Pay " + Constants.REVEAL_COST
					+ " gold to reveal a correct letter.",
					REQUEST_CONFIRM_REVEAL);
		} else {
			showInsufficientGold(Constants.REVEAL_COST);
		}
	}

	public void solvePuzzle(View v) {
		if (UserDataManager.isGoldEnough(Constants.SOLVE_COST)) {
			showConfirmUseHint("Pay " + Constants.SOLVE_COST
					+ " gold to solve the puzzle.", REQUEST_CONFIRM_SOLVE);
		} else {
			showInsufficientGold(Constants.SOLVE_COST);
		}
	}

	public void askFacebook(View v) {
		if (alreadyPosted) {
			Intent intent = new Intent(this, AcknowledgementPopup.class);
			intent.putExtra("title", "Facebook");
			intent.putExtra(
					"message",
					"You've already posted this image.\n\nCheck the comments section to know what your friends think.");
			startActivityForResult(intent, REQUEST_SHOW_ACKNOWLEDGE);
		} else {
			startActivityForResult(new Intent(this, SocialPopup.class),
					REQUEST_SHARE_FACEBOOK);
		}
	}

	private void showConfirmUseHint(String message, int requestCode) {
		Intent intent = new Intent(this, ConfirmationPopup.class);
		intent.putExtra("title", "Confirm Use Hint");
		intent.putExtra("message", message);
		startActivityForResult(intent, requestCode);
	}

	private void showInsufficientGold(int cost) {
		Intent intent = new Intent(this, AcknowledgementPopup.class);
		intent.putExtra("title", "Insufficient Gold");
		intent.putExtra("message", "You need at least " + cost
				+ " Gold to ues this hint.");
		startActivityForResult(intent, REQUEST_SHOW_ACKNOWLEDGE);
	}

	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, ConfirmationPopup.class);
		intent.putExtra("title", "Confirm Action");
		intent.putExtra("message", "Return to Main Menu?");
		startActivityForResult(intent, REQUEST_SHOW_CONFIRM_BACK);
		return;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case REQUEST_SHOW_CONFIRM_BACK:
				finish();
				break;
			case REQUEST_CONFIRM_REMOVE:
				if (buttonManager.removeLetter(puzzle.getAnswer())) {
					UserDataManager.spendGold(Constants.REMOVE_COST);
				} else {
					Util.displayToast(this, "No more letter to remove");
				}
				break;
			case REQUEST_CONFIRM_REVEAL:
				UserDataManager.spendGold(Constants.REVEAL_COST);
				buttonManager.revealLetter(puzzle.getAnswer());
				break;
			case REQUEST_CONFIRM_SOLVE:
				UserDataManager.spendGold(Constants.SOLVE_COST);
				showLevelComplete();
				break;
			case REQUEST_SHARE_FACEBOOK:
				if (GTCAuthAdapter.isNetworkAvailable(getApplicationContext())) {
					// Util.captureView(findViewById(R.id.gamepage));
					Intent intent = new Intent(this, SocialPostingPage.class);
					intent.putExtra("action", SocialPostingPage.ACTION_SHARE);
					startActivityForResult(intent, REQUEST_PUBLISH_FEED);
				} else {
					Util.displayToast(getApplicationContext(),
							"Check your Internet connection.");
				}
				break;
			case REQUEST_PUBLISH_FEED:
				SocialDataManager.updatePosted(puzzle.getId());
				alreadyPosted = true;
				break;
			default:
				break;
			}

			updateGold();
		}
	}

	@Override
	public void onAnswerComplete(String sequence) {
		if (puzzle.getAnswer().compareTo(sequence) == 0) {
			showLevelComplete();
		} else {
			Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
			startAnimation(R.id.answers, shake);
		}
	}

	private void showLevelComplete() {
		PuzzleManager.markAsSolved(puzzle.getId());
		int prize = puzzle.getDifficulty() * Constants.PUZZLE_PRIZE;
		UserDataManager.earnGold(prize);
		UserDataManager.levelUp();
		Intent intent = new Intent(this, LevelFinishedPage.class);
		intent.putExtra("prize", prize);
		intent.putExtra("image", puzzle.getImageId());
		intent.putExtra("answer", puzzle.getFormattedAnswer());
		startActivity(intent);

		ResourceManager.queueBitmapForRecycle(puzzle.getImageId());

		finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		buttonManager.saveData(this, puzzle);
		UserDataManager.saveData(this);
		PuzzleManager.saveData(this);
		SocialDataManager.saveData(this);
	}

}
