好的，要让你这段 **服务器 MOTD 检查逻辑** 支持 **Paper 1.8.8/1.8.9（协议版本 47）**，你只需要修改握手（Handshake）和状态请求（Status Request）部分的封包格式参数即可。

下面是 **1.8.9 协议的关键区别**：

---

### 🧩 Minecraft 1.8.8/1.8.9 协议规范（Protocol 47）

* **协议号 (Protocol Version)：** `47`
* **握手包 (Handshake)**：

  ```
  Packet ID: 0x00
  Fields:
    - protocol version (VarInt)
    - server address (String)
    - server port (Unsigned Short)
    - next state (VarInt)
  ```
* **状态请求包 (Request)**：

  ```
  Packet ID: 0x00
  没有额外字段
  ```
* **响应包 (Response)**：

  ```
  Packet ID: 0x00
  String JSON
  ```

---

### 🔧 修改要点

只要改动这两行：

```java
writeVarInt(handshake, 340); // protocol 1.12.2
```

改为：

```java
writeVarInt(handshake, 47); // protocol 1.8.8/1.8.9
```

---

### ✅ 完整修改示例（节选）

```java
// === Handshake ===
ByteArrayOutputStream handshake_bytes = new ByteArrayOutputStream();
DataOutputStream handshake = new DataOutputStream(handshake_bytes);

writeVarInt(handshake, 0x00); // packet id
writeVarInt(handshake, 47); // protocol 1.8.9
writeString(handshake, host);
handshake.writeShort(port);
writeVarInt(handshake, 1); // next state: status

writeVarInt(out, handshake_bytes.size());
out.write(handshake_bytes.toByteArray());

// === 请求状态包 ===
out.writeByte(1); // length
out.writeByte(0); // packet id
```

---

### 🧠 可选优化（防止旧版崩溃）

旧版（尤其是 1.8.x）的返回 JSON 结构可能是：

```json
{"description":"§a等待中","players":{...},"version":{...}}
```

因此建议你的读取部分稍作容错：

```java
JsonObject obj = new JsonParser().parse(json).getAsJsonObject();
String motd = "";

if (obj.has("description")) {
    if (obj.get("description").isJsonPrimitive()) {
        motd = obj.get("description").getAsString();
    } else if (obj.get("description").isJsonObject()) {
        JsonObject desc = obj.getAsJsonObject("description");
        if (desc.has("text")) motd = desc.get("text").getAsString();
        else motd = desc.toString();
    }
}
```

---

这样就能兼容 Paper 1.8.8/1.8.9 的 MOTD 状态协议查询，其他逻辑（如 `isServerReachable`、`selectAvailableServer`）都不用改。
