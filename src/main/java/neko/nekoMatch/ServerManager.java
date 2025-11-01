package neko.nekoMatch;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ServerManager {
    private final JavaPlugin plugin;
    
    public ServerManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 重新初始化服务器管理器
     */
    public void reinitialize() {
        // 重新加载配置等操作
        plugin.reloadConfig();
    }
    
    /**
     * 连接玩家到指定服务器
     */
    public void connectToServer(Player player, String serverName) {
        // 检查玩家是否在线
        if (player == null || !player.isOnline()) {
            plugin.getLogger().warning("无法连接玩家到服务器 " + serverName + "，玩家不在线或为null");
            return;
        }
        
        plugin.getLogger().info("正在尝试将玩家 " + player.getName() + " 连接到服务器 " + serverName);
        
        // 使用BungeeCord的插件消息通道连接到Velocity服务器
        try {
            // 构造正确的连接消息格式
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            
            player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
            plugin.getLogger().info("已发送连接消息到玩家 " + player.getName() + "，目标服务器: " + serverName);
        } catch (Exception e) {
            plugin.getLogger().warning("无法连接玩家 " + player.getName() + " 到服务器 " + serverName + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 通过MOTD检查服务器状态，选择合适的服务器（仅选择包含"等待中"的服务器用于自动匹配）
     */
    public String selectAvailableServer(String mode) {
        FileConfiguration config = plugin.getConfig();
        List<String> serverList = config.getStringList("modes." + mode + ".servers");
        
        plugin.getLogger().info("正在为模式 " + mode + " 选择服务器，共有 " + serverList.size() + " 个服务器可选");
        
        // 只选择MOTD包含"等待中"关键词的服务器
        for (String serverName : serverList) {
            plugin.getLogger().info("正在检查服务器 " + serverName + " 是否在等待中");
            if (isServerWaiting(serverName)) {
                plugin.getLogger().info("找到可用服务器: " + serverName);
                return serverName;
            }
        }
        
        plugin.getLogger().warning("没有找到MOTD包含\"等待中\"关键词的服务器用于模式 " + mode);
        return null;
    }
    
    /**
     * 检查服务器是否处于等待状态（MOTD包含"等待中"关键词）
     */
    public boolean isServerWaiting(String serverName) {
        // 从配置中获取服务器地址和端口信息
        String serverAddress = getServerAddress(serverName);
        
        if (serverAddress == null || serverAddress.isEmpty()) {
            plugin.getLogger().warning("无法找到服务器 " + serverName + " 的地址信息");
            return false;
        }
        
        plugin.getLogger().info("正在检查服务器 " + serverName + " (" + serverAddress + ") 的状态");
        
        // 分离地址和端口
        String[] parts = serverAddress.split(":");
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 25565; // 默认端口25565
        
        // 验证主机和端口
        if (host == null || host.isEmpty()) {
            plugin.getLogger().warning("服务器 " + serverName + " 的主机地址无效");
            return false;
        }
        
        if (port <= 0 || port > 65535) {
            plugin.getLogger().warning("服务器 " + serverName + " 的端口无效: " + port);
            return false;
        }
        
        // 首先尝试简单的TCP连接检查
        if (!isServerReachable(host, port)) {
            plugin.getLogger().warning("服务器 " + serverName + " (" + host + ":" + port + ") 不可达");
            return false;
        }
        
        // 如果TCP连接成功，再尝试获取MOTD信息
        return checkServerMOTD(serverName, host, port);
    }
    
    /**
     * 检查服务器是否可达
     */
    private boolean isServerReachable(String host, int port) {
        plugin.getLogger().info("正在检查服务器 " + host + ":" + port + " 是否可达");
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 3000); // 3秒超时
            plugin.getLogger().info("服务器 " + host + ":" + port + " 可达");
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("服务器 " + host + ":" + port + " 不可达: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查服务器MOTD信息
     */
    private boolean checkServerMOTD(String serverName, String host, int port) {
        try (Socket socket = new Socket()) {
            plugin.getLogger().info("正在连接到服务器 " + host + ":" + port + " 获取MOTD信息");
            socket.connect(new InetSocketAddress(host, port), 5000); // 5秒超时
            
            plugin.getLogger().info("成功连接到服务器 " + host + ":" + port + "，正在获取MOTD信息");
            
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            // === Handshake ===
            ByteArrayOutputStream handshake_bytes = new ByteArrayOutputStream();
            DataOutputStream handshake = new DataOutputStream(handshake_bytes);

            writeVarInt(handshake, 0x00); // packet id
            writeVarInt(handshake, 340); // protocol 1.12.2
            writeString(handshake, host);
            handshake.writeShort(port);
            writeVarInt(handshake, 1); // next state: status

            // 写入包长度 + 包体
            writeVarInt(out, handshake_bytes.size());
            out.write(handshake_bytes.toByteArray());

            // === 请求状态包 ===
            out.writeByte(1); // length
            out.writeByte(0); // packet id

            // === 读取响应 ===
            int size = readVarInt(in);
            int packetId = readVarInt(in);
            
            if (packetId != 0x00) { // 状态响应
                plugin.getLogger().warning("服务器 " + serverName + " 返回了无效的数据包ID: " + packetId);
                return false;
            }
            
            int stringLength = readVarInt(in);
            if (stringLength <= 0) {
                plugin.getLogger().warning("服务器 " + serverName + " 返回了无效的响应长度: " + stringLength);
                return false;
            }
            
            byte[] data = new byte[stringLength];
            in.readFully(data);

            String json = new String(data, "UTF-8");
            plugin.getLogger().info("从服务器 " + host + ":" + port + " 接收到响应: " + json);
            
            JsonObject obj = new JsonParser().parse(json).getAsJsonObject();
            JsonObject description = obj.getAsJsonObject("description");
            
            String motd;
            if (description.has("text")) {
                motd = description.get("text").getAsString();
            } else {
                motd = description.toString();
            }
            
            plugin.getLogger().info("服务器 " + serverName + " 返回的MOTD: " + motd);
            return motd.contains("等待中");
        } catch (Exception e) {
            plugin.getLogger().warning("检查服务器 " + serverName + " 时发生错误: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace(); // 打印完整的堆栈跟踪以帮助诊断问题
            return false;
        }
    }
    
    /**
     * 从配置文件获取服务器地址
     */
    private String getServerAddress(String serverName) {
        FileConfiguration config = plugin.getConfig();
        
        // 尝试从配置中获取服务器地址
        String address = config.getString("servers." + serverName + ".address");
        if (address != null && !address.isEmpty()) {
            return address;
        }
        
        // 如果配置中没有单独的地址信息，则返回默认端口的localhost
        return "localhost:25565"; // 这需要在实际部署时修改为正确地址
    }
    
    /**
     * 检查服务器是否可用（用于手动连接，只要服务器可达就可以连接）
     */
    public boolean isServerAvailable(String serverName) {
        try {
            // 对于手动连接，只要服务器可达就可以连接
            String serverAddress = getServerAddress(serverName);
            if (serverAddress != null && !serverAddress.isEmpty()) {
                String[] parts = serverAddress.split(":");
                String host = parts[0];
                int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 25565;
                return isServerReachable(host, port);
            }
            
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("检查服务器 " + serverName + " 状态时出错: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 写入变长整数到输出流
     */
    private void writeVarInt(DataOutputStream output, int value) throws IOException {
        while (true) {
            if ((value & 0xFFFFFF80) == 0) {
                output.writeByte(value);
                return;
            }
            
            output.writeByte(value & 0x7F | 0x80);
            value >>>= 7;
        }
    }
    
    /**
     * 写入字符串到输出流
     */
    private void writeString(DataOutputStream output, String value) throws IOException {
        byte[] bytes = value.getBytes("UTF-8");
        writeVarInt(output, bytes.length);
        output.write(bytes);
    }
    
    /**
     * 从输入流读取变长整数
     */
    private int readVarInt(DataInputStream input) throws IOException {
        int value = 0;
        int position = 0;
        byte currentByte;
        
        while (true) {
            currentByte = input.readByte();
            value |= (currentByte & 0x7F) << (position * 7);
            
            if ((currentByte & 0x80) == 0) break;
            
            position++;
            if (position > 5) throw new RuntimeException("VarInt越界");
        }
        
        return value;
    }
}