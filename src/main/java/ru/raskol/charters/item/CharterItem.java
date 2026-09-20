package ru.raskol.charters.item;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import ru.raskol.charters.RaskolCharters;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Фабрика и читалка предмета грамоты (бумага с PDC-тегами). */
public final class CharterItem {

    private final RaskolCharters plugin;
    private final NamespacedKey kTag;
    private final NamespacedKey kAmount;
    private final NamespacedKey kFreq;
    private final NamespacedKey kIssued;
    private final NamespacedKey kExpires;
    private final NamespacedKey kNext;
    private final NamespacedKey kNation;
    private final NamespacedKey kIssuer;

    public CharterItem(RaskolCharters plugin) {
        this.plugin = plugin;
        kTag = new NamespacedKey(plugin, "charter");
        kAmount = new NamespacedKey(plugin, "amount");
        kFreq = new NamespacedKey(plugin, "frequency");
        kIssued = new NamespacedKey(plugin, "issued_at");
        kExpires = new NamespacedKey(plugin, "expires_at");
        kNext = new NamespacedKey(plugin, "next_pay_at");
        kNation = new NamespacedKey(plugin, "nation");
        kIssuer = new NamespacedKey(plugin, "issuer");
    }

    public ItemStack create(CharterData d) {
        ItemStack item = new ItemStack(Material.PAPER, 1);
        apply(item, d);
        return item;
    }

    /** Пишет PDC-теги и лор на предмет. */
    public void apply(ItemStack item, CharterData d) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.setDisplayName(color(plugin.getConfig().getString("item.name", "&6Жалованная грамота")));
        List<String> lore = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList("item.flavor")) {
            lore.add(color(line));
        }
        lore.add(color("&7──────────────"));
        lore.add(color("&eЖалование: &6" + fmt(d.amount) + "⚜ &7за выплату"));
        lore.add(color("&eЧастота: &7" + freqRu(d.frequency)));
        lore.add(color("&eДействует до: &7" + date(d.expiresAt)));
        lore.add(color("&eСледующая выплата: &7" + date(d.nextPayAt)));
        lore.add(color("&eДержава: &7" + d.nation));
        lore.add(color("&eВыдана королём: &7" + d.issuer));
        if (d.isExpired(System.currentTimeMillis())) {
            lore.add(color("&8Грамота истекла"));
        }
        meta.setLore(lore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(kTag, PersistentDataType.STRING, "true");
        pdc.set(kAmount, PersistentDataType.DOUBLE, d.amount);
        pdc.set(kFreq, PersistentDataType.STRING, d.frequency);
        pdc.set(kIssued, PersistentDataType.LONG, d.issuedAt);
        pdc.set(kExpires, PersistentDataType.LONG, d.expiresAt);
        pdc.set(kNext, PersistentDataType.LONG, d.nextPayAt);
        pdc.set(kNation, PersistentDataType.STRING, d.nation);
        pdc.set(kIssuer, PersistentDataType.STRING, d.issuer);
        item.setItemMeta(meta);
    }

    /** Читает данные грамоты из предмета; null если это не грамота. */
    public CharterData parse(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!"true".equals(pdc.get(kTag, PersistentDataType.STRING))) return null;

        CharterData d = new CharterData();
        d.amount = pdc.getOrDefault(kAmount, PersistentDataType.DOUBLE, 0.0);
        d.frequency = pdc.getOrDefault(kFreq, PersistentDataType.STRING, "day");
        d.issuedAt = pdc.getOrDefault(kIssued, PersistentDataType.LONG, 0L);
        d.expiresAt = pdc.getOrDefault(kExpires, PersistentDataType.LONG, 0L);
        d.nextPayAt = pdc.getOrDefault(kNext, PersistentDataType.LONG, 0L);
        d.nation = pdc.getOrDefault(kNation, PersistentDataType.STRING, "");
        d.issuer = pdc.getOrDefault(kIssuer, PersistentDataType.STRING, "");
        return d;
    }

    private String freqRu(String f) {
        switch (f) {
            case "hour": return "раз в час";
            case "week": return "раз в неделю";
            default: return "раз в день";
        }
    }

    public static String fmt(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    private static String date(long ms) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(ms));
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
