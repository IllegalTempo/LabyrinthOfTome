package com.yourfault.system.GeneralPlayer;

import com.yourfault.CustomGUI.CustomGUI;
import com.yourfault.CustomGUI.GUIComponent;
import com.yourfault.Main;
import com.yourfault.Enemy.Enemy;
import com.yourfault.perks.PerkObject;
import com.yourfault.system.TabInfo;
import com.yourfault.utils.AnimationInfo;
import com.yourfault.utils.ItemUtil;
import com.yourfault.weapon.WeaponType;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.minecraft.network.protocol.Packet;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.yourfault.Main.plugin;
import static com.yourfault.system.BleedoutManager.BLEED_OUT_SECONDS;
import static com.yourfault.system.BleedoutManager.REVIVE_SECONDS;
import static com.yourfault.utils.ItemUtil.PlayAnimation;

public class GamePlayer
{
    private final CustomGUI playerGUI;

    public static final Vector Downed_WatchOffset = new Vector(0,5,0);
    private static final Title.Times DEATH_TITLE_TIMES = Title.Times.times(
            Duration.ofMillis(300),
            Duration.ofSeconds(2),
            Duration.ofMillis(600)
    );
    private static final Title.Times NO_FADE = Title.Times.times(
            Duration.ZERO,
            Duration.ofSeconds(1),
            Duration.ofMillis(200)
    );

    public enum SurvivalState {
        ALIVE,
        DOWNED,
        DEAD
    }

    public final Player MINECRAFT_PLAYER;
    public Perks PLAYER_PERKS;
    public final TabManager PLAYER_TAB;
    private final float MAX_HEALTH;
    private final float MAX_MANA;
    private float HEALTH;
    private float MANA;
    public float DEFENSE;
    public float Speed = 100;
    public float AttackSpeed = 100;
    public float flatDamageBonus = 0;
    public float bowDamageBonus = 0; //temp for bow dmg buffs (sharpshooter
    public WeaponType SELECTED_WEAPON = null;
    private int coins = 0;
    private int level = 1;
    private int experience = 0;
    private int perkSelectionTokens = 0;
    public long inActionTicks = 0;
    private float temporarySpeedBonus = 0;
    private int speedBoostTicks = 0;
    public SurvivalState CurrentState = SurvivalState.ALIVE;
    public GamePlayer Reviving_Someone = null;
    public GamePlayer Being_Revived = null;

    public Mannequin SndPerson;
    private BukkitTask downTask;
    private BukkitTask ReviveTask;

