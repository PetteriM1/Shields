package me.petterim1.shields;

import cn.nukkit.Player;
import cn.nukkit.block.BlockID;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.EntityLiving;
import cn.nukkit.entity.projectile.EntityProjectile;
import cn.nukkit.entity.weather.EntityWeather;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByChildEntityEvent;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.item.ItemSkull;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.level.Sound;
import cn.nukkit.math.Vector3;
import cn.nukkit.plugin.PluginBase;

import java.lang.reflect.Field;
import java.util.concurrent.ThreadLocalRandom;

public class Main extends PluginBase implements Listener {

    private Field attackTime;

    @Override
    public void onLoad() {
        Item.list[ItemID.SHIELD] = ItemShieldC.class;
    }

    @Override
    public void onEnable() {
        if (!getServer().getCodename().isEmpty()) {
            getLogger().error("Incompatible Nukkit version!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().scheduleDelayedRepeatingTask(this, this::tickPlayers, 1, 1);

        try {
            attackTime = EntityLiving.class.getDeclaredField("attackTime");
            attackTime.setAccessible(true);
        } catch (Exception ex) {
            getLogger().error("Failed to get attackTime using reflection", ex);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (blockedByShield(event)) {
            event.setCancelled(true);
        }
    }

    private boolean blockedByShield(EntityDamageEvent source) {
        if (!isBlocking(source.getEntity())) {
            return false;
        }

        Entity damager = source instanceof EntityDamageByChildEntityEvent ? ((EntityDamageByChildEntityEvent) source).getChild() : source instanceof EntityDamageByEntityEvent ? ((EntityDamageByEntityEvent) source).getDamager() : null;
        if (damager == null || damager instanceof EntityWeather) {
            return false;
        }

        try {
            if ((int) attackTime.get(damager) > 0) {
                return false;
            }
        } catch (Exception ex) {
            getLogger().debug("Getting attackTime failed", ex);
        }

        setBlocking(damager, false);

        Vector3 entityPos = damager.getPosition();
        Vector3 direction = source.getEntity().getDirectionVector();
        Vector3 normalizedVector = source.getEntity().getPosition().subtract(entityPos).normalize();
        boolean blocked = (normalizedVector.x * direction.x) + (normalizedVector.z * direction.z) < 0.0;
        boolean knockBack = !(damager instanceof EntityProjectile);
        EntityDamageBlockedEvent event = new EntityDamageBlockedEvent(source.getEntity(), source, knockBack);
        if (!blocked || !source.canBeReducedByArmor()) {
            event.setCancelled(true);
        }

        getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        if (event.getKnockBack() && damager instanceof EntityLiving) {
            double deltaX = damager.getX() - source.getEntity().getX();
            double deltaZ = damager.getZ() - source.getEntity().getZ();
            try {
                attackTime.set(damager, source.getAttackCooldown());
            } catch (Exception ex) {
                getLogger().debug("Setting attackTime failed", ex);
            }
            ((EntityLiving) damager).knockBack(source.getEntity(), 0, deltaX, deltaZ);
        }

        onBlock(source.getEntity(), damager, source.getDamage());
        return true;
    }

    private void onBlock(Entity entity, Entity damager, float damage) {
        entity.getLevel().addSound(entity, Sound.ITEM_SHIELD_BLOCK);
        if (entity instanceof EntityHuman) {
            EntityHuman en = (EntityHuman) entity;
            Item shieldOffhand = en.getOffhandInventory().getItem(0);
            if (shieldOffhand.getId() == ItemID.SHIELD) {
                en.getOffhandInventory().setItem(0, damageArmor(shieldOffhand, entity, damager, damage));
            } else {
                Item shield = en.getInventory().getItemInHand();
                if (shield.getId() == ItemID.SHIELD) {
                    en.getInventory().setItemInHand(damageArmor(shield, entity, damager, damage));
                }
            }
        }
    }

    private Item damageArmor(Item armor, Entity entity, Entity damager, float damage) {
        if (armor.isUnbreakable() || armor instanceof ItemSkull) {
            return armor;
        }

        if (armor.hasEnchantments()) {
            Enchantment durability = armor.getEnchantment(Enchantment.ID_DURABILITY);
            if (durability != null
                    && durability.getLevel() > 0
                    && (100 / (durability.getLevel() + 1)) <= ThreadLocalRandom.current().nextInt(100)) {
                return armor;
            }
        }

        armor.setDamage(armor.getDamage() + (damage >= 4.0f ? ((int) damage) : 1));

        if (armor.getDamage() >= armor.getMaxDurability()) {
            return Item.get(BlockID.AIR, 0, 0);
        }

        return armor;
    }

    private void tickPlayers() {
        for (Player player : getServer().getOnlinePlayers().values()) {
            if (player.spawned) {
                updateBlockingFlag(player);
            }
        }
    }

    private void updateBlockingFlag(Player player) {
        boolean shieldInHand = player.getInventory().getItemInHand().getId() == ItemID.SHIELD;
        boolean shieldInOffhand = player.getOffhandInventory().getItem(0).getId() == ItemID.SHIELD;
        if (isBlocking(player)) {
            if (!player.isSneaking() || (!shieldInHand && !shieldInOffhand)) {
                setBlocking(player, false);
            }
        } else if (player.isSneaking() && (shieldInHand || shieldInOffhand)) {
            setBlocking(player, true);
        }
    }

    private boolean isBlocking(Entity entity) {
        return entity.getDataFlag(Entity.DATA_FLAGS_EXTENDED, Entity.DATA_FLAG_BLOCKING);
    }

    private void setBlocking(Entity entity, boolean blocking) {
        entity.setDataFlag(Entity.DATA_FLAGS_EXTENDED, Entity.DATA_FLAG_BLOCKING, blocking);
    }
}
