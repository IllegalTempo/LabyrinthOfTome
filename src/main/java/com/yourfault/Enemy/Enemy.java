package com.yourfault.Enemy;

import com.yourfault.Enemy.system.AbstractEnemyType;
import com.yourfault.Main;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.wave.WaveContext;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockType;
import org.bukkit.entity.LivingEntity;

import java.util.Locale;

import static com.yourfault.Main.plugin;

public abstract class Enemy {
    public LivingEntity entity;
    public float HEALTH;
    public float DEFENSE;
    public float MaxHealth;
    private final long updateTime;
    public AbstractEnemyType enemyType;
    public WaveContext context;


    public float scale = 1;

    public float damageMultiplier = 1.0f;

    public Enemy(LivingEntity entity,WaveContext context,AbstractEnemyType type)
    {
        this(entity,type.getScaledHealth(context),type.getScaledDefense(context),type.getDamageMultipler(context),20L,type,context);

    }
    public Enemy(LivingEntity entity,WaveContext context,long updateTime,AbstractEnemyType type)
    {
        this(entity,type.getScaledHealth(context),type.getScaledDefense(context),type.getDamageMultipler(context),updateTime,type,context);

    }
    public Enemy(LivingEntity entity, float MaxHealth, float defense, float damageMultiplier,long updateTime,AbstractEnemyType enemyType,WaveContext context)
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
    private void updateDisplay() {

        String label = ChatColor.RED + String.format(Locale.US, "%.0f/%.0f HP ", HEALTH, MaxHealth)
                + ChatColor.GRAY + enemyType.displayName;
        entity.setCustomName(label);
        if(isBoss())
        {
            Main.game.BossHealthBar.progress(HEALTH/MaxHealth);
        }

    }
    private boolean isBoss()
    {
        return enemyType.isBoss;
    }
    public void startUpdate()
    {
        Bukkit.getScheduler().runTaskTimer(plugin,()->{
            update();
        },0L,updateTime);
        Bukkit.getScheduler().runTaskTimer(plugin,()->{
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
            Destroy();
        }
    }
    public void Destroy()
    {
        Main.game.ENEMY_LIST.remove(entity.getUniqueId());
        if (Main.game.getWaveManager() != null) {
            Main.game.getWaveManager().handleEnemyDeath(entity.getUniqueId(), null);
        }
        Main.game.onEnemyKilled(this);
        entity.remove();

    }
}