    //Perk Stats
    public int projectileMultiplier = 1;
    public float projectileSizeMultiplier = 1.0f;
    public float damageMultiplier = 1.0f;
    public float manaRegenRate = 0.2f;



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
        PLAYER_TAB = new TabManager(this);
        MINECRAFT_PLAYER.activeBossBars().forEach(MINECRAFT_PLAYER::hideBossBar);
        recalculateStats();
        playerGUI = new CustomGUI(InitializeGUI());
        refillVanillaHealth();
    }
    private List<GUIComponent> InitializeGUI()
    {
        List<GUIComponent> actionBarComponents = new ArrayList<>();

        actionBarComponents.add(new GUIComponent(128,"\ue101",0,-180));
        actionBarComponents.add(new GUIComponent(128,"\ue000",0,-180));
        actionBarComponents.add(new GUIComponent(128,"\u1f00",0,-180));



        return actionBarComponents;
    }

    public void recalculateStats() {
        this.DEFENSE = SELECTED_WEAPON.Defense;
        this.Speed = 100;
        this.AttackSpeed = 100;
        this.flatDamageBonus = 0;
        this.bowDamageBonus = 0;

        //apply perk stats (perk bonus buff -> gameplayer)
        if(PLAYER_PERKS != null) {
            for (PerkObject perk : PLAYER_PERKS.perks) {
                perk.perkType.applyStats(this, perk.getLevel());
            }
        }

        updatePlayerSpeed();
    }

    public void Update()
    {
        DisplayStatToPlayer();
        if(inActionTicks > 0)
        {
            inActionTicks--;
        }

        MANA += manaRegenRate;
        if(MANA > MAX_MANA) MANA = MAX_MANA;
        if (speedBoostTicks > 0) {
            speedBoostTicks--;
            if (speedBoostTicks <= 0) {
                temporarySpeedBonus = 0;
                updatePlayerSpeed();
            }
        }
    }

    public void applySpeedBoost(float amount, int ticks) {
        this.temporarySpeedBonus = amount;
        this.speedBoostTicks = ticks;
        updatePlayerSpeed();
    }

    private void updatePlayerSpeed() {
        if (MINECRAFT_PLAYER == null) return;
        float totalSpeed = Speed + temporarySpeedBonus;

        // Use Attribute.MOVEMENT_SPEED to ensure it applies to both walking and sprinting
        // Base attribute value for players is 0.1.
        // 100 Speed => 0.1 attribute value (default)
        double attributeValue = (totalSpeed / 100.0) * 0.1;

        var attribute = MINECRAFT_PLAYER.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attribute != null) {
            MINECRAFT_PLAYER.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(attributeValue);
        }

    }

    public Location getLocationRelativeToPlayer(Vector offset)
    {
        // Convert a local offset (x = right, y = up, z = forward) into world coordinates
        Location eye = MINECRAFT_PLAYER.getEyeLocation();

        // Full forward vector (includes pitch)
        Vector forward = eye.getDirection().clone();
        if (forward.lengthSquared() < 1e-6) {
            // Fallback if direction is somehow zero
            forward = new Vector(0, 0, -1);
        } else {
            forward.normalize();
        }

        // Up is world up
        Vector up = new Vector(0, 1, 0);

        // Right = forward x up. If forward is nearly parallel to up (looking straight up/down)
        // the cross product will be very small; in that case use the horizontal forward to compute right
        Vector right = forward.clone().crossProduct(up);

            right.normalize();


        // Compose world offset: forward * z + right * x + up * y
        Vector worldOffset = forward.multiply(offset.getZ())
                .add(right.multiply(offset.getX()))
                .add(up.multiply(offset.getY()));

        // Return a new Location (don't mutate the player's eye location)
        return eye.clone().add(worldOffset);
    }
    public void onGameStart()
    {

    }
    public void onWaveStart(int NextWave)
    {
        MINECRAFT_PLAYER.showBossBar(Main.game.MonsterRemaining);
    }
    public void onBossStart()
    {
        MINECRAFT_PLAYER.hideBossBar(Main.game.MonsterRemaining);
        MINECRAFT_PLAYER.showBossBar(Main.game.BossHealthBar);
    }
    public void onBossEnd()
    {
        grantPerkSelectionTokens(1);
        if (MINECRAFT_PLAYER != null) {
            MINECRAFT_PLAYER.sendMessage(ChatColor.AQUA + "Boss reward: +1 perk selection token (" + getPerkSelectionTokens() + " total).");
        }
        MINECRAFT_PLAYER.hideBossBar(Main.game.BossHealthBar);


    }
    public void sendPacket(Packet packet)
    {
        ((CraftPlayer) MINECRAFT_PLAYER).getHandle().connection.send(packet);

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
    public void playAnimation(String animationName, long durationTicks)
    {
        inActionTicks = durationTicks;
        ItemStack itemInHand = MINECRAFT_PLAYER.getInventory().getItemInMainHand();
        if (itemInHand == null || itemInHand.getItemMeta() == null) {
            return;
        }
        ItemMeta meta = PlayAnimation(itemInHand.getItemMeta(), animationName, durationTicks);
        MINECRAFT_PLAYER.getInventory().getItemInMainHand().setItemMeta(meta);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            itemInHand.setItemMeta(ItemUtil.SetCustomModelData(itemInHand.getItemMeta(),1,"0"));

        }, durationTicks);
    }
    public void playAnimation(AnimationInfo info)
    {
        playAnimation(info.animationName(), info.durationTicks());
    }
    public Location GetForward(double multiplier)
    {
        return MINECRAFT_PLAYER.getEyeLocation().add(MINECRAFT_PLAYER.getLocation().getDirection().multiply(multiplier));

    }
    public void DisplayStatToPlayer()
    {
//        Player player = MINECRAFT_PLAYER;
//        if (player == null) {
//            return;
//        }
        int healthPercent = Math.round(GetHealth_Ratio() * 100);
        int manaPercent = Math.round(GetMana_Ratio() * 100);
//
        int healthCodePoint = 0x1F00 + healthPercent;
        int manaCodePoint   = 0xe000 + manaPercent;
//
//        TextColor kyColor = TextColor.color(78, 108, 128);
//
//        Component manaChar = Component.text(new String(Character.toChars(manaCodePoint))).color(kyColor);
//        Component healthChar = Component.text(new String(Character.toChars(healthCodePoint))).color(kyColor);
//        Component weaponIcon = Component.text(SELECTED_WEAPON.weaponIcon).color(TextColor.color(78,112,128));
//        Component infoDisplay = Component.text("\u2f01").color(TextColor.color(78,140,128));
//        Component moneydisplay = Component.text(coins).color(TextColor.color(78,172,106));
//
//
//        // Build the action bar component and apply the bitmap font
//        Component actionBar = Component.text("").font(Key.key("minecraft:bitmaps"))
//                .append(manaChar)
//                .append(Component.text("\uff00")) // keep separators as before
//                .append(healthChar)
//                .append(Component.text("\uff00"))
//                .append(weaponIcon)
//                .append(Component.text("\uff00"))
//                .append(infoDisplay)
//                .append(Component.text("\uff00"))
//                .append(moneydisplay);
//
//        player.sendActionBar(actionBar);
        //1 = hb 2 = mb
        playerGUI.getActionBarComponent(1).setCharacter(String.valueOf((char)healthCodePoint));
        playerGUI.getActionBarComponent(2).setCharacter(String.valueOf((char)manaCodePoint));


        playerGUI.DisplayActionBar(MINECRAFT_PLAYER);
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

        float damageMultiplier = 100.0f / (100.0f + DEFENSE);
        float actualDamage = amount * damageMultiplier;

        HEALTH -= actualDamage;

        if(HEALTH <= 0) {
            HEALTH = 0;
            start_down();

        } else {
            refillVanillaHealth();
        }
    }

    public void onDoDamage(Enemy enemy, float damage) {
        if (Main.game.getWaveManager() != null) {
            Main.game.getWaveManager().handleEnemyHit(enemy.entity.getUniqueId(), this);
        }

    }
    public boolean ActionReady(WeaponType weapon,float manaCost)
    {
        if(inActionTicks > 0) return false;
        if(SELECTED_WEAPON != weapon) return false;

        if(MINECRAFT_PLAYER.getInventory().getItemInMainHand().getItemMeta() == null) return false;
        List<String> customData = MINECRAFT_PLAYER.getInventory().getItemInMainHand().getItemMeta().getCustomModelDataComponent().getStrings();
        if(customData.size() == 0) return false;
        String itemname = customData.get(0);
        if(!itemname.equals(weapon.weaponNBT)) return false;
        if(MANA < manaCost) return false;
        return true;
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
            Main.game.getDeadPlayer.put(mannequin.getUniqueId(),this);
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
        Main.tabInfo.GetTeam.get(TabInfo.TabType.PLAYERLIST_DOWN).addEntry(MINECRAFT_PLAYER.getName());
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
                    Title title;
                    if(Being_Revived == null)
                    {
                         title = Title.title(
                                Component.text("Waiting For Teammates to Revive", NamedTextColor.RED),
                                Component.text("Bleeding out - " + seconds + "s", NamedTextColor.RED),
                                NO_FADE
                        );
                        if (SndPerson != null) {
                            SndPerson.setDescription(Component.text("Bleeding out - " + seconds + "s\nHold SHIFT to revive", NamedTextColor.RED));

                        }

                    } else {
                        title = Title.title(
                                Component.text("Being Revived by " + Being_Revived.MINECRAFT_PLAYER.getName(), NamedTextColor.GREEN),
                                Component.text("Bleeding out - " + seconds + "s", NamedTextColor.RED),
                                NO_FADE
                        );

                    }

                    MINECRAFT_PLAYER.showTitle(title);

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
                    Title title = Title.title(
                            Component.text("Reviving " + targetState.MINECRAFT_PLAYER.getName(), NamedTextColor.GREEN),
                            Component.text(seconds + "s", NamedTextColor.GREEN),
                            NO_FADE
                    );
                    if(targetState.SndPerson != null) {
                        targetState.SndPerson.setDescription(Component.text("Being Revived by " + MINECRAFT_PLAYER.getName() + " - " + seconds + "s", NamedTextColor.GREEN));

                    }
                    MINECRAFT_PLAYER.showTitle(title);
                }

            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    public void revived_someone()
    {
        MINECRAFT_PLAYER.resetTitle();
        Reviving_Someone = null;
    }
    public void revived()
    {


        CurrentState = SurvivalState.ALIVE;
        if (Being_Revived != null) {
            Being_Revived.revived_someone();
            Being_Revived = null;
        }
        HEALTH = MAX_HEALTH * 0.2f;
        refillVanillaHealth();
        if (MINECRAFT_PLAYER != null) {
            Main.tabInfo.GetTeam.get(TabInfo.TabType.PLAYERLIST_ALIVE).addEntry(MINECRAFT_PLAYER.getName());

            MINECRAFT_PLAYER.resetTitle();

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
            if(Main.game.getDeadPlayer.containsKey(SndPerson.getUniqueId()))
            {
                Main.game.getDeadPlayer.remove(SndPerson.getUniqueId());
            }
            SndPerson.remove();

            SndPerson = null;
        }
    }
    public void died()
    {
        CurrentState = SurvivalState.DEAD;
        Main.tabInfo.GetTeam.get(TabInfo.TabType.PLAYERLIST_DEAD).addEntry(MINECRAFT_PLAYER.getName());
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
        int levelsGained = 0;
        while (experience >= xpForNextLevel()) {
            experience -= xpForNextLevel();
            level++;
            levelsGained++;
            grantPerkSelectionTokens(1);
        }
        if (levelsGained > 0 && MINECRAFT_PLAYER != null) {
            MINECRAFT_PLAYER.sendMessage("§6Level Up! You are now level " + level + "!");
            MINECRAFT_PLAYER.sendMessage(org.bukkit.ChatColor.AQUA + "+" + levelsGained + " perk selection" + (levelsGained > 1 ? "s" : "") + " available.");
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

    public void grantPerkSelectionTokens(int amount) {
        if (amount <= 0) {
            return;
        }
        perkSelectionTokens += amount;
    }

    public boolean consumePerkSelectionToken() {
        if (perkSelectionTokens <= 0) {
            return false;
        }
        perkSelectionTokens--;
        return true;
    }

    public int getPerkSelectionTokens() {
        return perkSelectionTokens;
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
        MINECRAFT_PLAYER.hideBossBar(Main.game.BossHealthBar);
        MINECRAFT_PLAYER.hideBossBar(Main.game.MonsterRemaining);
        level = 1;
        coins = 0;
        experience = 0;
        perkSelectionTokens = 0;
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
            Main.tabInfo.GetTeam.get(TabInfo.TabType.PLAYERLIST_ALIVE).addEntry(MINECRAFT_PLAYER.getName());

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
