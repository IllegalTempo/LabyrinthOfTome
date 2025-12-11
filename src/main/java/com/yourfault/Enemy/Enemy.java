package com.yourfault.Enemy;

import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.scheduler.BukkitTask;

import com.yourfault.Enemy.EnemyTypes.AbstractEnemyType;
import com.yourfault.Main;
import static com.yourfault.Main.plugin;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.wave.WaveContext;

public abstract class Enemy {
    public Mob entity;
    public float HEALTH;
    public float DEFENSE;
    public float MaxHealth;
    private final long updateTime;
    public AbstractEnemyType enemyType;
    public WaveContext context;

    BukkitTask updateTask;
    BukkitTask tickTask;
    public float scale = 1;

    public float damageMultiplier = 1.0f;

    public Enemy(Mob entity,WaveContext context,AbstractEnemyType type)
    {
        this(entity,type.getScaledHealth(context),type.getScaledDefense(context),type.getDamageMultipler(context),20L,type,context);

    }
    public Enemy(Mob entity,WaveContext context,long updateTime,AbstractEnemyType type)
    {
        this(entity,type.getScaledHealth(context),type.getScaledDefense(context),type.getDamageMultipler(context),updateTime,type,context);

    }
    public Enemy(Mob entity, float MaxHealth, float defense, float damageMultiplier,long updateTime,AbstractEnemyType enemyType,WaveContext context)
    {
        this.context = context;

        this.HEALTH = MaxHealth;
        this.MaxHealth = MaxHealth;
        this.DEFENSE = defense;
        this.entity = entity;
        this.damageMultiplier = damageMultiplier;
        this.enemyType = enemyType;
        entity.setCustomNameVisible(true);
        updateDisplay();
        this.updateTime = updateTime;


        Main.game.ENEMY_LIST.put(entity.getUniqueId(),this);
        startUpdate();
        Main.game.EnemyTeam.addEntity(entity);
        entity.setGlowing(true);
        entity.addScoreboardTag("lot_wave_enemy");
        entity.addScoreboardTag("lot_wave_enemy_" + enemyType.displayName.toLowerCase(Locale.ROOT));
    }
    protected void updateDisplay() {

        String label = ChatColor.RED + String.format(Locale.US, "%.0f/%.0f HP ", HEALTH, MaxHealth)
                + ChatColor.GRAY + enemyType.displayName;
        entity.setCustomName(label);
        if(isBoss())
        {
            Main.game.BossHealthBar.progress(HEALTH/MaxHealth);
        }

    }
    protected final GamePlayer getNearestPlayer()
    {
        double nearestDistance = Double.MAX_VALUE;
        GamePlayer nearestPlayer = null;
        for(GamePlayer gp : Main.game.PLAYER_LIST.values())
        {
            double distance = getDistance(gp.MINECRAFT_PLAYER);
            if(distance < nearestDistance)
            {
                nearestDistance = distance;
                nearestPlayer = gp;
            }
        }
        return nearestPlayer;
    }
    public final float getDistance(Entity e)
    {
        return (float) e.getLocation().distance(entity.getLocation());
    }
    private boolean isBoss()
    {
        return enemyType.isBoss;
    }
    public void startUpdate()
    {
        updateTask= Bukkit.getScheduler().runTaskTimer(plugin,()->{
            update();
        },0L,updateTime);
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin,()->{
            tick();
            if(scale > 1)
            {
                entity.getAttribute(Attribute.SCALE).setBaseValue(1);
            }
        },0L,1L);
    }
    public abstract void update();
    public abstract void tick();
    public abstract void OnAttack();
    public abstract void OnDealDamage();
    public void OnBeingDamage(float damage, GamePlayer damageDealer)
    {
        HEALTH -= damage;
        entity.damage(0);
        updateDisplay();
        if(damageDealer != null)
        {
            damageDealer.onDoDamage(this,damage);

        }

        entity.getAttribute(Attribute.SCALE).setBaseValue(1.1);
        scale = 1.1f;
        entity.getWorld().spawnParticle(Particle.BLOCK, entity.getLocation().add(0,1,0),50,0.3,0.6,0.3,0, BlockType.REDSTONE_BLOCK.createBlockData() );


        if(HEALTH <= 0)
        {
            Destroy(damageDealer);
        }
    }
    public void Destroy()
    {
        Destroy(null);
    }
    public void Destroy(GamePlayer killer)
    {
        Main.game.ENEMY_LIST.remove(entity.getUniqueId());
        if (Main.game.getWaveManager() != null) {
            Main.game.getWaveManager().handleEnemyDeath(entity.getUniqueId(), killer);
        }
        if(updateTask != null)
        {
            updateTask.cancel();
            updateTask = null;
        }
        if(tickTask != null)
        {
            tickTask.cancel();
            tickTask = null;
        }
        Main.game.onEnemyKilled(this);
        entity.remove();

    }
}
