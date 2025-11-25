package com.yourfault.system.GeneralPlayer;

import com.yourfault.Main;
import com.yourfault.weapon.WeaponType;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.Locale;

import static com.yourfault.Main.plugin;
import static com.yourfault.system.BleedoutManager.BLEED_OUT_SECONDS;
import static com.yourfault.system.BleedoutManager.REVIVE_SECONDS;

public class GamePlayer
{
    public static final Vector Downed_WatchOffset = new Vector(0,5,0);
    private static final Title.Times DEATH_TITLE_TIMES = Title.Times.times(
            Duration.ofMillis(300),
            Duration.ofSeconds(2),
            Duration.ofMillis(600)
    );

    public enum SurvivalState {
        ALIVE,
        DOWNED,
        DEAD
    }

    private Player MINECRAFT_PLAYER;
    public Perks PLAYER_PERKS;
    private final float MAX_HEALTH;
    private final float MAX_MANA;
    private float HEALTH;
    private float MANA;
    public float DEFENSE;
    public WeaponType SELECTED_WEAPON = null;
    private int coins = 0;
    private int level = 1;
    private int experience = 0;

    public SurvivalState CurrentState = SurvivalState.ALIVE;
    public GamePlayer Reviving_Someone = null;
    public GamePlayer Being_Revived = null;

    public Mannequin SndPerson;
    private BukkitTask downTask;
    private BukkitTask ReviveTask;

