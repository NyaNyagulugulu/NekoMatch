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
        Inventory gui = Bukkit.createInventory(null, 27, mode + " 模式匹配");
        
        // 显示模式信息
        ItemStack modeInfo = new ItemStack(getMaterialForMode(mode));
        ItemMeta modeInfoMeta = modeInfo.getItemMeta();
        modeInfoMeta.setDisplayName(ChatColor.AQUA + "模式: " + mode);
        modeInfo.setItemMeta(modeInfoMeta);
        gui.setItem(10, modeInfo);
        
        // 显示匹配状态
        ItemStack statusItem = new ItemStack(Material.WATCH);
        ItemMeta statusMeta = statusItem.getItemMeta();
        statusMeta.setDisplayName(ChatColor.GOLD + "匹配状态");
        statusMeta.setLore(java.util.Arrays.asList(
            ChatColor.WHITE + "当前状态: " + ChatColor.GREEN + "等待中",
            ChatColor.WHITE + "正在为您寻找队友..."
        ));
        statusItem.setItemMeta(statusMeta);
        gui.setItem(12, statusItem);
        
        // 显示可用服务器数量
        FileConfiguration config = plugin.getConfig();
        List<String> servers = config.getStringList("modes." + mode + ".servers");
        ItemStack serverInfo = new ItemStack(Material.CHEST);
        ItemMeta serverInfoMeta = serverInfo.getItemMeta();
        serverInfoMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "可用服务器");
        serverInfoMeta.setLore(java.util.Arrays.asList(
            ChatColor.WHITE + "总数: " + servers.size() + " 个",
            ChatColor.WHITE + "状态: " + ChatColor.GREEN + "在线"
        ));
        serverInfo.setItemMeta(serverInfoMeta);
        gui.setItem(14, serverInfo);
        
        // 匹配按钮
        ItemStack matchItem = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta matchMeta = matchItem.getItemMeta();
        matchMeta.setDisplayName(ChatColor.GREEN + "开始匹配");
        matchMeta.setLore(java.util.Arrays.asList(
            ChatColor.WHITE + "点击开始匹配 " + mode + " 模式",
            "",
            ChatColor.GRAY + "系统将自动为您选择最佳服务器"
        ));
        matchItem.setItemMeta(matchMeta);
        gui.setItem(16, matchItem);
        
        // 取消按钮
        ItemStack cancelItem = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "取消");
        cancelItem.setItemMeta(cancelMeta);
        gui.setItem(26, cancelItem);
        
        // 打开GUI
        player.openInventory(gui);
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