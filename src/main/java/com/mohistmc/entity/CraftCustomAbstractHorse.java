/*
 * Mohist - MohistMC
 * Copyright (C) 2018-2022.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.mohistmc.entity;

import com.mohistmc.api.EntityAPI;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftAbstractHorse;
import org.bukkit.entity.EntityCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;

public class CraftCustomAbstractHorse extends CraftAbstractHorse {

    public String entityName;

    public CraftCustomAbstractHorse(CraftServer server, AbstractHorseEntity entity) {
        super(server, entity);
        this.entityName = EntityAPI.entityName(entity);
    }

    @Override
    public String toString() {
        return "CraftCustomAbstractHorse{" + entityName + '}';
    }

    @Override
    public EntityType getType() {
        return EntityAPI.entityType(entityName, EntityType.FORGE_MOD_HORSE);
    }

    @Override
    public Horse.Variant getVariant() {
        return Horse.Variant.FORGE_MOD_HORSE;
    }

    @Override
    public EntityCategory getCategory() {
        return EntityCategory.NONE;
    }
}
