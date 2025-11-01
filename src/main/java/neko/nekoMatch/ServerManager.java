package neko.nekoMatch;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerManager {
    private final NekoMatch plugin;

    public ServerManager(NekoMatch plugin) {
        this.plugin = plugin;
    }

    /**
     * 重载配置文件
     */
    public void reloadConfig(FileConfiguration config) {
        // 无需额外操作，因为ServerManager会直接从plugin获取最新配置
    }

    /**
     * 查找并匹配服务器
     */
    public void findAndMatchServer(Player player, String mode) {
        FileConfiguration config = plugin.getConfig();
        
        // 检查模式是否存在
        if (!config.contains(mode)) {
            player.sendMessage("§c未找到 " + mode + " 模式的配置");
            return;
        }
        
        // 获取指定模式的服务器列表
        if (!config.contains(mode + ".server")) {
            player.sendMessage("§c" + mode + " 模式没有配置服务器");
            return;
        }
        
        List<String> serverNames = config.getStringList(mode + ".server");
        if (serverNames.isEmpty()) {
            player.sendMessage("§c" + mode + " 模式没有配置服务器");
            return;
        }
        
        // 随机选择一个服务器进行检查
        Random random = new Random();
        String selectedServer = serverNames.get(random.nextInt(serverNames.size()));
        
        player.sendMessage("§b正在检查服务器 " + selectedServer + "...");
        
        // 这里应该实现实际的服务器检查逻辑
        // 由于这是一个示例，我们假设服务器总是可用的
        checkServerAndMatch(player, selectedServer);
    }
    
    /**
     * 检查服务器并进行匹配
     */
    private void checkServerAndMatch(Player player, String serverName) {
        // 模拟服务器检查过程
        // 在实际应用中，这里应该检查服务器的实际状态和MOTD
        
        // 检查MOTD是否为等待中状态
        boolean isWaiting = checkMOTD(serverName);
        
        if (isWaiting) {
            player.sendMessage("§a找到合适的服务器! 正在连接到 " + serverName + "...");
            // 这里应该实现实际的服务器连接逻辑
            // 例如使用BungeeCord或Velocity进行服务器切换
        } else {
            player.sendMessage("§c服务器 " + serverName + " 不在等待中状态，正在寻找其他服务器...");
            // 这里可以实现重新寻找服务器的逻辑
        }
    }
    
    /**
     * 检查服务器MOTD
     */
    private boolean checkMOTD(String serverName) {
        // 在实际应用中，这里应该获取服务器的实际MOTD
        // 由于这是一个示例，我们随机返回true或false
        Random random = new Random();
        return random.nextBoolean();
    }
    
    /**
     * 检查MOTD是否为等待中状态
     */
    private boolean isWaitingMOTD(String motd) {
        FileConfiguration config = plugin.getConfig();
        String patternStr = config.getString("waiting_motd_pattern", "\\$currentplayers\\$/\\$maxplayers\\$");
        
        try {
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(motd);
            return matcher.matches();
        } catch (Exception e) {
            plugin.getLogger().warning("MOTD匹配模式不正确: " + patternStr);
            return false;
        }
    }
}