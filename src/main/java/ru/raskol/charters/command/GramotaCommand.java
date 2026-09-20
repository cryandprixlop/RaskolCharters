package ru.raskol.charters.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.raskol.charters.RaskolCharters;
import ru.raskol.charters.hook.TownyHook;
import ru.raskol.charters.item.CharterData;
import ru.raskol.charters.item.CharterItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public final class GramotaCommand implements CommandExecutor, TabCompleter {

    private final RaskolCharters plugin;
    private final CharterItem items;
    private final TownyHook towny;

    public GramotaCommand(RaskolCharters plugin, CharterItem items, TownyHook towny) {
        this.plugin = plugin;
        this.items = items;
        this.towny = towny;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "give" -> handleGive(sender, args);
            case "info" -> handleInfo(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== Королевские грамоты ===");
        sender.sendMessage("§e/gramota give <игрок> <сумма> <day|week|month> <hour|day|week> §7— выдать грамоту (король)");
        sender.sendMessage("§e/gramota info §7— ваши грамоты и условия");
    }

    private boolean canIssue(Player p) {
        return p.hasPermission("raskolcharters.admin") || towny.isKing(p);
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cТолько игроки могут выдавать грамоты.");
            return;
        }
        if (!canIssue(p)) {
            p.sendMessage("§cГрамоты выдаёт только король нации (или администратор).");
            return;
        }
        if (args.length < 5) {
            p.sendMessage("§cИспользуй: /gramota give <игрок> <сумма> <day|week|month> <hour|day|week>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            p.sendMessage("§cИгрок не найден: " + args[1]);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2].replace(",", "."));
        } catch (NumberFormatException e) {
            p.sendMessage("§cНекорректная сумма: " + args[2]);
            return;
        }
        if (amount <= 0) {
            p.sendMessage("§cСумма должна быть положительной.");
            return;
        }

        String period = args[3].toLowerCase();
        String freq = args[4].toLowerCase();
        long periodDays = plugin.getConfig().getLong("periods." + period, -1);
        long freqSec = plugin.getConfig().getLong("frequencies." + freq, -1);
        if (periodDays <= 0) {
            p.sendMessage("§cНеизвестный срок: " + period + " §7(day | week | month)");
            return;
        }
        if (freqSec <= 0) {
            p.sendMessage("§cНеизвестная частота: " + freq + " §7(hour | day | week)");
            return;
        }

        String nation = towny.nationOf(p);
        if (nation == null) nation = towny.nationOf(target);
        if (nation == null) {
            p.sendMessage("§cНе удалось определить нацию: ни вы, ни получатель не состоите в нации.");
            return;
        }
        if (plugin.getConfig().getBoolean("require-same-nation", true)) {
            String targetNation = towny.nationOf(target);
            if (targetNation == null || !targetNation.equals(nation)) {
                p.sendMessage("§c" + target.getName() + " не является подданным нации " + nation + ".");
                return;
            }
        }

        long now = System.currentTimeMillis();
        CharterData d = new CharterData();
        d.amount = amount;
        d.frequency = freq;
        d.issuedAt = now;
        d.expiresAt = now + periodDays * 86_400_000L;
        d.nextPayAt = now + freqSec * 1000L;
        d.nation = nation;
        d.issuer = p.getName();

        ItemStack item = items.create(d);
        HashMap<Integer, ItemStack> overflow = target.getInventory().addItem(item);
        for (ItemStack drop : overflow.values()) {
            target.getWorld().dropItem(target.getLocation(), drop);
        }

        target.sendMessage("§6Король §e" + p.getName() + " §6жаловал вам грамоту!");
        target.sendMessage("§e" + CharterItem.fmt(amount) + "⚜ §7" + freqRu(freq)
                + " §7из казны нации §e" + nation + "§7.");
        p.sendMessage("§aГрамота выдана игроку §e" + target.getName()
                + " §a(" + CharterItem.fmt(amount) + "⚜, " + freqRu(freq)
                + ", срок " + periodDays + " дн.).");
        plugin.getLogger().info("[Charters] give: " + p.getName() + " -> " + target.getName()
                + " amount=" + amount + " freq=" + freq + " period=" + period
                + " nation=" + nation);
    }

    private void handleInfo(CommandSender sender) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cТолько игроки.");
            return;
        }
        List<CharterData> found = new ArrayList<>();
        for (ItemStack stack : p.getInventory().getStorageContents()) {
            CharterData d = items.parse(stack);
            if (d != null) found.add(d);
        }
        if (found.isEmpty()) {
            p.sendMessage("§7У вас нет королевских грамот.");
            return;
        }
        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        p.sendMessage("§6=== Ваши грамоты: " + found.size() + " ===");
        int i = 1;
        for (CharterData d : found) {
            p.sendMessage("§e" + (i++) + ". §6" + CharterItem.fmt(d.amount) + "⚜ §7"
                    + freqRu(d.frequency));
            p.sendMessage("§7   Действует до: §e" + df.format(new Date(d.expiresAt))
                    + " §7| След. выплата: §e" + df.format(new Date(d.nextPayAt)));
            p.sendMessage("§7   Держава: §e" + d.nation + " §7| Выдал: §e" + d.issuer
                    + (d.isExpired(System.currentTimeMillis()) ? " §8(истекла)" : ""));
        }
    }

    private String freqRu(String f) {
        switch (f) {
            case "hour": return "раз в час";
            case "week": return "раз в неделю";
            default: return "раз в день";
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("give", "info"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> names = new ArrayList<>();
            for (Player pl : Bukkit.getOnlinePlayers()) names.add(pl.getName());
            return filter(names, args[1]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            return filter(new ArrayList<>(plugin.getConfig().getConfigurationSection("periods").getKeys(false)), args[3]);
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("give")) {
            return filter(new ArrayList<>(plugin.getConfig().getConfigurationSection("frequencies").getKeys(false)), args[4]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String prefix) {
        String lower = prefix.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(lower)) out.add(s);
        }
        return out;
    }
}
