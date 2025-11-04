package neko.nekoMatch;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;

public class MatchGUI {
    private final NekoMatch plugin;

    public MatchGUI(NekoMatch plugin) {
        this.plugin = plugin;
    }
    
    public void openModeSpecificGUI(Player player, String mode) {
        // 为特定模式创建匹配GUI
        Inventory gui = Bukkit.createInventory(null, 27, "匹配模式选择 - " + mode);
        
        // 模式标题
        ItemStack modeTitle = new ItemStack(getMaterialForMode(mode));
        ItemMeta modeTitleMeta = modeTitle.getItemMeta();
        modeTitleMeta.setDisplayName(ChatColor.AQUA + mode + " 对战模式");
        modeTitle.setItemMeta(modeTitleMeta);
        gui.setItem(13, modeTitle);
        
        // 服务器信息
        FileConfiguration config = plugin.getConfig();
        List<String> servers = config.getStringList("modes." + mode + ".servers");
        ItemStack serverInfo = new ItemStack(Material.CHEST);
        ItemMeta serverInfoMeta = serverInfo.getItemMeta();
        serverInfoMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "服务器信息");
        serverInfoMeta.setLore(java.util.Arrays.asList(
            ChatColor.WHITE + "可用服务器: " + ChatColor.GREEN + servers.size() + " 个",
            ChatColor.WHITE + "状态: " + ChatColor.GREEN + "在线",
            "",
            ChatColor.YELLOW + "点击查看服务器状态"
        ));
        serverInfo.setItemMeta(serverInfoMeta);
        gui.setItem(11, serverInfo);
        
        // 开始匹配按钮
        ItemStack matchItem = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta matchMeta = matchItem.getItemMeta();
        matchMeta.setDisplayName(ChatColor.GREEN + "开始匹配");
        matchMeta.setLore(java.util.Arrays.asList(
            ChatColor.WHITE + "点击开始匹配",
            "",
            ChatColor.GRAY + "正在寻找最佳服务器..."
        ));
        matchItem.setItemMeta(matchMeta);
        gui.setItem(15, matchItem);
        
        // 取消按钮
        ItemStack cancelItem = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "取消");
        cancelItem.setItemMeta(cancelMeta);
        gui.setItem(22, cancelItem);
        
        // 装饰物品
        ItemStack decoration = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 0);
        ItemMeta decorationMeta = decoration.getItemMeta();
        decorationMeta.setDisplayName(" ");
        decoration.setItemMeta(decorationMeta);
        
        // 在边缘放置装饰物品
        for (int i = 0; i < 27; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, decoration.clone());
            }
        }
        
        // 打开GUI
        player.openInventory(gui);
    }
    
    public void openServerStatusGUI(Player player, String mode) {
        // 为特定模式创建服务器状态GUI
        Inventory gui = Bukkit.createInventory(null, 54, mode + " 服务器状态");
        
        // 获取该模式的所有服务器
        FileConfiguration config = plugin.getConfig();
        List<String> servers = config.getStringList("modes." + mode + ".servers");
        
        int slot = 0;
        for (String serverName : servers) {
            // 获取服务器详细状态
            ServerManager.ServerStatus status = plugin.getServerManager().getServerStatus(serverName);
            
            ItemStack serverItem;
            String displayName;
            String statusText;
            String clickText = ChatColor.YELLOW + "点击手动加入";
            
            switch (status) {
                case DEVELOPING:
                    serverItem = new ItemStack(Material.WOOL, 1, (short) 4); // 黄色羊毛
                    displayName = ChatColor.YELLOW + serverName;
                    statusText = ChatColor.YELLOW + "开发中";
                    clickText = ChatColor.RED + "服务器正在维护中";
                    break;
                case PLAYING:
                    serverItem = new ItemStack(Material.WOOL, 1, (short) 14); // 红色羊毛
                    displayName = ChatColor.RED + serverName;
                    statusText = ChatColor.RED + "游戏中";
                    clickText = ChatColor.RED + "服务器正在游戏中";
                    break;
                case WAITING:
                    serverItem = new ItemStack(Material.WOOL, 1, (short) 5); // 绿色羊毛
                    displayName = ChatColor.GREEN + serverName;
                    statusText = ChatColor.GREEN + "等待中";
                    clickText = ChatColor.YELLOW + "点击手动加入";
                    break;
                case OFFLINE:
                default:
                    serverItem = new ItemStack(Material.WOOL, 1, (short) 7); // 灰色羊毛
                    displayName = ChatColor.GRAY + serverName;
                    statusText = ChatColor.GRAY + "离线";
                    clickText = ChatColor.RED + "服务器离线";
                    break;
            }
            
            ItemMeta serverMeta = serverItem.getItemMeta();
            serverMeta.setDisplayName(displayName);
            
            java.util.List<String> lore = java.util.Arrays.asList(
                ChatColor.WHITE + "状态: " + statusText,
                "",
                clickText
            );
            
            serverMeta.setLore(lore);
            serverItem.setItemMeta(serverMeta);
            
            if (slot < 54) {
                gui.setItem(slot, serverItem);
                slot++;
            }
        }
        
        // 返回按钮
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.AQUA + "返回");
        backItem.setItemMeta(backMeta);
        gui.setItem(53, backItem);
        
        // 装饰物品
        ItemStack decoration = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta decorationMeta = decoration.getItemMeta();
        decorationMeta.setDisplayName(" ");
        decoration.setItemMeta(decorationMeta);
        
        // 在空位放置装饰物品
        for (int i = 0; i < 54; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, decoration.clone());
            }
        }
        
        player.openInventory(gui);
    }
    
    private boolean checkServerStatus(String serverName) {
        // 使用ServerManager检查服务器状态
        return plugin.getServerManager().isServerAvailable(serverName);
    }
    
    private Material getMaterialForMode(String mode) {
        switch (mode.toLowerCase()) {
            case "1v1":
                return Material.IRON_SWORD;
            case "2v2":
                return Material.STONE_SWORD;
            case "4v4":
                return Material.DIAMOND_SWORD;
            default:
                return Material.WOOD_SWORD;
        }
    }
}