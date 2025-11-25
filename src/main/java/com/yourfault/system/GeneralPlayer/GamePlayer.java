package com.yourfault.system.GeneralPlayer;

import com.yourfault.Main;
import com.yourfault.system.BleedoutManager;
import com.yourfault.weapon.WeaponType;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.title.TitlePart;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Objects;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static com.yourfault.Main.plugin;
import static com.yourfault.system.BleedoutManager.BLEED_OUT_SECONDS;
import static com.yourfault.system.BleedoutManager.REVIVE_SECONDS;

public class GamePlayer
{
    public static final Vector Downed_WatchOffset = new Vector(0,5,0);
    public enum SurvivalState {
        ALIVE,
        DOWNED,
        DEAD
    }
    private Player MINECRAFT_PLAYER;
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
    public void damage(float amount)
    {
        HEALTH -= amount;
        MINECRAFT_PLAYER.damage(0);

        if(HEALTH < 0) {
            HEALTH = 0;
            start_down();

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
        //BleedoutManager.BleedoutState state = new BleedoutManager.BleedoutState(player);
        //applyDownedEffects(player, gamePlayer, state);
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
                    SndPerson.setDescription(Component.text("Bleeding out - " + seconds + "s\nHold SHIFT to revive", NamedTextColor.RED));}

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
            ReviveTask.cancel();
            Reviving_Someone = null;
        }
    }
    public void beginRevive(GamePlayer targetState) {
        if (Reviving_Someone != null) {
            //MINECRAFT_PLAYER.sendMessage(org.bukkit.ChatColor.YELLOW + "You are already reviving someone.");
            return;
        }
        if (targetState.Being_Revived != null) {
            MINECRAFT_PLAYER.sendMessage(org.bukkit.ChatColor.YELLOW + "Another teammate is already reviving them.");
            return;
        }
        Reviving_Someone = targetState;
        targetState.Being_Revived = this;
//        if (!isWithinRange(rescuer, targetState.playerId)) {
//            rescuer.sendMessage(org.bukkit.ChatColor.RED + "Move closer to revive.");
//            return;
//        }
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
        Being_Revived.Reviving_Someone = null;
        Being_Revived = null;
        HEALTH = MAX_HEALTH * 0.3f;
        MINECRAFT_PLAYER.setGameMode(GameMode.ADVENTURE);
        downTask.cancel();
        SndPerson.remove();
    }
    public void died()
    {
        CurrentState = SurvivalState.DEAD;
        if(Being_Revived == null) //no one saving you
        {


        } else { //you died in someones hand
            Being_Revived.CancelRevive();

        }
        SndPerson.setDescription(Component.text("DEAD - Revive next wave", NamedTextColor.DARK_RED));
        MINECRAFT_PLAYER.sendTitlePart(TitlePart.TITLE, Component.text("You Died", NamedTextColor.RED));
        MINECRAFT_PLAYER.sendTitlePart(TitlePart.SUBTITLE, Component.text("Revive next dimension", NamedTextColor.DARK_RED));
        //TODO handle death logic (What is death?)
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
        CurrentState = SurvivalState.ALIVE;
        if(SndPerson != null) {
            SndPerson.remove();
        }
        PLAYER_PERKS.removePerks();
        if(downTask != null)downTask.cancel();
        if(ReviveTask != null)ReviveTask.cancel();

    }

//    private static final class DownedVisual {
//        private boolean active;
//        private boolean hadGravity;
//        private boolean allowedFlight;
//        private boolean wasFlying;
//        private boolean wasSleepingIgnored;
//        private Pose previousPose;
//        private BukkitTask poseTask;
//        private BooleanSupplier validator;
//
//        void start(JavaPlugin plugin, GamePlayer gamePlayer, BooleanSupplier stillDowned) {
//            Player player = gamePlayer.getMinecraftPlayer();
//            if (player == null) {
//                return;
//            }
//            stop(gamePlayer);
//            active = true;
//            validator = stillDowned;
//            hadGravity = player.hasGravity();
//            allowedFlight = player.getAllowFlight();
//            wasFlying = player.isFlying();
//            wasSleepingIgnored = player.isSleepingIgnored();
//            previousPose = player.getPose();
//            player.setGravity(false);
//            player.setAllowFlight(false);
//            player.setFlying(false);
//            player.setSneaking(false);
//            player.setSleepingIgnored(true);
//            applyPose(player);
//            poseTask = new BukkitRunnable() {
//                @Override
//                public void run() {
//                    Player tracked = gamePlayer.getMinecraftPlayer();
//                    if (tracked == null || !tracked.isOnline()) {
//                        restore(gamePlayer, false);
//                        cancel();
//                        return;
//                    }
//                    if (validator != null && !validator.getAsBoolean()) {
//                        restore(gamePlayer, false);
//                        cancel();
//                        return;
//                    }
//                    applyPose(tracked);
//                }
//            }.runTaskTimer(plugin, 0L, 1L);
//        }
//
//        void stop(GamePlayer gamePlayer) {
//            restore(gamePlayer, true);
//        }
//
//        boolean isActive() {
//            return active;
//        }
//
//        private void restore(GamePlayer gamePlayer, boolean cancelTask) {
//            if (poseTask != null && cancelTask) {
//                poseTask.cancel();
//            }
//            poseTask = null;
//            if (!active) {
//                return;
//            }
//            active = false;
//            Player player = gamePlayer.getMinecraftPlayer();
//            if (player == null) {
//                return;
//            }
//            player.setGravity(hadGravity);
//            player.setAllowFlight(allowedFlight);
//            if (allowedFlight) {
//                player.setFlying(wasFlying);
//            }
//            player.setSleepingIgnored(wasSleepingIgnored);
//            try {
//                player.setPose(previousPose != null ? previousPose : Pose.STANDING);
//            } catch (NoSuchMethodError ignored) {
//                // ignore
//            }
//        }
//
//        private void applyPose(Player player) {
//            try {
//                player.setPose(Pose.SLEEPING);
//            } catch (NoSuchMethodError ignored) {
//                try {
//                    player.setPose(Pose.SWIMMING);
//                } catch (NoSuchMethodError ignored2) {
//                    player.setPose(Pose.SNEAKING);
//                }
//            }
//        }
//    }
}
