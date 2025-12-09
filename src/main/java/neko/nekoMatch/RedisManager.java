package neko.nekoMatch;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import org.bukkit.configuration.file.FileConfiguration;

public class RedisManager {
    private JedisPool jedisPool;
    private NekoMatch plugin;

    public RedisManager(NekoMatch plugin) {
        this.plugin = plugin;
        initializeRedis();
    }

    private void initializeRedis() {
        FileConfiguration config = plugin.getConfig();
        
        // 从配置文件获取Redis设置，如果没有则使用默认值
        String host = config.getString("redis.host", "localhost");
        int port = config.getInt("redis.port", 6379);
        String password = config.getString("redis.password", "");
        int database = config.getInt("redis.database", 0);
        
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);
        
        if (password != null && !password.isEmpty()) {
            this.jedisPool = new JedisPool(poolConfig, host, port, 2000, password, database);
        } else {
            this.jedisPool = new JedisPool(poolConfig, host, port);
        }
        
        plugin.getLogger().info("Redis连接池已初始化 - " + host + ":" + port);
    }

    public void saveServerStatusToCache(String mode, String serverName, String status, int playerCount) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "server_status:" + mode + ":" + serverName;
            // 存储服务器状态和玩家数量
            jedis.hset(key, "status", status);
            jedis.hset(key, "playerCount", String.valueOf(playerCount));
            // 设置30秒过期时间
            jedis.expire(key, 30);
        } catch (Exception e) {
            plugin.getLogger().warning("保存服务器状态到Redis失败: " + e.getMessage());
        }
    }

    public String getServerStatusFromCache(String mode, String serverName) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "server_status:" + mode + ":" + serverName;
            return jedis.hget(key, "status");
        } catch (Exception e) {
            plugin.getLogger().warning("从Redis获取服务器状态失败: " + e.getMessage());
            return null;
        }
    }

    public Integer getServerPlayerCountFromCache(String mode, String serverName) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "server_status:" + mode + ":" + serverName;
            String playerCountStr = jedis.hget(key, "playerCount");
            if (playerCountStr != null) {
                try {
                    return Integer.parseInt(playerCountStr);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        } catch (Exception e) {
            plugin.getLogger().warning("从Redis获取服务器玩家数量失败: " + e.getMessage());
            return null;
        }
    }

    public boolean isServerStatusCached(String mode, String serverName) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "server_status:" + mode + ":" + serverName;
            return jedis.exists(key);
        } catch (Exception e) {
            plugin.getLogger().warning("检查Redis缓存状态失败: " + e.getMessage());
            return false;
        }
    }

    public void close() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }
}