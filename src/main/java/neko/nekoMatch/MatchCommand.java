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
            // 打开匹配GUI
            openMatchGUI(player);
        } else {
            String mode = args[0];
            // 尝试匹配到指定模式
            joinMatch(player, mode);
        }

        return true;
    }

    private void openMatchGUI(Player player) {
        // 获取MatchGUIListener实例并打开GUI
        MatchGUIListener guiListener = new MatchGUIListener(plugin);
        guiListener.openMatchGUI(player);
    }

    private void joinMatch(Player player, String mode) {
        // 使用ServerManager选择合适的服务器
        String serverName = plugin.selectAvailableServer(mode);
        if (serverName != null) {
            plugin.connectToServer(player, serverName);
            player.sendMessage(ChatColor.GREEN + "正在将您连接到 " + serverName + " 服务器...");
        } else {
            player.sendMessage(ChatColor.RED + "当前没有可用的 " + mode + " 服务器，请稍后再试");
        }
    }
}