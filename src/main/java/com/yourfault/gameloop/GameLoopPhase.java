package com.yourfault.gameloop;

public enum GameLoopPhase {
    IDLE,
    INITIAL_READY,
    MAP_GENERATING,
    WAVES_ACTIVE,
    BOSS_GATHER,
    BOSS_TRANSITION,
    BOSS_FIGHT,
    POST_BOSS_READY,
    ENDED
}
