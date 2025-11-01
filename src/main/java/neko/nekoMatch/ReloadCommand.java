package neko.nekoMatch;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;

public class ReloadCommand implements CommandExecutor {
    private final NekoMatch plugin;

    public ReloadCommand(NekoMatch plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 重新加载配置文件
        plugin.reloadConfig();
        plugin.reinitializeServerManager();
        
        sender.sendMessage(ChatColor.GREEN + "NekoMatch 插件配置已重新加载!");
        return true;
    }
}