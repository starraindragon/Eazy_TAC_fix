# Eazy TAC Fix

**中文**：一个 NeoForge 模组，修复 [DataEnergistics](https://github.com/fish1145/DataEnergistics) 三台受影响机器（数据重组器 DataRipperReassembler、数据异步处理工厂 DataAsynchronousProcessingFactory、数据充压一体机 DataIntegratedCharger）在 AE 批量制作时因材料分堆而罢工的问题。

**English**: A NeoForge mod that fixes three affected DataEnergistics machines (DataRipperReassembler, DataAsynchronousProcessingFactory, DataIntegratedCharger) from halting when AE batch-crafting spills materials into extra slots.

- **中文**：Minecraft `1.21.1` · NeoForge `21.1.248+` · Mod ID `eazy_tac_fix`
- **English**: Minecraft `1.21.1` · NeoForge `21.1.248+` · Mod ID `eazy_tac_fix`

---

## 整合包包内模组 / Pack-Internal Mod

**中文**
本模组是**整合包包内模组**：它作为整合包的一部分随整合包一起分发，通常**不单独提供独立下载**。使用者无需单独安装本模组，只要整合包带上了它，放入 `mods` 目录即可直接使用。若你在整合包中使用或分发本模组，请在整合包发布页注明包含此模组及其 AGPL 许可。

**English**
This mod is a **pack-internal mod**: it ships as part of a modpack and is generally **not distributed as a standalone download**. Users do not install it separately — once the pack includes it, it is used directly from the `mods` folder. If you use or redistribute this mod inside a modpack, please credit the mod and its AGPL license on the pack's release page.

---

## 问题 / The Problem

**中文**
**数据重组器**有 9 个物品输入槽，每格容量上限 **512**；**数据异步处理工厂**有 21 个物品输入槽；**数据充压一体机**（DataIntegratedCharger）为多输入配方机器（1–3 个输入）。当你用 `a + b = c` 这类配方批量制作（一次几千个）时，AE 自动合成/样板送料会因单格放不下而把同一种材料**分堆到多个输入槽**，界面呈现 `a, b, a` 这种排布。

这三台机器的原版逻辑都要求「**每个非空输入槽都必须被配方完全消耗**」，因此那些"多出来的 `a` 槽"会被判为不匹配，机器随即停摆，无法继续批量制作。

**English**
The **DataRipperReassembler** has 9 item input slots with a capacity limit of **512 per slot**; the **DataAsynchronousProcessingFactory** has 21 item input slots; the **DataIntegratedCharger** is a multi-input recipe machine (1–3 inputs). When you batch-craft a recipe like `a + b = c` in large quantities (thousands at once), AE auto-crafting/patterns spill the same material into multiple slots because a single slot cannot hold it all, producing a layout like `a, b, a`.

All three machines share the original logic that requires "**every non-empty input slot be fully consumed by the recipe**", so those "extra `a` slots" are treated as unmatched and the machine halts, unable to continue batch-crafting.

---

## 修复原理 / How It's Fixed

**中文**
通过三个 Mixin 覆盖三台机器的输入匹配逻辑：

- `DataRipperReassemblerRecipeMixin` → `DataRipperReassemblerRecipe.findMatchingItemInputs`（数据重组器；数据异步处理工厂继承其实体、复用同一配方逻辑，同样被覆盖）
- `DataIntegratedChargerRecipeMixin` → `DataIntegratedChargerRecipe.findMatchingInputSlots`（数据充压一体机 DataIntegratedCharger）
- `DataChargePressRecipeMixin` → `DataChargePressRecipe.findMatchingInputSlots`（数据充压，充压一体机内部模式）

三台机器共用相同的修复策略：

- **保留**：每个非空输入槽必须能被配方的某个需求识别（防止部分匹配/误触发）；
- **放宽**：不再要求每个槽被完全消耗，`maxFlow` 满足配方法所需总量即可；
- **效果**：多余的材料留在槽内不再阻塞机器，可以正常批量制作数千个产物。

> 如果你同时存在 `a = d` 和 `a + b = c` 两个配方，只要 `a`、`b` 同时在机器里就只会匹配 `a + b = c`；仅当某时刻槽里只有 `a`（例如 `b` 尚未送进）时 `a = d` 才会被触发，这与原版行为一致。

**English**
Three Mixins override the input-matching logic of the three machines:

- `DataRipperReassemblerRecipeMixin` → `DataRipperReassemblerRecipe.findMatchingItemInputs` (DataRipperReassembler; the DataAsynchronousProcessingFactory extends its block entity and reuses the same recipe logic, so it is covered too)
- `DataIntegratedChargerRecipeMixin` → `DataIntegratedChargerRecipe.findMatchingInputSlots` (DataIntegratedCharger)
- `DataChargePressRecipeMixin` → `DataChargePressRecipe.findMatchingInputSlots` (DataChargePress, a mode inside the DataIntegratedCharger)

All three share the same fix strategy:

- **Kept**: every non-empty input slot must be recognized by at least one recipe requirement (prevents partial matches / false triggers);
- **Relaxed**: each slot no longer needs to be fully consumed; the network `maxFlow` merely needs to satisfy the recipe's total required amount;
- **Result**: excess material stays in the slots without blocking the machine, so batch-crafting thousands of outputs works normally.

> If you have both `a = d` and `a + b = c` recipes, as long as `a` and `b` are present in the machine together, only `a + b = c` will match; `a = d` is only triggered when a slot momentarily holds only `a` (e.g. `b` has not been fed yet) — same behavior as the original.

---

## 依赖 / Dependencies

**中文**
- [NeoForge](https://neoforged.net/) `21.1.248+`
- [DataEnergistics](https://www.curseforge.com/minecraft/mc-mods/data-energistics) **仅兼容 `v3.3.5-1.21`**（三个 Mixin 均基于 3.3.5 的方法签名编写；若升级到 3.4.0 及以上，输入匹配方法的参数类型已改变，会导致本模组注入失败/游戏崩溃，需先移除本模组再升级；该「跨槽位数量匹配」问题已由官方 3.4.0 修复）
- Applied Energistics 2

**English**
- [NeoForge](https://neoforged.net/) `21.1.248+`
- [DataEnergistics](https://www.curseforge.com/minecraft/mc-mods/data-energistics) **only compatible with `v3.3.5-1.21`** (all three Mixins target 3.3.5 method signatures; upgrading to 3.4.0+ changes the input-matching method parameter types, which breaks the injection / crashes the game — remove this mod before upgrading; this "cross-slot quantity matching" issue is already fixed in official 3.4.0)
- Applied Energistics 2

---

## 安装 / Installation

**中文**：把打包好的 jar 放入整合包的 `mods` 目录，启动游戏即可。作为整合包包内模组，随包分发时无需用户额外操作。

**English**: Place the built jar into the modpack's `mods` folder and start the game. As a pack-internal mod, no extra step is required when it ships with the pack.

---

## 构建 / Building

**中文**
本仓库不包含编译产物。在工程目录执行：

```bash
./gradlew build
```

构建产物位于 `build/libs/eazy_tac_fix-<version>.jar`。构建前请确认 `build.gradle` 中 `compileOnly` 引用的 `data_energistics-*.jar` 路径正确。

**English**
This repository does not include build artifacts. From the project directory run:

```bash
./gradlew build
```

The built jar is at `build/libs/eazy_tac_fix-<version>.jar`. Make sure the `data_energistics-*.jar` referenced by `compileOnly` in `build.gradle` points to the correct path before building.

---

## 许可 / License

**中文**
本项目是 [DataEnergistics](https://github.com/fish1145/DataEnergistics)（代码采用 **GNU AGPL-3.0-or-later**）的衍生作品，故本项目代码同样以 **GNU Affero General Public License v3.0 or later** 授权，详见 [LICENSE](LICENSE)。

**English**
This project is a derivative work of [DataEnergistics](https://github.com/fish1145/DataEnergistics) (code licensed under **GNU AGPL-3.0-or-later**); therefore this project's code is likewise licensed under the **GNU Affero General Public License v3.0 or later**. See [LICENSE](LICENSE).

---

## 作者 / Author

**中文**：[xing_yu_dragon](https://github.com/starraindragon)

**English**: [xing_yu_dragon](https://github.com/starraindragon)
