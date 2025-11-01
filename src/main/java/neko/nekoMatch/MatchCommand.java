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
                // 如果有参数，直接开始匹配指定模式
                String mode = args[0].toLowerCase();
                startMatching(player, mode);
            }
        } else if (label.equalsIgnoreCase("matchreload")) {
            // 重载配置文件
            plugin.reloadConfig();
            plugin.getServerManager().reloadConfig(plugin.getConfig());
            sender.sendMessage("§a配置文件已重载！");
        }
        
        return true;
    }
    
    private void startMatching(Player player, String mode) {
        player.sendMessage("§b正在为 " + mode + " 模式寻找合适的服务器...");
        
        // 调用服务器管理器来查找合适的服务器
        plugin.getServerManager().findAndMatchServer(player, mode);
    }
}