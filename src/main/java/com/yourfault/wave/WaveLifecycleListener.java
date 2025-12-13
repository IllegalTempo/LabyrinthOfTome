package com.yourfault.wave;

public interface WaveLifecycleListener {
    void onEncounterStarted(int waveNumber, WaveEncounterType type, int enemyCount);
    void onEncounterCompleted(int waveNumber, WaveEncounterType type);
}
