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
        
        // 检查是否是特定模式的匹配GUI（标题以"§b§l"开头并以"匹配"结尾）
        if (inventoryTitle.startsWith("§b§l") && inventoryTitle.endsWith("匹配")) {
            event.setCancelled(true); // 防止玩家拿取物品
            
            if (event.getCurrentItem() == null || event.getCurrentItem().getItemMeta() == null) {
                return;
            }
            
            String itemName = event.getCurrentItem().getItemMeta().getDisplayName();
            
            // 处理开始匹配按钮点击
            if (itemName.equals("§a§l开始匹配")) {
                player.closeInventory();
                // 从标题中提取模式名称
                String mode = inventoryTitle.replaceAll("§[0-9a-fk-or]", "").replace("匹配", "").trim();
                startMatching(player, mode);
                return;
            }
            
            // 处理关闭按钮点击
            if (itemName.equals("§c§l关闭")) {
                player.closeInventory();
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