# NiceHUD

一个 Minecraft Fabric 模组，将 MiniHUD 的信息项渲染为可拖拽的精美 HUD 元素。

> Minecraft 1.21.8 · Fabric Loader 0.18.4 · NiceHUD v1.5.0

---

## 功能特性

- **54+ MiniHUD 信息项**可独立拖拽定位，每项均有半透明圆角背景
- **中文标签**覆盖所有信息项（群系名、维度名、方块属性等全部本地化）
- **快捷栏 / 背包**显示，支持横向与纵向布局，物品图标 + 耐久条 + 数量
- **装备耐久**条形图显示，头盔/胸甲/护腿/靴子/主手/副手
- **状态效果**列表，效果图标 + 剩余时间，快到期时颜色变红并闪烁
- **编辑器**（H 键打开）：拖拽移动、框选多选、滚轮缩放、右键菜单、组合分组、吸附辅助线
- 面板可一键隐藏，全屏自由布局

## 截图

> *(游戏中按 H 键打开编辑器，拖拽元素到喜欢的位置)*

## 依赖

| 模组 | 版本 |
|------|------|
| [Fabric API](https://modrinth.com/mod/fabric-api) | 0.136.1+1.21.8 |
| [MiniHUD](https://www.curseforge.com/minecraft/mc-mods/minihud) | 0.36.7 (1.21.8) |
| [Malilib](https://www.curseforge.com/minecraft/mc-mods/malilib) | 0.25.7 (1.21.8) |

## 安装

1. 安装 [Fabric Loader](https://fabricmc.net/use/) 0.18.4+
2. 将以上依赖模组放入 `.minecraft/mods/`
3. 将 `nicehud-1.5.0.jar` 放入 `.minecraft/mods/`
4. 启动游戏，进入存档后按 **H** 键打开编辑器

## 从源码构建

```bash
git clone https://github.com/Justice-ocr/nicehud.git
cd nicehud
# 将依赖 JAR 放入 libs/
./gradlew build
# 产物: build/libs/nicehud-1.5.0.jar
```

所需依赖 JAR（放入 `libs/`）：
- `fabric-api-0.136.1+1.21.8.jar`
- `minihud-fabric-1.21.8-0.36.7.jar`
- `malilib-fabric-1.21.8-0.25.7.jar`
- `cloth-config-19.0.147-fabric.jar`

## 键位

| 按键 | 功能 |
|------|------|
| **H** | 打开 / 关闭编辑器 |
| 左键拖拽 | 移动元素 |
| 右键 | 单个元素菜单（缩放 / 分组 / 横纵向） |
| 滚轮 | 缩放悬停元素 |
| 框选 + 右键 | 批量操作 |

## 许可证

MIT License

---

*由 [Justice-ocr](https://github.com/Justice-ocr) 开发*
