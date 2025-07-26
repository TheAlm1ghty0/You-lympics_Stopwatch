package com.Kohnqueror.you_lympics_stopwatch;

/**
 * A data model for storing global tournament settings in Firestore.
 */
public class TournamentSettings {
    private boolean round2Locked = true; // Default to locked
    private boolean round3Locked = true; // Default to locked

    // Public no-argument constructor is required for Firestore
    public TournamentSettings() {}

    public boolean isRound2Locked() {
        return round2Locked;
    }

    public void setRound2Locked(boolean round2Locked) {
        this.round2Locked = round2Locked;
    }

    public boolean isRound3Locked() {
        return round3Locked;
    }

    public void setRound3Locked(boolean round3Locked) {
        this.round3Locked = round3Locked;
    }
}
