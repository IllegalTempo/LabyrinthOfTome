package com.yourfault.system;

import java.util.Random;

import com.yourfault.perks.perkType.ScavengerPerk;
import org.bukkit.entity.Player;

import com.yourfault.perks.PerkType;
import com.yourfault.perks.perkType.VengeancePerk;
import com.yourfault.system.GeneralPlayer.GamePlayer;

/**
 * Centralizes rewarding logic (coins, XP) and perk effects like Scavenger and Vengeance.
 */
public class PlayerRewarder {
    private final Game game;
    private final Random random = new Random();

    public PlayerRewarder(Game game) {
        this.game = game;
    }

    public void rewardPlayer(GamePlayer player, int coins, int xp, boolean isKill) {
        if (player == null) return;

        int finalCoins = Math.max(0, coins);
        boolean scavengerProc = false;

        // Scavenger: 25% chance to double coins if player has the perk
        try {
            if (player.PLAYER_PERKS != null) {
                for (com.yourfault.perks.PerkObject perk : player.PLAYER_PERKS.perks) {
                    if (perk.perkType instanceof ScavengerPerk) {
                        if (random.nextDouble() < 0.25) {
                            finalCoins *= 2;
                            scavengerProc = true;
                        }
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}

        boolean rewarded = false;
        if (finalCoins > 0) {
            player.addCoins(finalCoins);
            rewarded = true;
        }
        if (xp > 0) {
            player.addExperience(xp);
            rewarded = true;
        }

        if (rewarded) {
            sendRewardMessage(player, finalCoins, xp, scavengerProc);
        }

        // Vengeance: only apply on kills
        if (isKill) {
            try {
                PerkType p = game.ALL_PERKS.get("Vengeance");
                if (p instanceof VengeancePerk vp) {
                    int lvl = player.PLAYER_PERKS.getPerkLevel(vp);
                    if (lvl > 0) vp.applyKillBonus(player, lvl);
                }
            } catch (Exception ignored) {}
        }
    }

    private void sendRewardMessage(GamePlayer gamePlayer, int coins, int xp, boolean scavengerProc) {
        Player bukkitPlayer = gamePlayer.getMinecraftPlayer();
        if (bukkitPlayer == null) return;
        String coinMsg = "+" + coins + " coins";
        if (scavengerProc) coinMsg += " (Scavenger)";
        String message = String.format("%s, + %d XP", coinMsg, xp);
        bukkitPlayer.sendMessage(message);
    }
}
