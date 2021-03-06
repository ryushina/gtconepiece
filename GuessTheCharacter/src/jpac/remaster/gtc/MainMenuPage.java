package jpac.remaster.gtc;

import jpac.remaster.gtc.core.GTCActivity;
import jpac.remaster.gtc.data.DataManager;
import jpac.remaster.gtc.logic.PuzzleManager;
import jpac.remaster.gtc.util.Constants;
import jpac.remaster.gtc.util.ResourceManager;
import jpac.remaster.gtc.util.Util;
import jpac.remaster.gtc.util.social.AppRater;
import jpac.remaster.gtc.util.social.GTCAuthAdapter;

import org.brickred.socialauth.android.SocialAuthAdapter.Provider;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import com.purplebrain.adbuddiz.sdk.AdBuddiz;

public class MainMenuPage extends GTCActivity {

	private static final int REQUEST_RESETCONFIRM = 1;
	private static final int REQUEST_ACKNOWLEDGE_RESET = 2;
	private static final int REQUEST_FACEBOOK_ACTION = 3;
	private static final int REQUEST_FACEBOOK_SIGN = 4;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.new_main_menu);

		AppRater.app_launched(this);
		checkOneTimeBonus();

		// this is to fix multiple instance of main menu
		SharedPreferences prefs = getSharedPreferences("splash", MODE_PRIVATE);
		if (!prefs.getBoolean("loaded", false)) {
			finish();
		}

		setOnClickListener(R.id.playButton, new OnClickListener() {

			@Override
			public void onClick(View v) {
				mGaTracker.sendEvent("ui_action", "button_press",
						"play_button", 0L);
				if (PuzzleManager.isFinished()) {
					startActivity(new Intent(MainMenuPage.this,
							GameFinishedPage.class));
				} else {
					startActivity(new Intent(MainMenuPage.this,
							InGamePage.class));
				}
			}
		});

		setOnClickListener(R.id.aboutButton, new OnClickListener() {

			@Override
			public void onClick(View v) {
				mGaTracker.sendEvent("ui_action", "button_press",
						"about_button", 0L);
				startActivity(new Intent(MainMenuPage.this, AboutUsPage.class));
			}
		});

		setOnClickListener(R.id.resetButton, new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivityForResult(
						Util.createConfirmPopup(
								MainMenuPage.this,
								"Confirm Action",
								"This will clear all your progress as of now. Are you sure you want to reset data?"),
						REQUEST_RESETCONFIRM);
			}
		});

		setOnClickListener(R.id.facebookButton, new OnClickListener() {

			@Override
			public void onClick(View v) {
				String message;
				if (GTCAuthAdapter.isConnected(MainMenuPage.this,
						Provider.FACEBOOK)) {
					message = "Are you sure you want to sign out from Facebook?";
				} else {
					message = "This will connect to your Facebook account.";
				}
				startActivityForResult(Util.createConfirmPopup(
						MainMenuPage.this, "Confirm Action", message),
						REQUEST_FACEBOOK_ACTION);
			}
		});

		Typeface roboto = ResourceManager.getFont("roboto_bold.ttf");
		setTypeface(R.id.playButton, roboto);
		setTypeface(R.id.facebookButton, roboto);
		setTypeface(R.id.aboutButton, roboto);
		setTypeface(R.id.resetButton, roboto);
	}

	private void checkOneTimeBonus() {
		SharedPreferences prefs = getSharedPreferences("bonus", MODE_PRIVATE);
		boolean received = prefs.getBoolean("receive_bonus", false);

		if (!received) {
			prefs.edit().putBoolean("receive_bonus", true).commit();
			DataManager.earnGold(40);
			startActivityForResult(
					Util.createAcknowledgePopup(
							this,
							"Bonus Gold",
							"Thank you for playing Guess the Character: One Piece.\n\nAs a token of " +
							"appreciation, here is 40 bonus gold. Enjoy the game."),
					Constants.NA);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		SharedPreferences prefs = getSharedPreferences("splash", MODE_PRIVATE);
		prefs.edit().putBoolean("loaded", false).commit();
	}

	@Override
	protected void onStart() {
		super.onStart();
		AdBuddiz.getInstance().onStart(this);
		if (Util.showAd()) {
			mGaTracker.sendEvent("event", "show_ad", "ads", 0L);
		}
	}

	private void doFacebookAction() {
		Intent intent = new Intent(this, SocialPostingPage.class);
		if (GTCAuthAdapter.isConnected(this, Provider.FACEBOOK)) {
			intent.putExtra("action", SocialPostingPage.ACTION_SIGN_OUT);
		} else {
			intent.putExtra("action", SocialPostingPage.ACTION_SIGN_IN);
		}
		startActivityForResult(intent, REQUEST_FACEBOOK_SIGN);
	}

	@Override
	protected void onResume() {
		super.onResume();
		setTypeface(R.id.currLevelLabel,
				ResourceManager.getFont("roboto_black.ttf"));
		setText(R.id.currLevelLabel, "" + DataManager.checkLevel());

		setTypeface(R.id.banner, ResourceManager.getFont("digitalstrip.ttf"));

		if (GTCAuthAdapter.isConnected(this, Provider.FACEBOOK)) {
			setText(R.id.facebookButton,
					ResourceManager.loadString(R.string.label_signout));
		} else {
			setText(R.id.facebookButton,
					ResourceManager.loadString(R.string.label_signin));
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_RESETCONFIRM && resultCode == RESULT_OK) {
			DataManager.reset();
			PuzzleManager.reset();
			getSharedPreferences("skip_credits", MODE_PRIVATE).edit().clear()
					.commit();
			startActivityForResult(Util.createAcknowledgePopup(this,
					"Data Reset", "Your user data has been deleted."),
					REQUEST_ACKNOWLEDGE_RESET);
		} else if (requestCode == REQUEST_ACKNOWLEDGE_RESET) {
			onResume();
		} else if (requestCode == REQUEST_FACEBOOK_ACTION
				&& resultCode == RESULT_OK) {
			doFacebookAction();
		} else if (requestCode == REQUEST_FACEBOOK_SIGN) {
			if (resultCode == RESULT_OK) {
				if (GTCAuthAdapter.isConnected(this, Provider.FACEBOOK)) {
					Util.displayToast(this,
							"You are now connected to Facebook.");
				} else {
					Util.displayToast(this, "Logged Out");
				}
			}
		}
	}
}
