package ru.raskol.charters;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import ru.raskol.charters.command.GramotaCommand;
import ru.raskol.charters.hook.TownyHook;
import ru.raskol.charters.item.CharterItem;

public final class RaskolCharters extends JavaPlugin {

    private CharterItem charterItem;
    private TownyHook townyHook;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        charterItem = new CharterItem(this);
        townyHook = new TownyHook(this);

        PluginCommand cmd = getCommand("gramota");
        if (cmd != null) {
            GramotaCommand executor = new GramotaCommand(this, charterItem, townyHook);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        getLogger().info("RaskolCharters v" + getDescription().getVersion()
                + " включён. Towny: " + (townyHook.isAvailable() ? "да" : "нет")
                + ". Предмет: " + getConfig().getString("item.name", "&6Жалованная грамота"));
    }

    @Override
    public void onDisable() {
        getLogger().info("RaskolCharters выключен.");
    }

    public CharterItem getCharterItem() { return charterItem; }
    public TownyHook getTownyHook() { return townyHook; }
}
