package com.yourfault.wave;

public class WaveContext {
    private final int waveNumber;
    private final int playerCount;
    private final WaveDifficulty difficulty;
    private final double baseWeight;
    private final double exponentialWeight;
    private final double totalWeightBudget;

    public WaveContext(int waveNumber, int playerCount, WaveDifficulty difficulty, double baseWeight, double exponentialWeight, double totalWeightBudget) {
        this.waveNumber = waveNumber;
        this.playerCount = playerCount;
        this.difficulty = difficulty;
        this.baseWeight = baseWeight;
        this.exponentialWeight = exponentialWeight;
        this.totalWeightBudget = totalWeightBudget;
    }

    public int waveNumber() {
        return waveNumber;
    }

    public int playerCount() {
        return playerCount;
    }

    public WaveDifficulty difficulty() {
        return difficulty;
    }

    public double baseWeight() {
        return baseWeight;
    }

    public double exponentialWeight() {
        return exponentialWeight;
    }

    public double totalWeightBudget() {
        return totalWeightBudget;
    }
}
