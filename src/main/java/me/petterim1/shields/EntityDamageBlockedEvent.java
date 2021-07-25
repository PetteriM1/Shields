package me.petterim1.shields;

import cn.nukkit.entity.Entity;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityEvent;

public class EntityDamageBlockedEvent extends EntityEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    private final EntityDamageEvent source;
    private boolean knockBack;

    public EntityDamageBlockedEvent(Entity entity, EntityDamageEvent damage, boolean knockBack) {
        this.entity = entity;
        this.source = damage;
        this.knockBack = knockBack;
    }

    public EntityDamageEvent.DamageCause getCause() {
        return source.getCause();
    }

    public Entity getAttacker() {
        return source.getEntity();
    }

    public EntityDamageEvent getSource() {
        return source;
    }

    public boolean getKnockBack() {
        return knockBack;
    }

    public void setKnockBack(boolean knockBack) {
        this.knockBack = knockBack;
    }
}
