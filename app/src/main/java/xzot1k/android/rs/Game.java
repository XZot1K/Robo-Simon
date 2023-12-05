package xzot1k.android.rs;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

public class Game extends AppCompatActivity {

    public static long RECORD = 0; // the global record for keeping score.

    // media player, buttons, animations, and the handler
    private MediaPlayer mediaPlayer;
    private Button redButton, greenButton, yellowButton, blueButton, pinkButton, purpleButton;
    private TextView repeatText, statsText;
    private Animation scaleUp, scaleDown;
    private Handler handler;

    // all instance variables for game functionality
    private boolean repeatTurn;
    private long currentMove, currentMoveTotal, winCounter;
    private long[] sequenceOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);

        // initialize variables alongside the handler for delays
        winCounter = 0;
        currentMove = 0;
        currentMoveTotal = 3;
        repeatTurn = false;
        handler = new Handler();

        // get toolbar and set as the support action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.gameToolbar);
        setSupportActionBar(toolbar);

        // setup the action bar for cosmetics (future additions can be made)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayUseLogoEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle(R.string.app_name);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.small_icon);
        }

        // get each button for class access
        redButton = findViewById(R.id.redButton);
        greenButton = findViewById(R.id.greenButton);
        yellowButton = findViewById(R.id.yellowButton);
        blueButton = findViewById(R.id.blueButton);
        pinkButton = findViewById(R.id.pinkButton);
        purpleButton = findViewById(R.id.purpleButton);

        // sets up the listener for each button
        redButton.setOnClickListener((view) -> checkSelection(0));
        greenButton.setOnClickListener((view) -> checkSelection(1));
        yellowButton.setOnClickListener((view) -> checkSelection(2));
        blueButton.setOnClickListener((view) -> checkSelection(3));
        pinkButton.setOnClickListener((view) -> checkSelection(4));
        purpleButton.setOnClickListener((view) -> checkSelection(5));

        // get each text view for class access
        repeatText = findViewById(R.id.repeatText);
        statsText = findViewById(R.id.stats);

        updateStatsString(); // updates stats

        // get scale up/down animations for the buttons
        scaleUp = AnimationUtils.loadAnimation(this, R.anim.scale_up);
        scaleDown = AnimationUtils.loadAnimation(this, R.anim.scale_down);

        playMusic(); // play the background music

        // wait 2 seconds to ensure all UI elements are loaded, then begin the first animation
        handler.postDelayed(() -> {
            try {
                generateSequence(true);
            } catch (InterruptedException e) {e.printStackTrace();}
        }, 2000);
    }

    /**
     * Plays the background music.
     */
    private void playMusic() {
        // creates the player and makes it loop
        mediaPlayer = MediaPlayer.create(this, R.raw.solve_the_puzzle);
        mediaPlayer.setLooping(true);
        mediaPlayer.setVolume(0.15f, 0.15f);
        mediaPlayer.start(); // starts the music
    }

    /**
     * Updates the stats string on the game screen.
     */
    private void updateStatsString() {
        // update stats text to current memory
        statsText.setText(getString(R.string.stats).replace("{current}", String.valueOf(currentMove + 1))
                .replace("{total}", String.valueOf(currentMoveTotal))
                .replace("{record}", String.valueOf(RECORD)));
    }

    private void checkSelection(int color) {

        if (!repeatTurn) return; // skip action if input is not yet requested

        final Button button = getButtonFromColor(color); // get the button from the color selected

        playButtonAnimation(button); // play the button animation

        // update stats text to current memory
        statsText.setText(getString(R.string.stats).replace("{current}", String.valueOf(currentMove + 1))
                .replace("{total}", String.valueOf(currentMoveTotal))
                .replace("{record}", String.valueOf(RECORD)));

        final long correctColor = sequenceOrder[(int) currentMove];
        if (correctColor != color) {

            // play wrong answer/selection sound
            MediaPlayer mp = MediaPlayer.create(this, R.raw.wrong_answer);
            mp.setVolume(0.3f, 0.3f);
            mp.setLooping(false);
            mp.start();

            // reset stats, aside from the record
            currentMove = 0;
            currentMoveTotal = 3;

            // disable user input and hide the repeat text
            repeatTurn = false;
            repeatText.setVisibility(View.INVISIBLE);

            // clear the sequence array
            sequenceOrder = null;

            // stop and release the media player
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }

            // delay the play again screen to ensure the user doesn't click out of it unintentionally
            handler.postDelayed(() -> {
                // show the play again popup
                showPlayAgain(getWindow().getDecorView().getRootView());
            }, 800L);

            return;
        }

        currentMove++; // increment to next move in sequence

        if (currentMove >= currentMoveTotal) { // check if the user completed the full sequence, in order

            winCounter++; // update win counter

            if (RECORD < winCounter) {
                RECORD = winCounter; // update record, if surpassed
                MainMenu.save();     // save record to the save file
            }

            // play next sequence sound
            MediaPlayer mp = MediaPlayer.create(this, R.raw.achieve_sound);
            mp.setVolume(0.3f, 0.3f);
            mp.setLooping(false);
            mp.start();

            currentMove = 0;
            repeatTurn = false;
            repeatText.setVisibility(View.INVISIBLE);

            // wait 2 seconds to ensure all UI elements are loaded, then begin the next animation
            handler.postDelayed(() -> {
                try {
                    generateSequence(false);
                } catch (InterruptedException e) {e.printStackTrace();}
            }, 2000);

            return;
        }

        // play correct answer sound
        MediaPlayer mp = MediaPlayer.create(this, R.raw.ping);
        mp.setVolume(0.5f, 0.5f);
        mp.setLooping(false);
        mp.start();
    }

    private void generateSequence(boolean isFirstTime) throws InterruptedException {

        if (!isFirstTime) currentMoveTotal += 1;                                  // increase total moves that must be repeated by 1

        final ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current(); // thread random to randomize sequence moves

        sequenceOrder = new long[(int) currentMoveTotal];                        // new sequence order

        AtomicReference<Button> lastButton = new AtomicReference<>(); // atomic button reference to determine what the last button selection was

        for (int i = -1; ++i < currentMoveTotal; ) { // loop the total moves in the new sequence

            final int finalI = i; // duplicate i to allow usage within the delay tasks

            // after i * 1 second play the sequence animations and update the sequence order for repeating
            handler.postDelayed(() -> {
                int nextMove = threadLocalRandom.nextInt(0, 6);  // get next sequence move

                sequenceOrder[finalI] = nextMove;                             // update sequence with new color at current index

                // determine which button should be animated based on randomized integer
                final Button button = getButtonFromColor(nextMove);

                // check if the last button was the same as the newly selected, if so, clear the animation to make it clear what was selected
                if(lastButton.get() != null && lastButton.get() == button)
                    button.clearAnimation();

                lastButton.set(button); // update the last button as the current selection

                playButtonAnimation(button); // play the button animation

            }, (i * MainMenu.DIFFICULTY.getBaseDelay())); // increase speed of next sequence selection based on difficulty
        }

        // update stats text to current memory
        statsText.setText(getString(R.string.stats).replace("{current}", String.valueOf(currentMove + 1))
                .replace("{total}", String.valueOf(currentMoveTotal))
                .replace("{record}", String.valueOf(RECORD)));

        // delay enabling user input until the animations are done
        handler.postDelayed(() -> {

            repeatText.setVisibility(View.VISIBLE); // show the repeat text

            repeatTurn = true;                      // allow the player to start the repeating process

            // use difficulty base delay to ensure user input allowance is not too spaced out
        }, (currentMoveTotal * MainMenu.DIFFICULTY.getBaseDelay()));

    }

    /**
     * Simply gets the button associated to the color index (color meaning button).
     *
     * @param color The button color.
     * @return The button associated to the color number.
     */
    private Button getButtonFromColor(int color) {
        switch (color) {
            case 0: {return redButton;}

            case 1: {return greenButton;}

            case 2: {return yellowButton;}

            case 3: {return blueButton;}

            case 4: {return pinkButton;}

            default: {return purpleButton;}
        }
    }

    /**
     * Plays the button scale up & down animation.
     *
     * @param button The button to animate.
     */
    private void playButtonAnimation(Button button) {
        button.startAnimation(scaleUp); // start the scale up animation

        // wait 140ms then start the scale down animation
        handler.postDelayed(() -> {
            button.startAnimation(scaleDown);
            handler.postDelayed(button::clearAnimation, 140); // clear the animation 140ms later, post scale down
        }, 140);
    }

    /**
     * Takes the current view and creates a play again popup window
     *
     * @param view The current view.
     */
    private void showPlayAgain(final View view) {

        // pop up view
        View popupView = View.inflate(view.getContext(), R.layout.play_again, null);

        // creates a new popup window
        final PopupWindow popupWindow = new PopupWindow(popupView,
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, true);

        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0); // center the popup window on screen

        // make the close button dismiss the pop up window when clicked
        Button playAgainButton = popupView.findViewById(R.id.playAgainButton);
        playAgainButton.setOnClickListener(v -> {

            popupWindow.dismiss(); // dismiss the popup window

            playMusic(); // play the background music

            // wait 2 seconds to ensure all UI elements are loaded, then begin the first animation
            handler.postDelayed(() -> {
                try {
                    generateSequence(true);
                } catch (InterruptedException e) {e.printStackTrace();}
            }, 2000);
        });

        Button quitToMenuButton = popupView.findViewById(R.id.quitToMenuButton);
        quitToMenuButton.setOnClickListener(v -> {

            popupWindow.dismiss(); // dismiss the popup window

            finish();              // finish the game activity

            // start a new intent and start the main menu activity
            Intent intent = new Intent(this, MainMenu.class);
            startActivity(intent);
        });
    }

}