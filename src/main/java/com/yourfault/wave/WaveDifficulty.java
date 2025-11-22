package com.yourfault.wave;

public enum WaveDifficulty {
    EASY("Easy", 0.0, 0.9),
    MEDIUM("Medium", 5.0, 1.0),
    HARD("Hard", 12.0, 1.15);

    private final String displayName;
    private final double modeBonus;
    private final double difficultyScale;

    WaveDifficulty(String displayName, double modeBonus, double difficultyScale) {
        this.displayName = displayName;
        this.modeBonus = modeBonus;
        this.difficultyScale = difficultyScale;
    }

    public String displayName() {
        return displayName;
    }

    public double modeBonus() {
        return modeBonus;
    }

    public double difficultyScale() {
        return difficultyScale;
    }
}
