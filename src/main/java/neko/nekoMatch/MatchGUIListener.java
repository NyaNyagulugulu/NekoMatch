package neko.nekoMatch;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.ChatColor;

public class MatchGUIListener implements Listener {
    private final NekoMatch plugin;
    private final MatchGUI matchGUI;

    public MatchGUIListener(NekoMatch plugin) {
        this.plugin = plugin;
        this.matchGUI = new MatchGUI(plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        String inventoryTitle = event.getView().getTitle();
        event.setCancelled(true);

        if (inventoryTitle.contains("匹配模式选择")) {
            handleModeSpecificGUI(player, clickedItem, inventoryTitle);
        } else if (inventoryTitle.contains("服务器状态")) {
            handleServerStatusGUI(player, clickedItem, inventoryTitle);
        }
    }

    private void handleModeSpecificGUI(Player player, ItemStack clickedItem, String inventoryTitle) {
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;
        
        String displayName = meta.getDisplayName();
        
        if (displayName.equals(ChatColor.RED + "取消")) {
            // 关闭GUI
            player.closeInventory();
        } else if (displayName.equals(ChatColor.GREEN + "开始匹配")) {
            // 从标题中提取模式名称
            String mode = extractModeFromTitle(inventoryTitle);
            // 使用ServerManager选择合适的服务器
            String serverName = plugin.selectAvailableServer(mode);
            if (serverName != null) {
                plugin.connectToServer(player, serverName);
                player.sendMessage(ChatColor.GREEN + "正在连接到服务器 " + serverName + "...");
                player.closeInventory();
            } else {
                player.sendMessage(ChatColor.RED + "当前没有可用的 " + mode + " 服务器");
            }
        } else if (displayName.equals(ChatColor.LIGHT_PURPLE + "服务器信息")) {
            // 打开服务器状态GUI
            String mode = extractModeFromTitle(inventoryTitle);
            matchGUI.openServerStatusGUI(player, mode);
        }
    }

    private void handleServerStatusGUI(Player player, ItemStack clickedItem, String inventoryTitle) {
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;
        
        String displayName = meta.getDisplayName();
        String serverName = ChatColor.stripColor(displayName);
        
        if (displayName.equals(ChatColor.AQUA + "返回")) {
            // 返回模式选择GUI
            String mode = extractModeFromServerStatusTitle(inventoryTitle);
            matchGUI.openModeSpecificGUI(player, mode);
        } else {
            // 获取服务器状态
            ServerManager.ServerStatus status = plugin.getServerManager().getServerStatus(serverName);
            
            switch (status) {
                case DEVELOPING:
                    // 如果是开发中状态，拒绝所有方式加入
                    player.sendMessage(ChatColor.RED + "服务器 " + serverName + " 正在维护中，无法连接");
                    player.closeInventory();
                    break;
                case PLAYING:
                    // 如果是游戏中状态，拒绝手动加入
                    player.sendMessage(ChatColor.RED + "服务器 " + serverName + " 正在游戏中，无法手动加入");
                    player.closeInventory();
                    break;
                case WAITING:
                    // 如果是等待中状态，允许手动加入
                    plugin.connectToServer(player, serverName);
                    player.sendMessage(ChatColor.GREEN + "正在连接到服务器 " + serverName + "...");
                    player.closeInventory();
                    break;
                case OFFLINE:
                default:
                    // 如果是离线状态，尝试连接但提示玩家
                    player.sendMessage(ChatColor.RED + "服务器 " + serverName + " 当前离线，无法连接");
                    plugin.connectToServer(player, serverName);
                    player.sendMessage(ChatColor.YELLOW + "正在尝试连接到服务器 " + serverName + "...");
                    player.closeInventory();
                    break;
            }
        }
    }

    private String extractModeFromTitle(String title) {
        // 从标题中提取模式名称，例如从 "匹配模式选择 - 4v4" 提取 "4v4"
        if (title.contains("1v1")) {
            return "1v1";
        } else if (title.contains("2v2")) {
            return "2v2";
        } else if (title.contains("4v4")) {
            return "4v4";
        }
        return "4v4"; // 默认模式
    }
    
    private String extractModeFromServerStatusTitle(String title) {
        // 从标题中提取模式名称，例如从 "4v4 服务器状态" 提取 "4v4"
        return title.replace(" 服务器状态", "");
    }
}