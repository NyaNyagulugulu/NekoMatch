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
            sender.sendMessage("此命令只能由玩家执行");
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
        // 验证模式是否存在
        if (plugin.getConfig().contains("modes." + mode)) {
            MatchGUI gui = new MatchGUI(plugin);
            gui.openModeSpecificGUI(player, mode);
        } else {
            player.sendMessage(ChatColor.RED + "未知的游戏模式: " + mode);
        }
    }
}