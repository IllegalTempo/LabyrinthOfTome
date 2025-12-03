package com.yourfault.wave;

import com.yourfault.Enemy.mob.WaveEnemyType;

public interface WaveLifecycleListener {
    void onEncounterStarted(int waveNuber, WaveEncounterType type, int enemyCount);
    void onEncounterCompleted(int waveNumber, WaveEncounterType type);
}
