package neko.nekoMatch;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class MatchGUIListener implements Listener {
    private final NekoMatch plugin;

    public MatchGUIListener(NekoMatch plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        // 1.12.2中使用getTitle()而不是getTitle()
        String inventoryTitle = event.getInventory().getTitle();
        
        // 检查是否是匹配GUI
        if (inventoryTitle.equals("§b§l匹配模式选择")) {
            event.setCancelled(true); // 防止玩家拿取物品
            
            if (event.getCurrentItem() == null || event.getCurrentItem().getItemMeta() == null) {
                return;
            }
            
            String itemName = event.getCurrentItem().getItemMeta().getDisplayName();
            
            // 处理关闭按钮点击
            if (itemName.equals("§c§l关闭")) {
                player.closeInventory();
                return;
            }
            
            // 处理模式点击（检查是否以"模式"结尾）
            if (itemName.endsWith("§l模式")) {
                player.closeInventory();
                // 提取模式名称（移除颜色代码和"模式"字样）
                String mode = itemName.replaceAll("§[0-9a-fk-or]", "").replace("模式", "").trim();
                startMatching(player, mode);
                return;
            }
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // 可以在这里处理GUI关闭事件
    }
    
    private void startMatching(Player player, String mode) {
        player.sendMessage("§b正在为 " + mode + " 模式寻找合适的服务器...");
        
        // 调用服务器管理器来查找合适的服务器
        plugin.getServerManager().findAndMatchServer(player, mode);
    }
}