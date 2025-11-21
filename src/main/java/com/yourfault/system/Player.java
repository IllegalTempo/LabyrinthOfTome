package com.yourfault.system;

import com.yourfault.Main;
import com.yourfault.listener.WeaponSelectionListener;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.UUID;

public class Player
{
    private org.bukkit.entity.Player MINECRAFT_PLAYER;
    private float MAX_HEALTH = 100f;
    private float MAX_MANA = 100f;
    private float HEALTH;
    private float MANA;
    public float DEFENSE;
    public WeaponSelectionListener.WeaponType SELECTED_WEAPON = null;

    public Player(org.bukkit.entity.Player minecraftplayer, float MaxHealth, float MaxMana, float Defense, WeaponSelectionListener.WeaponType selectedWeapon)
    {
        this.MINECRAFT_PLAYER = minecraftplayer;
        this.MAX_HEALTH = MaxHealth;
        this.MAX_MANA = MaxMana;
        this.HEALTH = MaxHealth;
        this.MANA = MaxMana;
        this.DEFENSE = Defense;
        this.SELECTED_WEAPON = selectedWeapon;


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




}
