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
        Socket socket = null;
        try {
            plugin.getLogger().info("正在连接到服务器 " + host + ":" + port + " 获取MOTD信息");
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 5000); // 5秒超时
            
            plugin.getLogger().info("成功连接到服务器 " + host + ":" + port + "，正在获取MOTD信息");
            
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            DataInputStream input = new DataInputStream(socket.getInputStream());
            
            // 发送握手包
            plugin.getLogger().info("发送握手包到服务器 " + host + ":" + port);
            output.write(0x00); // packet ID
            // 写入协议版本 (使用340表示1.12.2版本)
            writeVarInt(output, 340); // 1.12.2的协议版本号
            // 写入服务器地址
            writeString(output, host);
            // 写入服务器端口
            output.writeShort(port);
            // 写入状态
            writeVarInt(output, 1); // 1 = 状态请求
            
            // 发送状态请求
            plugin.getLogger().info("发送状态请求到服务器 " + host + ":" + port);
            output.write(0x00); // packet ID
            writeVarInt(output, 0); // 数据长度
            
            // 读取响应
            plugin.getLogger().info("等待服务器响应...");
            try {
                // 检查是否有数据可读
                if (input.available() <= 0) {
                    plugin.getLogger().warning("服务器 " + serverName + " 没有返回任何数据");
                    return false;
                }
                
                int length = readVarInt(input);
                plugin.getLogger().info("收到响应长度: " + length);
                
                // 检查长度是否有效
                if (length <= 0) {
                    plugin.getLogger().warning("服务器 " + serverName + " 返回了无效的响应长度: " + length);
                    return false;
                }
                
                int packetID = readVarInt(input);
                plugin.getLogger().info("收到数据包ID: " + packetID);
                
                if (packetID == 0x00) { // 状态响应
                    int responseLength = readVarInt(input);
                    plugin.getLogger().info("收到响应数据长度: " + responseLength);
                    if (responseLength > 0) {
                        // 确保不会读取超过可用的数据
                        if (responseLength > input.available()) {
                            plugin.getLogger().warning("服务器 " + serverName + " 声明的响应长度大于实际可用数据");
                            return false;
                        }
                        
                        byte[] responseData = new byte[responseLength];
                        input.readFully(responseData);
                        
                        String jsonResponse = new String(responseData, "UTF-8");
                        plugin.getLogger().info("从服务器 " + host + ":" + port + " 接收到响应: " + jsonResponse);
                        
                        // 简单解析JSON响应并提取MOTD
                        // 由于没有Gson库，我们使用简单的字符串解析
                        int descStart = jsonResponse.indexOf("\"description\":\"");
                        if (descStart != -1) {
                            descStart += 14; // 跳过"\"description\":\""
                            int descEnd = jsonResponse.indexOf("\"", descStart);
                            if (descEnd != -1) {
                                String motd = jsonResponse.substring(descStart, descEnd);
                                // 处理转义字符
                                motd = motd.replace("\\\"", "\"");
                                motd = motd.replace("\\\\", "\\");
                                motd = motd.replace("\\n", "\n");
                                plugin.getLogger().info("服务器 " + serverName + " 返回的MOTD: " + motd);
                                if (motd.contains("等待中")) {
                                    plugin.getLogger().info("服务器 " + serverName + " 包含\"等待中\"关键词，标记为可用");
                                    return true;
                                }
                                plugin.getLogger().info("服务器 " + serverName + " 不包含\"等待中\"关键词");
                                return false;
                            }
                        }
                        
                        // 处理复杂的MOTD格式 (包含"text"字段)
                        int textStart = jsonResponse.indexOf("\"text\":\"");
                        if (textStart != -1) {
                            textStart += 8; // 跳过"\"text\":\""
                            int textEnd = jsonResponse.indexOf("\"", textStart);
                            if (textEnd != -1) {
                                String motd = jsonResponse.substring(textStart, textEnd);
                                // 处理转义字符
                                motd = motd.replace("\\\"", "\"");
                                motd = motd.replace("\\\\", "\\");
                                motd = motd.replace("\\n", "\n");
                                plugin.getLogger().info("服务器 " + serverName + " 返回的MOTD: " + motd);
                                if (motd.contains("等待中")) {
                                    plugin.getLogger().info("服务器 " + serverName + " 包含\"等待中\"关键词，标记为可用");
                                    return true;
                                }
                                plugin.getLogger().info("服务器 " + serverName + " 不包含\"等待中\"关键词");
                                return false;
                            }
                        }
                        
                        plugin.getLogger().warning("无法从服务器 " + serverName + " 的响应中解析MOTD");
                        return false;
                    } else {
                        plugin.getLogger().warning("服务器 " + serverName + " 返回了空的响应");
                        return false;
                    }
                } else {
                    plugin.getLogger().warning("服务器 " + serverName + " 返回了无效的数据包ID: " + packetID);
                    return false;
                }
            } catch (IOException e) {
                plugin.getLogger().warning("读取服务器 " + serverName + " 响应时出错: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                return false;
            }
        } catch (IOException e) {
            plugin.getLogger().warning("无法连接到服务器 " + serverName + " (" + host + ":" + port + "): " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace(); // 打印完整的堆栈跟踪以帮助诊断问题
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("检查服务器 " + serverName + " 时发生未知错误: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace(); // 打印完整的堆栈跟踪以帮助诊断问题
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    plugin.getLogger().warning("关闭到服务器 " + serverName + " 的连接时出错: " + e.getMessage());
                }
            }
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
        int size = 0;
        byte b;
        
        while (((b = input.readByte()) & 0x80) == 0x80) {
            value |= (b & 0x7F) << (size++ * 7);
            
            if (size > 5) {
                throw new RuntimeException("VarInt越界");
            }
        }
        
        return value | ((b & 0x7F) << (size * 7));
    }
}