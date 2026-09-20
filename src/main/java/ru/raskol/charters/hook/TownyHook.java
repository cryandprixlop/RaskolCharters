package ru.raskol.charters.hook;

import org.bukkit.entity.Player;
import ru.raskol.charters.RaskolCharters;

import java.lang.reflect.Method;
import java.util.UUID;

/** Хук Towny через рефлексию (без жёсткой зависимости). */
public final class TownyHook {

    private final RaskolCharters plugin;

    public TownyHook(RaskolCharters plugin) {
        this.plugin = plugin;
    }

    public boolean isAvailable() {
        return plugin.getServer().getPluginManager().getPlugin("Towny") != null;
    }

    /** Имя нации, в которой живёт игрок; null если не состоит. */
    public String nationOf(Player player) {
        try {
            Object nation = nationObject(player);
            if (nation == null) return null;
            return (String) nation.getClass().getMethod("getName").invoke(nation);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Является ли игрок королём своей нации. */
    public boolean isKing(Player player) {
        try {
            Object nation = nationObject(player);
            if (nation == null) return false;
            Object king = nation.getClass().getMethod("getKing").invoke(nation);
            if (king == null) return false;
            String kingName = (String) king.getClass().getMethod("getName").invoke(king);
            return kingName != null && kingName.equalsIgnoreCase(player.getName());
        } catch (Throwable t) {
            return false;
        }
    }

    private Object nationObject(Player player) throws Exception {
        Class<?> apiClass = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
        Object api = apiClass.getMethod("getInstance").invoke(null);
        Object resident = apiClass.getMethod("getResident", UUID.class)
                .invoke(api, player.getUniqueId());
        if (resident == null) return null;
        Object town = callFirst(resident, "getTownOrNull", "getTown");
        if (town == null) return null;
        return callFirst(town, "getNationOrNull", "getNation");
    }

    private Object callFirst(Object target, String... methods) {
        for (String name : methods) {
            try {
                Method m = target.getClass().getMethod(name);
                Object out = m.invoke(target);
                if (out != null) return out;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
