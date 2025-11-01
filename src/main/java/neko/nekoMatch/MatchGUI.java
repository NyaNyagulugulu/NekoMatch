package neko.nekoMatch;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Set;

public class MatchGUI {
    private static final String GUI_TITLE = "§b§l匹配模式选择";

    public static void openGUI(Player player, NekoMatch plugin) {
        openGUI(player, plugin, null);
    }

    public static void openGUI(Player player, NekoMatch plugin, String preselectedMode) {
        // 获取所有模式（排除MOTD模式配置）
        Set<String> modes = plugin.getConfig().getKeys(false);
        modes.remove("waiting_motd_pattern");
        
        // 计算GUI大小（至少27格，最多54格）
        int guiSize = Math.min(54, Math.max(27, (int) Math.ceil(modes.size() / 9.0) * 9));
        
        Inventory gui = Bukkit.createInventory(null, guiSize, GUI_TITLE);
        
        // 填充背景玻璃板
        ItemStack background = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15); // 1.12.2中使用STAINED_GLASS_PANE和数据值
        ItemMeta backgroundMeta = background.getItemMeta();
        backgroundMeta.setDisplayName(" ");
        background.setItemMeta(backgroundMeta);
        
        for (int i = 0; i < guiSize; i++) {
            gui.setItem(i, background);
        }
        
        // 添加模式选项
        addModeItems(gui, plugin, modes, preselectedMode);
        
        // 添加关闭按钮
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName("§c§l关闭");
        closeMeta.setLore(Arrays.asList("§7点击关闭此界面"));
        closeItem.setItemMeta(closeMeta);
        gui.setItem(guiSize - 5, closeItem); // 放在GUI底部中间
        
        // 打开GUI
        player.openInventory(gui);
        
        // 如果有预选模式，可以直接开始匹配
        if (preselectedMode != null && modes.contains(preselectedMode)) {
            // 这里可以添加自动开始匹配的逻辑
        }
    }
    
    private static void addModeItems(Inventory gui, NekoMatch plugin, Set<String> modes, String preselectedMode) {
        int slot = 10; // 从第二行第二个位置开始
        
        for (String mode : modes) {
            // 计算行位置，避免超出GUI边界
            if (slot >= gui.getSize() - 9) break;
            
            // 创建模式选项
            ItemStack item = new ItemStack(getModeMaterial(mode, plugin));
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§6§l" + mode + "模式");
            
            // 如果是预选模式，添加特殊标记
            if (preselectedMode != null && preselectedMode.equals(mode)) {
                meta.setLore(Arrays.asList(
                    "§7点击开始匹配" + mode + "模式",
                    "§7当前可用服务器: " + getServerCount(plugin, mode) + "个",
                    "§a§l已预选"
                ));
            } else {
                meta.setLore(Arrays.asList(
                    "§7点击开始匹配" + mode + "模式",
                    "§7当前可用服务器: " + getServerCount(plugin, mode) + "个"
                ));
            }
            item.setItemMeta(meta);
            
            gui.setItem(slot, item);
            
            // 计算下一个位置
            slot++;
            if ((slot - 1) % 9 == 7) { // 如果到达行尾
                slot += 2; // 跳到下一行的第二个位置
            }
        }
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