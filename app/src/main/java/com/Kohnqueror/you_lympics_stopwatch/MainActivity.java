package com.Kohnqueror.you_lympics_stopwatch;

import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

// Corrected imports to use the stopwatch app's own copied files
import com.Kohnqueror.you_lympics_stopwatch.Player;
import com.Kohnqueror.you_lympics_stopwatch.PlayerDataManager;
import com.Kohnqueror.you_lympics_stopwatch.TournamentSettings;


public class MainActivity extends AppCompatActivity implements PlayerDataManager.PlayerDataListener {

    // UI Components
    private TextView timerTextView;
    private Button startStopButton, resetButton, saveButton;
    private AutoCompleteTextView playerAutoComplete, eventAutoComplete, roundAutoComplete;
    private TextInputLayout playerSpinnerLayout, eventSpinnerLayout, roundSpinnerLayout;


    // Timer State
    private boolean isRunning = false;
    private long startTime = 0L;
    private Handler timerHandler = new Handler();
    private long timeInMillis = 0L;

    // Data
    private List<Player> sortedPlayerList;
    private Player selectedPlayer;
    private int selectedEvent = 1;
    private int selectedRound = 1;

    private final List<String> timedEventNames = Arrays.asList(
            "Strawpedo", "Race 2 Pint", "Crab Run", "Ping Pong Run"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
        setupButtons();
        setupEventSpinner(); // Initialize the event spinner
    }

    @Override
    protected void onStart() {
        super.onStart();
        PlayerDataManager.getInstance().addListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        PlayerDataManager.getInstance().removeListener(this);
    }

    @Override
    public void onDataUpdated(List<Player> players) {
        runOnUiThread(() -> setupPlayerDropdown(players));
    }

    @Override
    public void onSettingsUpdated(TournamentSettings settings) {
        runOnUiThread(() -> {
            // Hide the round spinner if both R2 and R3 are locked
            if (settings.isRound2Locked() && settings.isRound3Locked()) {
                roundSpinnerLayout.setVisibility(View.GONE);
            } else {
                roundSpinnerLayout.setVisibility(View.VISIBLE);
            }
            setupRoundSpinner(settings);
        });
    }

    private void findViews() {
        timerTextView = findViewById(R.id.textView_timer);
        startStopButton = findViewById(R.id.button_start_stop);
        resetButton = findViewById(R.id.button_reset);
        saveButton = findViewById(R.id.button_save);
        playerAutoComplete = findViewById(R.id.autocomplete_player);
        eventAutoComplete = findViewById(R.id.autocomplete_event);
        roundAutoComplete = findViewById(R.id.autocomplete_round);
        playerSpinnerLayout = findViewById(R.id.layout_player_spinner);
        eventSpinnerLayout = findViewById(R.id.layout_event_spinner);
        roundSpinnerLayout = findViewById(R.id.layout_round_spinner);
    }

    private void setupButtons() {
        startStopButton.setOnClickListener(v -> {
            if (isRunning) {
                stopTimer();
            } else {
                startTimer();
            }
        });

        resetButton.setOnClickListener(v -> resetTimer());

        saveButton.setOnClickListener(v -> saveTime());
    }

    private void setupPlayerDropdown(List<Player> players) {
        sortedPlayerList = new ArrayList<>(players);
        Collections.sort(sortedPlayerList, (p1, p2) -> p1.getName().compareTo(p2.getName()));

        List<String> playerNames = new ArrayList<>();
        for (Player p : sortedPlayerList) {
            playerNames.add(p.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_dropdown_item, playerNames);
        playerAutoComplete.setAdapter(adapter);

        playerAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            selectedPlayer = sortedPlayerList.get(position);
            checkSaveButtonState();
        });

        playerAutoComplete.setOnDismissListener(() -> {
            playerSpinnerLayout.clearFocus();
            playerAutoComplete.clearFocus();
        });
    }

    private void setupEventSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_dropdown_item, timedEventNames);
        eventAutoComplete.setAdapter(adapter);
        eventAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            selectedEvent = position + 1;
        });

        eventAutoComplete.setOnDismissListener(() -> {
            eventSpinnerLayout.clearFocus();
            eventAutoComplete.clearFocus();
        });
    }

    private void setupRoundSpinner(TournamentSettings settings) {
        List<String> rounds = new ArrayList<>();
        rounds.add("Round 1");
        if (!settings.isRound2Locked()) {
            rounds.add("Round 2");
        }
        if (!settings.isRound3Locked()) {
            rounds.add("Round 3");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_dropdown_item, rounds);
        roundAutoComplete.setAdapter(adapter);
        roundAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            // This logic needs to be smarter to handle hidden rounds
            String selectedRoundStr = (String) parent.getItemAtPosition(position);
            if (selectedRoundStr.equals("Round 1")) {
                selectedRound = 1;
            } else if (selectedRoundStr.equals("Round 2")) {
                selectedRound = 2;
            } else if (selectedRoundStr.equals("Round 3")) {
                selectedRound = 3;
            }
        });

        roundAutoComplete.setOnDismissListener(() -> {
            roundSpinnerLayout.clearFocus();
            roundAutoComplete.clearFocus();
        });
    }

    private void startTimer() {
        isRunning = true;
        startTime = SystemClock.uptimeMillis() - timeInMillis; // Resume from where it was paused
        timerHandler.postDelayed(timerRunnable, 0);
        startStopButton.setText("Stop");
        resetButton.setEnabled(false);
        saveButton.setEnabled(false);
    }

    private void stopTimer() {
        isRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
        startStopButton.setText("Start");
        resetButton.setEnabled(true);
        checkSaveButtonState();
    }

    private void resetTimer() {
        timeInMillis = 0L;
        startTime = 0L;
        timerTextView.setText("00.000");
        saveButton.setEnabled(false);

        // Clear dropdown selections
        playerAutoComplete.setText("", false);
        eventAutoComplete.setText("", false);
        roundAutoComplete.setText("", false);
        selectedPlayer = null;
        selectedEvent = 1;
        selectedRound = 1;

        // Clear focus to reset the UI state of the dropdowns
        playerSpinnerLayout.clearFocus();
        eventSpinnerLayout.clearFocus();
        roundSpinnerLayout.clearFocus();
        playerAutoComplete.clearFocus();
        eventAutoComplete.clearFocus();
        roundAutoComplete.clearFocus();
    }

    private void saveTime() {
        if (selectedPlayer == null) {
            Toast.makeText(this, "Please select a player.", Toast.LENGTH_SHORT).show();
            return;
        }

        double scoreInSeconds = timeInMillis / 1000.0;
        String score = String.format(Locale.US, "%.3f", scoreInSeconds);

        selectedPlayer.setScore(selectedRound, selectedEvent, score);
        PlayerDataManager.getInstance().updatePlayer(selectedPlayer);

        Toast.makeText(this, "Time saved for " + selectedPlayer.getName(), Toast.LENGTH_SHORT).show();
        resetTimer();
    }

    private void checkSaveButtonState() {
        saveButton.setEnabled(selectedPlayer != null && timeInMillis > 0 && !isRunning);
    }

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            timeInMillis = SystemClock.uptimeMillis() - startTime;
            int seconds = (int) (timeInMillis / 1000);
            int milliseconds = (int) (timeInMillis % 1000);
            timerTextView.setText(String.format(Locale.getDefault(), "%02d.%03d", seconds, milliseconds));
            if (isRunning) {
                timerHandler.postDelayed(this, 0);
            }
        }
    };
}
