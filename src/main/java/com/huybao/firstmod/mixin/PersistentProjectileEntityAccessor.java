package com.huybao.firstmod.mixin;

import net.minecraft.entity.projectile.PersistentProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Exposes the private `damage` field so we can read a projectile's current damage.
// (setDamage is already public; there's no public getter.)
@Mixin(PersistentProjectileEntity.class)
public interface PersistentProjectileEntityAccessor {
    @Accessor("damage")
    double getDamage();
}
