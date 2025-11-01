package neko.nekoMatch;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class MinecraftServerStatus {

    public static class ServerInfo {
        public String motd;
        public int currentPlayers;
        public int maxPlayers;

        public ServerInfo(String motd, int currentPlayers, int maxPlayers) {
            this.motd = motd;
            this.currentPlayers = currentPlayers;
            this.maxPlayers = maxPlayers;
        }
    }

    /**
     * 获取Minecraft服务器状态
     */
    public static ServerInfo getServerStatus(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 3000); // 3秒超时

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            // 发送握手包
            sendHandshake(out, host, port);
            
            // 请求服务器状态
            sendStatusRequest(out);
            
            // 读取响应
            String response = readStatusResponse(in);
            
            // 解析JSON响应
            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
            String motd = jsonResponse.getAsJsonObject("description").get("text").getAsString();
            int currentPlayers = jsonResponse.getAsJsonObject("players").get("online").getAsInt();
            int maxPlayers = jsonResponse.getAsJsonObject("players").get("max").getAsInt();

            return new ServerInfo(motd, currentPlayers, maxPlayers);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void sendHandshake(DataOutputStream out, String host, int port) throws IOException {
        // 构建握手包
        writeVarInt(out, 0x00); // 包ID
        writeVarInt(out, 4); // 协议版本
        writeString(out, host);
        out.writeShort(port);
        writeVarInt(out, 1); // 状态请求
    }

    private static void sendStatusRequest(DataOutputStream out) throws IOException {
        writeVarInt(out, 0x00); // 包ID
    }

    private static String readStatusResponse(DataInputStream in) throws IOException {
        readVarInt(in); // 忽略包长度
        int packetId = readVarInt(in);
        if (packetId != 0x00) {
            throw new IOException("意外的包ID: " + packetId);
        }
        return readString(in);
    }

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & 0xFFFFFF80) != 0) {
            out.writeByte(value & 0xFF | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int value = 0;
        int position = 0;
        byte currentByte;

        while (true) {
            currentByte = in.readByte();
            value |= (currentByte & 0x7F) << position;

            if ((currentByte & 0x80) == 0) break;

            position += 7;

            if (position >= 32) throw new RuntimeException("VarInt越界");
        }

        return value;
    }

    private static void writeString(DataOutputStream out, String string) throws IOException {
        byte[] bytes = string.getBytes("UTF-8");
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = readVarInt(in);
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, "UTF-8");
    }
}