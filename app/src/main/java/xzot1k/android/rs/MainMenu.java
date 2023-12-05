package xzot1k.android.rs;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainMenu extends AppCompatActivity {

    public static Difficulty DIFFICULTY = Difficulty.EASY;  // set difficulty to easy by default
    private static boolean FIRST_START = false, SHOW_AGAIN = false;
    private static File SAVE_FILE; // save file

    private MediaPlayer mediaPlayer; // media player variable for class access

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SAVE_FILE = new File(getFilesDir(), "data.json"); // initialize the save file

        // ensure this is the first time the app was started
        if (!FIRST_START) {
            FIRST_START = true;
            if (SAVE_FILE.exists()) load(); // check if the save file exists, if so, load all data values stored
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);

        // get toolbar and make it the support action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // setup the action bar for cosmetics (future additions can be made)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayUseLogoEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle(R.string.app_name);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.small_icon);
        }

        // get the spinner for the difficulty selection
        Spinner spinner = (Spinner) findViewById(R.id.difficultySpinner);

        // create & set the adapter for the difficulty spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new ArrayList<String>(){{
                    // add all difficulties to the drop down by name
                    for(int i = -1; ++i < Difficulty.values().length; )
                        add(Difficulty.values()[i].name());
                }});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // ensure the correct item is selected based on current difficulty
        spinner.setSelection(DIFFICULTY.getIndex());

        // handle the listener for when a drop down is selected
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                DIFFICULTY = Difficulty.getByIndex(position); // update difficulty to the selected difficulty
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {}

        });



        // get and setup the play button
        Button playButton = findViewById(R.id.play);
        playButton.setOnClickListener(view -> {

            // if checkbox wasn't checked this instance, show the instructions
            if (!SHOW_AGAIN) {
                showInstructions(view);
                return;
            }

            startGame();  // start the game

        });

        // create that credits button and make it show the credits when clicked
        Button creditsButton = findViewById(R.id.creditsButton);
        creditsButton.setOnClickListener(this::showCredits);

        // handle the quit button to exit the game
        Button quitButton = findViewById(R.id.quitButton);
        quitButton.setOnClickListener(view -> {
            finish();               // finish the main activity
            System.exit(1);   // exit the application
        });

        playMusic(); // play the background music
    }

    /**
     * Plays the background music.
     */
    private void playMusic() {
        // creates the player and makes it loop
        mediaPlayer = MediaPlayer.create(this, R.raw.childs_nightmare);
        mediaPlayer.setLooping(true);
        mediaPlayer.setVolume(0.15f, 0.15f);
        mediaPlayer.start(); // starts the music
    }

    /**
     * Takes the current view and creates a credits popup window for all associations with sounds, music, etc.
     *
     * @param view The current view.
     */
    private void showCredits(final View view) {

        // pop up view
        View popupView = View.inflate(view.getContext(), R.layout.credits, null);

        // creates a new popup window
        final PopupWindow popupWindow = new PopupWindow(popupView,
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, true);

        // set the outside touch popup view (close when the popup windows is unfocused).
        popupView.setOnClickListener(pView -> popupWindow.dismiss());

        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0); // center the popup window on screen

        // make sure text links are clickable
        TextView content = popupView.findViewById(R.id.creditsText);
        content.setMovementMethod(LinkMovementMethod.getInstance());

        // make the close button dismiss the pop up window when clicked
        Button closeButton = popupView.findViewById(R.id.playConfirmButton);
        closeButton.setOnClickListener(v -> popupWindow.dismiss());
    }

    /**
     * Takes the current view and creates a credits popup window for all associations with sounds, music, etc.
     *
     * @param view The current view.
     */
    private void showInstructions(final View view) {

        // pop up view
        View popupView = View.inflate(view.getContext(), R.layout.instructions, null);

        // creates a new popup window
        final PopupWindow popupWindow = new PopupWindow(popupView,
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, true);

        // set the outside touch popup view (close when the popup windows is unfocused).
        popupView.setOnClickListener(pView -> popupWindow.dismiss());

        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0); // center the popup window on screen

        // make sure text links are clickable
        TextView content = popupView.findViewById(R.id.insText);
        content.setMovementMethod(LinkMovementMethod.getInstance());

        // make sure the switch toggles whether the instructions are shown for the rest of the instance
        SwitchCompat showAgainSwitch = popupView.findViewById(R.id.showAgainSwitch);
        showAgainSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> SHOW_AGAIN = isChecked);

        // make the close button dismiss the pop up window when clicked
        Button playButton = popupView.findViewById(R.id.playConfirmButton);
        playButton.setOnClickListener(v -> {

            popupWindow.dismiss(); // dismiss the popup menu

            startGame();           // start the game

        });
    }

    /**
     * Starts the game and finishes the main menu activity.
     */
    private void startGame() {
        // stop and release the music player
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        finish();               // finish the activity

        // start a new intent and start the game activity
        Intent intent = new Intent(this, Game.class);
        startActivity(intent);
    }

    /**
     * Loads all saved data from the save file.
     */
    public void load() {

        if (!SAVE_FILE.exists()) return; // check if the file exists, return if not

        // open new buffered reader that auto closes
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(openFileInput(SAVE_FILE.getName())))) {

            StringBuilder contentString = new StringBuilder(); // content string builder for the json

            String line; // current line bytes
            do {

                line = bufferedReader.readLine();   // read next line
                contentString.append(line);         // append line to content string

            } while (line != null); // loop until the line is invalid

            JSONObject dataObject = new JSONObject(contentString.toString()); // new json object given the content string

            Game.RECORD = dataObject.getLong("record"); // load the record value to memory

        } catch (JSONException | IOException e) {e.printStackTrace();} // catch exceptions and print error to console
    }

    /**
     * Save all data to the save file.
     */
    public static void save() {
        try {
            JSONObject dataObject = new JSONObject(); // new json object to save data

            dataObject.put("record", Game.RECORD); // save record value to the save file

            FileWriter fileWriter = new FileWriter(SAVE_FILE);               // new file writer for the save file
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter); // new buffered writer to write to the save file writer
            bufferedWriter.write(dataObject.toString());                    // write json object string to the file

            // close both the buffered and file writers
            bufferedWriter.close();
            fileWriter.close();

        } catch (JSONException | IOException e) {e.printStackTrace();} // catch exceptions and print error to console
    }

    public enum Difficulty {
        EASY(1000), MEDIUM(600), HARD(300);

        private final long baseDelay;

        Difficulty(long baseDelay) {this.baseDelay = baseDelay;}

        /**
         * @param index The index of the difficulty desired.
         * @return The found difficulty (Can be NULL).
         */
        public static Difficulty getByIndex(int index) {

            // loop each difficulty to compare indexes to see which difficulty to return
            for(int i = -1; ++i < values().length;)
            {
                final Difficulty difficulty = values()[i];
                if(difficulty.getIndex() == index) return difficulty;
            }

            return null;
        }

        /**
         * @return Get the index value of the difficulty.
         */
        public int getIndex() {

            for(int i = -1; ++i < values().length;)
                if(this == values()[i]) return i;

            return -1;
        }

        /**
         * @return The base delay used for the difficulty.
         */
        public long getBaseDelay() {return baseDelay;}

    }
}