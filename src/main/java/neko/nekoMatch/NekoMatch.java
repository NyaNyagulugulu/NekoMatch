package neko.nekoMatch;

import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;

public final class NekoMatch extends JavaPlugin {

    private ServerManager serverManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        serverManager = new ServerManager(this);
        
        // 注册BungeeCord插件消息通道
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        
        // 注册命令
        getCommand("match").setExecutor(new MatchCommand(this));
        getCommand("matchreload").setExecutor(new ReloadCommand(this));
        
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new MatchGUIListener(this), this);
        
        getLogger().info("NekoMatch 插件已启用 (直接连接到 Paper 1.12.2 服务器)");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("NekoMatch 插件已禁用");
    }

    /**
     * 获取服务器管理器
     */
    public ServerManager getServerManager() {
        return serverManager;
    }
    
    /**
     * 重新初始化服务器管理器
     */
    public void reinitializeServerManager() {
        serverManager.reinitialize();
    }
    
    /**
     * 连接玩家到指定服务器
     */
    public void connectToServer(Player player, String serverName) {
        serverManager.connectToServer(player, serverName);
    }
    
    /**
     * 根据游戏模式选择可用服务器
     */
    public String selectAvailableServer(String mode) {
        return serverManager.selectAvailableServer(mode);
    }
}