package neko.nekoMatch;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class MatchGUI {
    private static final String MODE_GUI_TITLE_PREFIX = "§b§l";

    public static void openGUI(Player player, NekoMatch plugin, String mode) {
        // 检查模式是否存在
        if (!plugin.getConfig().contains(mode)) {
            player.sendMessage("§c未找到 " + mode + " 模式的配置");
            return;
        }
        
        String guiTitle = MODE_GUI_TITLE_PREFIX + mode + "匹配";
        // 确保标题不超过32个字符
        if (guiTitle.length() > 32) {
            guiTitle = guiTitle.substring(0, 32);
        }
        
        Inventory gui = Bukkit.createInventory(null, 27, guiTitle);
        
        // 填充背景玻璃板
        ItemStack background = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15); // 1.12.2中使用STAINED_GLASS_PANE和数据值
        ItemMeta backgroundMeta = background.getItemMeta();
        backgroundMeta.setDisplayName(" ");
        background.setItemMeta(backgroundMeta);
        
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, background);
        }
        
        // 添加模式信息
        addModeInfoItems(gui, plugin, mode);
        
        // 添加开始匹配按钮
        ItemStack matchItem = new ItemStack(getModeMaterial(mode, plugin));
        ItemMeta matchMeta = matchItem.getItemMeta();
        matchMeta.setDisplayName("§a§l开始匹配");
        matchMeta.setLore(Arrays.asList(
            "§7点击开始匹配" + mode + "模式",
            "§7当前可用服务器: " + getServerCount(plugin, mode) + "个"
        ));
        matchItem.setItemMeta(matchMeta);
        gui.setItem(13, matchItem); // 放在中间位置
        
        // 添加关闭按钮
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName("§c§l关闭");
        closeMeta.setLore(Arrays.asList("§7点击关闭此界面"));
        closeItem.setItemMeta(closeMeta);
        gui.setItem(22, closeItem); // 放在底部中间
        
        // 打开GUI
        player.openInventory(gui);
    }
    
    private static void addModeInfoItems(Inventory gui, NekoMatch plugin, String mode) {
        // 添加模式信息到GUI顶部
        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName("§b§l" + mode + "模式");
        
        // 获取服务器列表
        List<String> servers = plugin.getConfig().getStringList(mode + ".server");
        String serverInfo = "§7服务器列表: ";
        if (servers.isEmpty()) {
            serverInfo += "无";
        } else {
            serverInfo += String.join(", ", servers);
        }
        
        infoMeta.setLore(Arrays.asList(
            serverInfo,
            "§7点击下方按钮开始匹配"
        ));
        infoItem.setItemMeta(infoMeta);
        gui.setItem(4, infoItem); // 放在顶部中间
    }
    
    private static Material getModeMaterial(String mode, NekoMatch plugin) {
        // 从配置文件中获取材料类型，如果没有配置则使用默认值
        String materialName = "STONE_SWORD"; // 默认材料
        
        // 从配置文件中读取材料配置
        if (plugin.getConfig().contains("materials." + mode)) {
            materialName = plugin.getConfig().getString("materials." + mode, "STONE_SWORD");
        } else {
            // 如果没有特定配置，使用默认映射
            switch (mode.toLowerCase()) {
                case "1v1": materialName = "IRON_SWORD"; break;
                case "2v2": materialName = "GOLD_INGOT"; break; // 1.12.2中使用GOLD_INGOT而不是GOLDEN_SWORD
                case "4v4": materialName = "DIAMOND"; break; // 1.12.2中使用DIAMOND而不是DIAMOND_SWORD
                case "8v8": materialName = "DIAMOND"; break; // 1.12.2没有NETHERITE_SWORD，使用DIAMOND替代
                case "ffa": materialName = "BOW"; break;
                default: materialName = "STONE_SWORD"; break;
            }
        }
        
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            // 如果材料名称无效，使用默认材料
            return Material.STONE_SWORD;
        }
    }
    
    private static int getServerCount(NekoMatch plugin, String mode) {
        // 获取指定模式的服务器数量
        if (plugin.getConfig().contains(mode + ".server")) {
            return plugin.getConfig().getStringList(mode + ".server").size();
        }
        return 0;
    }
}