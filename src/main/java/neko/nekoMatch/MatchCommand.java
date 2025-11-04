package neko.nekoMatch;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

public class MatchCommand implements CommandExecutor {
    private final NekoMatch plugin;

    public MatchCommand(NekoMatch plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ実行可能です");
            return true;
        }

        Player player = (Player) sender;

        // 检查玩家是否在线
        if (!player.isOnline()) {
            player.sendMessage("您已离线，无法执行此操作");
            return true;
        }

        if (args.length == 0) {
            // 默认打开4v4模式的匹配GUI
            openModeSpecificGUI(player, "4v4");
        } else {
            String mode = args[0];
            // 打开对应模式的匹配GUI
            openModeSpecificGUI(player, mode);
        }

        return true;
    }

    private void openModeSpecificGUI(Player player, String mode) {
        plugin.getLogger().info("尝试打开模式: " + mode);
        // 首先尝试直接查找模式
        if (plugin.getConfig().contains("modes." + mode)) {
            plugin.getLogger().info("直接找到模式: " + mode);
            MatchGUI gui = new MatchGUI(plugin);
            gui.openModeSpecificGUI(player, mode);
            return;
        }
        
        // 如果直接查找失败，尝试通过描述查找模式ID
        if (plugin.getConfig().contains("modes")) {
            plugin.getLogger().info("在配置文件中查找描述匹配");
            for (String modeKey : plugin.getConfig().getConfigurationSection("modes").getKeys(false)) {
                String description = plugin.getConfig().getString("modes." + modeKey + ".description");
                plugin.getLogger().info("检查模式 " + modeKey + " 描述: " + description);
                if (mode.equals(description)) {
                    plugin.getLogger().info("通过描述匹配到模式ID: " + modeKey);
                    MatchGUI gui = new MatchGUI(plugin);
                    gui.openModeSpecificGUI(player, modeKey);
                    return;
                }
                // 也检查模式ID是否与输入匹配（处理用户可能直接输入模式ID的情况）
                if (mode.equals(modeKey)) {
                    plugin.getLogger().info("通过模式ID匹配到模式: " + modeKey);
                    MatchGUI gui = new MatchGUI(plugin);
                    gui.openModeSpecificGUI(player, modeKey);
                    return;
                }
            }
        }
        
        player.sendMessage(ChatColor.RED + "未知的游戏模式: " + mode);
    }
}