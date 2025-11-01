package neko.nekoMatch;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
}