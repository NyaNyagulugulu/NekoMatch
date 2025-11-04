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
    
    // 服务器状态枚举
    public enum ServerStatus {
        DEVELOPING,  // 开发中
        PLAYING,     // 游戏中
        WAITING,     // 等待中
        OFFLINE      // 离线
    }
    
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
        plugin.getLogger().info("开始为模式 " + mode + " 选择服务器");
        FileConfiguration config = plugin.getConfig();
        if (!config.contains("modes." + mode)) {
            plugin.getLogger().warning("配置文件中不存在模式: " + mode);
            return null;
        }
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
        ServerStatus status = getServerStatus(serverName);
        return status == ServerStatus.WAITING;
    }
    
    /**
     * 获取服务器状态
     */
    public ServerStatus getServerStatus(String serverName) {
        // 从配置中获取服务器地址和端口信息
        String serverAddress = getServerAddress(serverName);
        
        if (serverAddress == null || serverAddress.isEmpty()) {
            plugin.getLogger().warning("无法找到服务器 " + serverName + " 的地址信息");
            return ServerStatus.OFFLINE;
        }
        
        plugin.getLogger().info("正在检查服务器 " + serverName + " (" + serverAddress + ") 的状态");
        
        // 分离地址和端口
        String[] parts = serverAddress.split(":");
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 25565; // 默认端口25565
        
        // 验证主机和端口
        if (host == null || host.isEmpty()) {
            plugin.getLogger().warning("服务器 " + serverName + " 的主机地址无效");
            return ServerStatus.OFFLINE;
        }
        
        if (port <= 0 || port > 65535) {
            plugin.getLogger().warning("服务器 " + serverName + " 的端口无效: " + port);
            return ServerStatus.OFFLINE;
        }
        
        // 首先尝试简单的TCP连接检查
        if (!isServerReachable(host, port)) {
            plugin.getLogger().warning("服务器 " + serverName + " (" + host + ":" + port + ") 不可达");
            return ServerStatus.OFFLINE;
        }
        
        // 如果TCP连接成功，再尝试获取MOTD信息
        return getServerMOTDStatus(serverName, host, port);
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
     * 获取服务器MOTD状态
     */
    private ServerStatus getServerMOTDStatus(String serverName, String host, int port) {
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
            //writeVarInt(handshake, 340); // protocol 1.12.2
            writeVarInt(handshake, 47); // protocol 1.8.9 / 1.8.8
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
                return ServerStatus.OFFLINE;
            }
            
            int stringLength = readVarInt(in);
            if (stringLength <= 0) {
                plugin.getLogger().warning("服务器 " + serverName + " 返回了无效的响应长度: " + stringLength);
                return ServerStatus.OFFLINE;
            }
            
            byte[] data = new byte[stringLength];
            in.readFully(data);

            String json = new String(data, "UTF-8");
            plugin.getLogger().info("从服务器 " + host + ":" + port + " 接收到响应: " + json);
            
            JsonObject obj = new JsonParser().parse(json).getAsJsonObject();
            String motd = "";
            
            // 兼容不同版本的MOTD格式
            if (obj.has("description")) {
                if (obj.get("description").isJsonPrimitive()) {
                    // 1.8.x 可能直接返回字符串
                    motd = obj.get("description").getAsString();
                } else if (obj.get("description").isJsonObject()) {
                    // 1.12+ 格式
                    JsonObject description = obj.getAsJsonObject("description");
                    if (description.has("text")) {
                        motd = description.get("text").getAsString();
                    } else {
                        motd = description.toString();
                    }
                }
            } else {
                // 如果没有description字段，尝试获取直接的motd字段
                if (obj.has("motd")) {
                    motd = obj.get("motd").getAsString();
                } else {
                    // 如果没有找到MOTD字段，可能是格式问题，返回空字符串
                    motd = "";
                }
            }
            
            plugin.getLogger().info("服务器 " + serverName + " 返回的MOTD: " + motd);
            
            // 根据MOTD内容判断服务器状态
            if (motd.contains("开发中")) {
                return ServerStatus.DEVELOPING;
            } else if (motd.contains("游戏中")) {
                return ServerStatus.PLAYING;
            } else if (motd.contains("等待中")) {
                return ServerStatus.WAITING;
            } else {
                // 如果MOTD不包含任何特定状态关键词，但MOTD本身是有效的
                // 这种情况下我们返回WAITING作为默认可连接状态，因为服务器是可达的
                plugin.getLogger().info("服务器 " + serverName + " 的MOTD不包含特定状态关键词，视为可连接状态");
                return ServerStatus.WAITING;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("检查服务器 " + serverName + " 时发生错误: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace(); // 打印完整的堆栈跟踪以帮助诊断问题
            return ServerStatus.OFFLINE;
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
     * 检查服务器是否可用（用于手动连接）
     */
    public boolean isServerAvailable(String serverName) {
        ServerStatus status = getServerStatus(serverName);
        // 服务器可用的条件：不是离线状态且不是开发中状态
        // 即使服务器在游戏中，也可以显示在服务器列表中（但不允许手动加入）
        return status != ServerStatus.OFFLINE && status != ServerStatus.DEVELOPING;
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