package com.yourfault.system.GeneralPlayer;

import com.yourfault.weapon.WeaponType;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class GamePlayer
{
    private org.bukkit.entity.Player MINECRAFT_PLAYER;
    public Perks PLAYER_PERKS;
    private float MAX_HEALTH;
    private float MAX_MANA;
    private float HEALTH;
    private float MANA;
    public float DEFENSE;
    public WeaponType SELECTED_WEAPON = null;
    private int coins = 0;
    private int level = 1;
    private int experience = 0;

    public GamePlayer(org.bukkit.entity.Player minecraftplayer)
    {
        this.MINECRAFT_PLAYER = minecraftplayer;
        this.SELECTED_WEAPON = GetSelectedWeapon_From_Scoreboardtag();
        this.MAX_HEALTH = SELECTED_WEAPON.Health;
        this.MAX_MANA = SELECTED_WEAPON.Mana;
        this.HEALTH = MAX_HEALTH;
        this.MANA = MAX_MANA;
        this.DEFENSE = SELECTED_WEAPON.Defense;
        PLAYER_PERKS = new Perks(this);
    }
    private WeaponType GetSelectedWeapon_From_Scoreboardtag() {
        return MINECRAFT_PLAYER.getScoreboardTags().stream()
                .filter(tag -> tag.startsWith("SelectedWeapon_"))
                .map(tag -> tag.replace("SelectedWeapon_", ""))
                .findFirst()
                .map(name -> {
                    try {
                        return WeaponType.valueOf(name);
                    } catch (IllegalArgumentException | NullPointerException e) {
                        return WeaponType.Excalibur;
                    }
                })
                .orElse(WeaponType.Excalibur);
    }

    public void DisplayStatToPlayer()
    {
        int healthPercent = Math.round(GetHealth_Ratio() * 100);
        int manaPercent = Math.round(GetMana_Ratio() * 100);
        int xpTarget = xpForNextLevel();
        String message = "❤ " + healthPercent + "%  ✦ " + manaPercent + "%  |  L" + level + " " + coins + "C" +
                "  (" + experience + "/" + xpTarget + " XP)";
        MINECRAFT_PLAYER.spigot().sendMessage(ChatMessageType.ACTION_BAR,new TextComponent(message));

    }
    public void ChangeMana(float amount)
    {
        MANA += amount;
        if(MANA > MAX_MANA) MANA = MAX_MANA;
        if(MANA < 0) MANA = 0;

    }

    public float GetHealth_Ratio()
    {
        return (HEALTH / MAX_HEALTH);
    }
    public float GetMana_Ratio()
    {
        return (MANA / MAX_MANA);
    }
    public float GetHealth()
    {
        return HEALTH;
    }
    public float GetMana()
    {
        return MANA;
    }

    public void setMinecraftPlayer(org.bukkit.entity.Player minecraftPlayer) {
        this.MINECRAFT_PLAYER = minecraftPlayer;
    }

    public org.bukkit.entity.Player getMinecraftPlayer() {
        return MINECRAFT_PLAYER;
    }

    public void addCoins(int amount) {
        if (amount <= 0) {
            return;
        }
        coins += amount;
    }

    public void addExperience(int amount) {
        if (amount <= 0) {
            return;
        }
        experience += amount;
        boolean leveled = false;
        while (experience >= xpForNextLevel()) {
            experience -= xpForNextLevel();
            level++;
            leveled = true;
        }
        if (leveled) {
            MINECRAFT_PLAYER.sendMessage("§6Level Up! You are now level " + level + "!");
        }
    }

    public int getCoins() {
        return coins;
    }

    public int getLevel() {
        return level;
    }

    public int getExperience() {
        return experience;
    }

    private int xpForNextLevel() {
        return 100 + ((level - 1) * 50);
    }

}
