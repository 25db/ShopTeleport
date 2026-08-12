# ShopTeleport

🎮 一个支持 Folia 1.21.1 的商店传送插件

## 功能特性

- ✅ **OP 设置商店** - OP 玩家可以在当前位置设置商店传送点
- ✅ **OP 删除商店** - OP 玩家可以删除已设置的商店
- ✅ **玩家传送** - 普通玩家可以传送到已设置的商店
- ✅ **商店列表** - 查看所有已设置的商店
- ✅ **Folia 兼容** - 使用 Folia 的异步 API 进行传送
- ✅ **数据持久化** - 商店位置自动保存到配置文件

## 安装方法

1. 从 [Releases](../../releases) 页面下载最新版本的 JAR 文件
2. 将 JAR 文件放入服务器的 `plugins` 文件夹
3. 重启服务器或使用插件管理器加载

## 命令

| 命令 | 描述 | 权限 |
|------|------|------|
| `/shop set <名称>` | 在当前位置设置商店 | 仅 OP |
| `/shop delete <名称>` | 删除指定商店 | 仅 OP |
| `/shop list` | 列出所有商店 | 所有玩家 |
| `/shop tp <名称>` | 传送到指定商店 | 所有玩家 |
| `/shop reload` | 重载配置 | 仅 OP |
| `/shop help` | 查看帮助 | 所有玩家 |

## 使用示例

### 设置商店 (仅 OP)
```
/shop set 主城商店
```

### 查看商店列表
```
/shop list
```

### 传送到商店
```
/shop tp 主城商店
```

### 删除商店 (仅 OP)
```
/shop delete 主城商店
```

## 权限节点

| 权限 | 描述 | 默认 |
|------|------|------|
| `shopteleport.admin` | 设置和删除商店的权限 | OP |
| `shopteleport.use` | 传送到商店的权限 | 所有玩家 |

## 技术信息

- **支持版本**: Minecraft 1.21.1
- **服务端类型**: Folia (兼容 Paper/Spigot)
- **Java 版本**: Java 21+
- **API**: Paper API 1.21.1-R0.1-SNAPSHOT

## 构建项目

```bash
mvn clean package
```

构建完成后，JAR 文件位于 `target/ShopTeleport-1.0.0.jar`

## 许可证

本项目仅供学习和个人使用。

## 作者

- GitHub: [25db](https://github.com/25db)