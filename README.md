# Eazy TAC Fix

一个 NeoForge 模组，修复 [DataEnergistics](https://github.com/fish1145/DataEnergistics)「数据重组器（DataRipperReassembler）」在 **AE 批量制作**时因材料分堆而罢工的问题。

- **Minecraft**：1.21.1
- **Mod Loader**：NeoForge 21.1.248 或更高
- **Mod ID**：`eazy_tac_fix`

---

## 问题

数据重组器有 9 个物品输入槽，每格容量上限 **512**。当你用 `a + b = c` 这类配方批量制作（一次几千个）时，AE 自动合成/样板送料会因单格放不下而把同一种材料**分堆到多个输入槽**，界面呈现 `a, b, a` 这种排布。

原版逻辑要求「**每个非空输入槽都必须被配方完全消耗**」，因此那些"多出来的 `a` 槽"会被判为不匹配，机器随即停摆，无法继续批量制作。

## 修复原理

通过 Mixin 覆盖了 `DataRipperReassemblerRecipe.findMatchingItemInputs` 的匹配逻辑：

- **保留**：每个非空输入槽必须能被配方的某个需求识别（防止部分匹配/误触发）；
- **放宽**：不再要求每个槽被完全消耗，`maxFlow` 满足配方法所需总量即可；
- **效果**：多余的材料留在槽内不再阻塞机器，可以正常批量制作数千个产物。

> 如果你同时存在 `a = d` 和 `a + b = c` 两个配方，只要 `a`、`b` 同时在机器里就只会匹配 `a + b = c`；仅当某时刻槽里只有 `a`（例如 `b` 尚未送进）时 `a = d` 才会被触发，这与原版行为一致。

## 依赖

- [NeoForge](https://neoforged.net/) `21.1.248+`
- [DataEnergistics](https://www.curseforge.com/minecraft/mc-mods/data-energistics) `3.3.5`
- Applied Energistics 2

## 安装

把打包好的 jar 放入整合包的 `mods` 目录，启动游戏即可。

## 构建

本仓库不包含编译产物。在工程目录执行：

```bash
./gradlew build
```

构建产物位于 `build/libs/eazy_tac_fix-<version>.jar`。构建前请确认 `build.gradle` 中 `compileOnly` 引用的 `data_energistics-*.jar` 路径正确。

## 许可

本项目是 [DataEnergistics](https://github.com/fish1145/DataEnergistics)（代码采用 **GNU AGPL-3.0-or-later**）的衍生作品，故本项目代码同样以 **GNU Affero General Public License v3.0 or later** 授权，详见 [LICENSE](LICENSE)。

## 作者

[xing_yu_dragon](https://github.com/starraindragon)
