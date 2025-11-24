package com.yourfault.system.GeneralPlayer;

import com.yourfault.Main;
import com.yourfault.weapon.WeaponType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.awt.*;

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
//        String message = "❤ " + healthPercent + "%  ✦ " + manaPercent + "%  |  L" + level + " " + coins + "C" +
//                "  (" + experience + "/" + xpTarget + " XP)";
        int healthCodePoint = 0x1F1F + healthPercent;
        int manaCodePoint   = 0x1F2F + manaPercent;

        String Preset = ChatColor.of("#000000") + "\u1f01";
        String healthChar = ChatColor.of("#000000") + new String(Character.toChars(healthCodePoint));
        String manaChar   = ChatColor.of("#000000") + new String(Character.toChars(manaCodePoint));




        //String message =  "\u1f00" + String.join("\u1f00",Preset,manaChar,healthChar) + "\u1f00";
        String message = "\u2f01".repeat(healthPercent);

        Component component = GsonComponentSerializer.gson().deserialize(
                "{\"text\":\"" + message + "\",\"shadow_color\":0}"
        );
        MINECRAFT_PLAYER.sendActionBar(component);


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

    public float getMaxHealth() {
        return MAX_HEALTH;
    }

    public void setHealth(float amount) {
        HEALTH = Math.max(0, Math.min(amount, MAX_HEALTH));
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

    public void setCoins(int amount) {
        coins = Math.max(0, amount);
    }

    public boolean spendCoins(int amount) {
        if (amount <= 0) {
            return true;
        }
        if (coins < amount) {
            return false;
        }
        coins -= amount;
        return true;
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

    public void resetProgress() {
        level = 1;
        coins = 0;
        experience = 0;
        HEALTH = MAX_HEALTH;
        MANA = MAX_MANA;
        PLAYER_PERKS.removePerks();

    }

}
