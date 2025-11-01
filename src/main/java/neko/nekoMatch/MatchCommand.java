package neko.nekoMatch;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MatchCommand implements CommandExecutor {
    private final NekoMatch plugin;

    public MatchCommand(NekoMatch plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("match")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("只有玩家可以使用此命令");
                return true;
            }

            Player player = (Player) sender;
            
            // 检查是否有参数
            if (args.length == 0) {
                // 如果没有参数，不执行任何操作
                return true;
            } else {
                // 如果有参数，打开对应模式的匹配GUI
                String mode = args[0].toLowerCase();
                MatchGUI.openGUI(player, plugin, mode);
            }
        } else if (label.equalsIgnoreCase("matchreload")) {
            // 重载配置文件
            plugin.reloadConfig();
            plugin.getServerManager().reloadConfig(plugin.getConfig());
            sender.sendMessage("§a配置文件已重载！");
        }
        
        return true;
    }
}