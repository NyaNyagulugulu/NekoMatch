package neko.nekoMatch;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;

public class MatchGUIListener implements Listener {
    private final NekoMatch plugin;

    public MatchGUIListener(NekoMatch plugin) {
        this.plugin = plugin;
    }

    public void openMatchGUI(Player player) {
        // 创建匹配GUI (兼容1.12.2版本)
        Inventory gui = Bukkit.createInventory(null, 27, "匹配选择");
        
        // 添加4v4选项 (使用1.12.2版本中可用的物品)
        ItemStack item4v4 = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta4v4 = item4v4.getItemMeta();
        meta4v4.setDisplayName(ChatColor.YELLOW + "4v4 对战");
        item4v4.setItemMeta(meta4v4);
        gui.setItem(13, item4v4);
        
        // 添加说明物品
        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName(ChatColor.GREEN + "服务器状态");
        infoItem.setItemMeta(infoMeta);
        gui.setItem(26, infoItem);
        
        // 打开GUI
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // 检查是否是匹配GUI的点击事件
        if (event.getView().getTitle().equals("匹配选择")) {
            event.setCancelled(true); // 阻止默认行为

            // 检查点击者是否是玩家
            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            
            // 检查玩家是否在线
            if (!player.isOnline()) {
                player.closeInventory();
                return;
            }

            ItemStack clickedItem = event.getCurrentItem();

            // 检查点击的物品是否为空
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            // 根据点击的物品执行匹配
            String mode = getModeFromItem(clickedItem);
            if (mode != null) {
                player.closeInventory();
                // 使用ServerManager选择合适的服务器
                String serverName = plugin.selectAvailableServer(mode);
                if (serverName != null) {
                    plugin.connectToServer(player, serverName);
                    player.sendMessage(ChatColor.GREEN + "正在将您连接到 " + serverName + " 服务器...");
                } else {
                    player.sendMessage(ChatColor.RED + "当前没有可用的 " + mode + " 服务器，请稍后再试");
                }
            }
        }
    }

    private String getModeFromItem(ItemStack item) {
        // 根据物品的显示名称判断游戏模式
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = meta.getDisplayName();
            if (displayName.contains("4v4")) {
                return "4v4";
            }
        }
        return null;
    }
}