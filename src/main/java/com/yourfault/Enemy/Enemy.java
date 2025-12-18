package com.yourfault.Enemy;

import java.util.Locale;

import com.yourfault.system.LabyrinthCreature;
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

public abstract class Enemy extends LabyrinthCreature {
    public Mob entity;
    private final long updateTime;
    public AbstractEnemyType enemyType;
    public WaveContext context;

    BukkitTask updateTask;
    BukkitTask tickTask;
    public float scale = 1;


    public Enemy(Mob entity,WaveContext context,AbstractEnemyType type)
    {
        this(entity,type.getScaledHealth(context),type.getScaledDefense(context),type.getDamageMultipler(context),20L,type,context);

    }
    public Enemy(Mob entity,WaveContext context,long updateTime,AbstractEnemyType type)
    {
        this(entity,type.getScaledHealth(context),type.getScaledDefense(context),type.getDamageMultipler(context),updateTime,type,context);

    }
    public Enemy(Mob entity, float Health, float defense, float damageMultiplier,long updateTime,AbstractEnemyType enemyType,WaveContext context)
    {
        this(entity,Health,0f,defense,damageMultiplier,updateTime,enemyType,context);
    }
    public Enemy(Mob entity, float Health, float Mana,float defense, float damageMultiplier,long updateTime,AbstractEnemyType enemyType,WaveContext context)
    {
        super(entity,Health,Mana,defense,1,1);
        this.updateTime = updateTime;
        this.context = context;
        this.entity = entity;
        this.damageMultiplier = damageMultiplier;
        this.enemyType = enemyType;
        entity.setCustomNameVisible(true);
        updateDisplay();
        Main.game.ENEMY_LIST.put(entity.getUniqueId(),this);
        startUpdate();
        Main.game.EnemyTeam.addEntity(entity);
        entity.setGlowing(true);
        entity.addScoreboardTag("lot_wave_enemy");
        entity.addScoreboardTag("lot_wave_enemy_" + enemyType.displayName.toLowerCase(Locale.ROOT));
    }
    protected void updateDisplay() {

        String label = ChatColor.RED + String.format(Locale.US, "%.0f/%.0f HP ", HEALTH, MAX_HEALTH)
                + ChatColor.GRAY + enemyType.displayName;
        entity.setCustomName(label);
        if(isBoss() && HEALTH > 0)
        {
            Main.game.BossHealthBar.progress(HEALTH/MAX_HEALTH);
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
        },0L,1L);
    }
    public abstract void update();
    public abstract void tick();
    public abstract void OnAttack();
    public abstract void OnDealDamage();
    @Override
    public void applyDamage(float damage, LabyrinthCreature damageDealer, boolean bypassChain)
    {
        HEALTH -= damage;
        entity.damage(0);
        updateDisplay();
        entity.getWorld().spawnParticle(Particle.BLOCK, entity.getLocation().add(0,1,0),50,0.3,0.6,0.3,0, BlockType.REDSTONE_BLOCK.createBlockData() );

        if(damageDealer != null && damageDealer instanceof GamePlayer)
        {
            ((GamePlayer)damageDealer).onDoDamage(this,damage);
            if(HEALTH <= 0)
            {
                Destroy((GamePlayer) damageDealer);
            }

        } else {
            if(HEALTH <= 0)
            {
                Destroy();
            }
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