    public GamePlayer(Player minecraftplayer)
    {
        this.MINECRAFT_PLAYER = minecraftplayer;
        this.SELECTED_WEAPON = GetSelectedWeapon_From_Scoreboardtag();
        this.MAX_HEALTH = SELECTED_WEAPON.Health;
        this.MAX_MANA = SELECTED_WEAPON.Mana;
        this.HEALTH = MAX_HEALTH;
        this.MANA = MAX_MANA;
        this.DEFENSE = SELECTED_WEAPON.Defense;
        PLAYER_PERKS = new Perks(this);
        refillVanillaHealth();
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
        Player player = MINECRAFT_PLAYER;
        if (player == null) {
            return;
        }
        int healthPercent = Math.round(GetHealth_Ratio() * 100);
        int manaPercent = Math.round(GetMana_Ratio() * 100);
//        String message = "❤ " + healthPercent + "%  ✦ " + manaPercent + "%  |  L" + level + " " + coins + "C" +
//                "  (" + experience + "/" + xpTarget + " XP)";
        int healthCodePoint = 0x1F1F + healthPercent;
        int manaCodePoint   = 0x1F2F + manaPercent;

        String Preset = ChatColor.of("#000000") + "\u1f01";
        String healthChar = ChatColor.of("#000000") + new String(Character.toChars(healthCodePoint));
        String manaChar   = ChatColor.of("#000000") + new String(Character.toChars(manaCodePoint));




        //String message =  "\u1f00" + String.join("\u1f00",Preset,manaChar,healthChar) + "\u1f00";
        String message =  "\u1f00" + String.join("\u1f00",Preset,manaChar,healthChar) + "\u1f00";

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
    public void damage(float amount)
    {
        if (CurrentState != SurvivalState.ALIVE) {
            return;
        }
        HEALTH -= amount;

        if(HEALTH <= 0) {
            HEALTH = 0;
            start_down();

        } else {
            refillVanillaHealth();
        }
    }
    public Mannequin SpawnSecondPerson()
    {
        return Main.world.spawn(MINECRAFT_PLAYER.getLocation(), Mannequin.class, mannequin -> {
            mannequin.setCustomName(MINECRAFT_PLAYER.getName());
            mannequin.setCustomNameVisible(true);
            mannequin.setInvulnerable(true);
            mannequin.setCollidable(false);
            mannequin.setPose(Pose.SLEEPING);
            mannequin.setProfile(ResolvableProfile.resolvableProfile(MINECRAFT_PLAYER.getPlayerProfile()));
            SndPerson = mannequin;
        });
    }
    public void start_down() {
        if (MINECRAFT_PLAYER == null) {
            return;
        }
        CurrentState = SurvivalState.DOWNED;
        SndPerson = SpawnSecondPerson();
        Location watchcorpse = MINECRAFT_PLAYER.getLocation().clone();
        watchcorpse.add(Downed_WatchOffset);
        watchcorpse.setRotation(0,90);
        MINECRAFT_PLAYER.teleport(watchcorpse);
        MINECRAFT_PLAYER.setGameMode(GameMode.SPECTATOR);
        Bukkit.broadcastMessage(org.bukkit.ChatColor.RED + MINECRAFT_PLAYER.getName() + " is bleeding out! Hold SHIFT on them for 5 seconds to revive.");
        downTask = new BukkitRunnable() {
            private int ticksRemaining = BLEED_OUT_SECONDS * 20;
            @Override
            public void run() {
                if (!MINECRAFT_PLAYER.isOnline()) {
                    cancel();
                    return;
                }
                ticksRemaining -= 20;
                if (ticksRemaining <= 0) {
                    died();
                    cancel();
                } else {
                    int seconds = Math.max(0, ticksRemaining / 20);
                    MINECRAFT_PLAYER.sendActionBar(Component.text("Bleeding out - " + seconds + "s", NamedTextColor.RED));
                    if (SndPerson != null) {
                        SndPerson.setDescription(Component.text("Bleeding out - " + seconds + "s\nHold SHIFT to revive", NamedTextColor.RED));
                    }
                }

            }
        }.runTaskTimer(plugin, 0L, 20L);

        Main.game.CheckWholeFamilyDies();
    }
    public void CancelRevive()
    {
        if(Reviving_Someone != null)
        {
            if(MINECRAFT_PLAYER.isOnline())
            {
                MINECRAFT_PLAYER.sendMessage(org.bukkit.ChatColor.YELLOW + "You Cancelled the revive.");

            }
            Reviving_Someone.Being_Revived = null;
            if (ReviveTask != null) {
                ReviveTask.cancel();
                ReviveTask = null;
            }
            Reviving_Someone = null;
        }
    }
    public void beginRevive(GamePlayer targetState) {
        if (CurrentState != SurvivalState.ALIVE) {
            return;
        }
        if (targetState == null || targetState.CurrentState != SurvivalState.DOWNED) {
            MINECRAFT_PLAYER.sendMessage(org.bukkit.ChatColor.RED + "Only bleeding teammates can be revived.");
            return;
        }
        if (Reviving_Someone != null) {
            return;
        }
        if (targetState.Being_Revived != null) {
            MINECRAFT_PLAYER.sendMessage(org.bukkit.ChatColor.YELLOW + "Another teammate is already reviving them.");
            return;
        }
        Reviving_Someone = targetState;
        targetState.Being_Revived = this;
        ReviveTask = new BukkitRunnable() {
            private int ticksRemaining = REVIVE_SECONDS * 20;
            @Override
            public void run() {
                if (!MINECRAFT_PLAYER.isOnline()) {
                    cancel();
                    return;
                }
                ticksRemaining -= 20;
                if (ticksRemaining <= 0) {
                    targetState.revived();
                    cancel();
                } else {
                    int seconds = Math.max(0, ticksRemaining / 20);
                    MINECRAFT_PLAYER.sendActionBar(Component.text("Reviving " + targetState.MINECRAFT_PLAYER.getName() + " - " + seconds + "s", NamedTextColor.GREEN));
                }

            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void revived()
    {
        CurrentState = SurvivalState.ALIVE;
        if (Being_Revived != null) {
            Being_Revived.Reviving_Someone = null;
            Being_Revived = null;
        }
        HEALTH = MAX_HEALTH * 0.2f;
        refillVanillaHealth();
        if (MINECRAFT_PLAYER != null) {
            MINECRAFT_PLAYER.setGameMode(GameMode.ADVENTURE);
            if (SndPerson != null) {
                Location reviveLocation = SndPerson.getLocation().clone();
                reviveLocation.setPitch(0f);
                MINECRAFT_PLAYER.teleport(reviveLocation);
            }
        }
        if (downTask != null) {
            downTask.cancel();
            downTask = null;
        }
        if (SndPerson != null) {
            SndPerson.remove();
            SndPerson = null;
        }
    }
    public void died()
    {
        CurrentState = SurvivalState.DEAD;
        if(Being_Revived == null)
        {


        } else {
            Being_Revived.CancelRevive();

        }
        if (SndPerson != null) {
            SndPerson.setDescription(Component.text("DEAD - Revive next wave", NamedTextColor.DARK_RED));
        }
        Player player = MINECRAFT_PLAYER;
        if (player != null) {
            Title deathTitle = Title.title(
                    Component.text("You Died", NamedTextColor.RED),
                    Component.text("Revive next dimension", NamedTextColor.DARK_RED),
                    DEATH_TITLE_TIMES
            );
            player.showTitle(deathTitle);
        }
    }

    public void setMinecraftPlayer(Player minecraftPlayer) {
        this.MINECRAFT_PLAYER = minecraftPlayer;
    }

    public Player getMinecraftPlayer() {
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
        if (leveled && MINECRAFT_PLAYER != null) {
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
        CurrentState = SurvivalState.ALIVE;
        if (ReviveTask != null) {
            ReviveTask.cancel();
            ReviveTask = null;
        }
        if (downTask != null) {
            downTask.cancel();
            downTask = null;
        }
        if (Reviving_Someone != null) {
            Reviving_Someone.Being_Revived = null;
            Reviving_Someone = null;
        }
        if (Being_Revived != null) {
            Being_Revived.Reviving_Someone = null;
            Being_Revived = null;
        }
        Player player = MINECRAFT_PLAYER;
        if (player != null) {
            if (SndPerson != null) {
                Location reviveLocation = SndPerson.getLocation().clone();
                reviveLocation.setPitch(0f);
                player.teleport(reviveLocation);
            }
            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(false);
            player.setFlying(false);
            refillVanillaHealth();
        }
        if(SndPerson != null) {
            SndPerson.remove();
            SndPerson = null;
        }
        PLAYER_PERKS.removePerks();
    }

    public void refillVanillaHealth() {
        Player player = MINECRAFT_PLAYER;
        if (player == null) {
            return;
        }
        double maxHealth = player.getMaxHealth();
        player.setHealth(maxHealth);
    }
}
