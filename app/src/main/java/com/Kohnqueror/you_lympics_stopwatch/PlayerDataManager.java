package com.Kohnqueror.you_lympics_stopwatch;

import android.util.Log;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PlayerDataManager {

    private static final String TAG = "PlayerDataManager";
    private static PlayerDataManager instance;
    private final List<Player> playerList = new CopyOnWriteArrayList<>();
    private TournamentSettings tournamentSettings = new TournamentSettings();
    private final FirebaseFirestore db;
    private ListenerRegistration playerListenerReg;
    private ListenerRegistration settingsListenerReg;
    private final List<PlayerDataListener> listeners = new CopyOnWriteArrayList<>();

    private static final List<String> PLAYER_NAMES = Arrays.asList(
            "Callum", "Carrie", "Charlotte", "Conor", "Dave",
            "Jamie", "Joel", "Oscar", "Peter", "Tim"
    );

    private PlayerDataManager() {
        db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);
        startListeningForUpdates();
    }

    public static synchronized PlayerDataManager getInstance() {
        if (instance == null) {
            instance = new PlayerDataManager();
        }
        return instance;
    }

    public void addListener(PlayerDataListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
        if (!playerList.isEmpty()) {
            listener.onDataUpdated(new ArrayList<>(playerList));
        }
        if (tournamentSettings != null) {
            listener.onSettingsUpdated(tournamentSettings);
        }
    }

    public void removeListener(PlayerDataListener listener) {
        listeners.remove(listener);
    }

    private void startListeningForUpdates() {
        CollectionReference playersCollection = db.collection("players");
        playerListenerReg = playersCollection.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.w(TAG, "Player listen failed.", e);
                return;
            }
            if (snapshots != null && !snapshots.isEmpty()) {
                playerList.clear();
                for (int i = 0; i < snapshots.getDocuments().size(); i++) {
                    Player player = snapshots.getDocuments().get(i).toObject(Player.class);
                    if (player != null) {
                        player.setId(snapshots.getDocuments().get(i).getId());
                        playerList.add(player);
                    }
                }
                notifyDataListeners();
            } else {
                createInitialData();
            }
        });

        DocumentReference settingsDoc = db.collection("settings").document("tournament_config");
        settingsListenerReg = settingsDoc.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Settings listen failed.", e);
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                tournamentSettings = snapshot.toObject(TournamentSettings.class);
            } else {
                tournamentSettings = new TournamentSettings();
                updateTournamentSettings(tournamentSettings);
            }
            notifySettingsListeners();
        });
    }

    private void notifyDataListeners() {
        for (PlayerDataListener listener : listeners) {
            listener.onDataUpdated(new ArrayList<>(playerList));
        }
    }

    private void notifySettingsListeners() {
        if (tournamentSettings != null) {
            for (PlayerDataListener listener : listeners) {
                listener.onSettingsUpdated(tournamentSettings);
            }
        }
    }

    public void updateTournamentSettings(TournamentSettings settings) {
        this.tournamentSettings = settings;
        db.collection("settings").document("tournament_config").set(settings)
                .addOnFailureListener(e -> Log.w(TAG, "Error updating settings", e));
    }

    private void createInitialData() {
        CollectionReference playersCollection = db.collection("players");
        for (String name : PLAYER_NAMES) {
            Player player = new Player(name);
            // Use the player's name as the unique document ID
            String playerId = name.toLowerCase();
            player.setId(playerId);
            player.calculateTotalPoints();
            // Use .document(id).set(player) instead of .add(player)
            playersCollection.document(playerId).set(player);
        }
    }

    public void updatePlayer(Player player) {
        if (player != null && player.getId() != null) {
            player.calculateTotalPoints();
            db.collection("players").document(player.getId()).set(player)
                    .addOnFailureListener(err -> Log.w(TAG, "Error updating player", err));
        }
    }

    public void restorePlayers(List<Player> playersToRestore) {
        for (Player player : playersToRestore) {
            updatePlayer(player);
        }
    }

    public void resetAllData() {
        for (Player player : playerList) {
            Player freshPlayer = new Player(player.getName());
            freshPlayer.setId(player.getId());
            freshPlayer.calculateTotalPoints();
            updatePlayer(freshPlayer);
        }
        updateTournamentSettings(new TournamentSettings());
    }

    public Player getPlayerById(String id) {
        for (Player p : playerList) {
            if (p.getId() != null && p.getId().equals(id)) {
                return p;
            }
        }
        return null;
    }

    public List<Player> getPlayerList() {
        return new ArrayList<>(playerList);
    }

    public interface PlayerDataListener {
        void onDataUpdated(List<Player> players);
        void onSettingsUpdated(TournamentSettings settings);
    }
}
