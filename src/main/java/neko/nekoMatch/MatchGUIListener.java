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
        Inventory inventory = event.getInventory();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        String inventoryTitle = event.getView().getTitle();
        event.setCancelled(true);

        if (inventoryTitle.contains("模式匹配")) {
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
            String mode = inventoryTitle.replace(" 模式匹配", "");
            // 使用ServerManager选择合适的服务器
            String serverName = plugin.selectAvailableServer(mode);
            if (serverName != null) {
                plugin.connectToServer(player, serverName);
                player.sendMessage(ChatColor.GREEN + "正在将您连接到 " + serverName + " 服务器...");
                player.closeInventory();
            } else {
                player.sendMessage(ChatColor.RED + "当前没有可用的 " + mode + " 服务器，请稍后再试");
                // 更新GUI状态
                updateGUIStatus(inventoryTitle, player, mode, "无可用服务器");
            }
        }
    }

    private void updateGUIStatus(String inventoryTitle, Player player, String mode, String status) {
        // 这里可以更新GUI中的状态显示
        // 由于Bukkit API限制，我们简单地重新打开GUI
        MatchGUI gui = new MatchGUI(plugin);
        gui.openModeSpecificGUI(player, mode);
    }
}