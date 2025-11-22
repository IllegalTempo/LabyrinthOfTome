package com.yourfault.system.GeneralPlayer;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import com.yourfault.perk.PerkType;

import com.yourfault.weapon.WeaponType;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.craftbukkit.v1_21_R6.entity.CraftPlayer;

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
        String message = "❤ " + Math.round(GetHealth_Ratio() * 100) + "%  ✦ " + Math.round(GetMana_Ratio() * 100) + "%";
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

}
