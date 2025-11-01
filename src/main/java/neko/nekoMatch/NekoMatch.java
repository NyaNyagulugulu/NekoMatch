package neko.nekoMatch;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class NekoMatch extends JavaPlugin {

    private ServerManager serverManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        serverManager = new ServerManager(this);
        
        // 注册命令
        getCommand("match").setExecutor(new MatchCommand(this));
        getCommand("matchreload").setExecutor(new MatchCommand(this));
        
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new MatchGUIListener(this), this);
        
        getLogger().info("NekoMatch 插件已启用");
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
}
