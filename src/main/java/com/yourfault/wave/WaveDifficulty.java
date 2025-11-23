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

    public static WaveDifficulty fromInput(String input) {
        if (input == null || input.isEmpty()) {
            return EASY;
        }
        String normalized = input.trim().toUpperCase();
        String trimmed = input.trim();
        for (WaveDifficulty difficulty : values()) {
            if (difficulty.name().equalsIgnoreCase(normalized) ||
                difficulty.displayName().equalsIgnoreCase(trimmed)) {
                return difficulty;
            }
        }
        return EASY;
    }

    public static String optionsList() {
        StringBuilder builder = new StringBuilder();
        for (WaveDifficulty difficulty : values()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(difficulty.displayName());
        }
        return builder.toString();
    }
}
