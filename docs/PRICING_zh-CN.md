# 可选模组定价

[English](PRICING.md) · [首页](../README_zh-CN.md)

按顺序运行以下命令重建价格：

```sh
python tools/generate_values.py client.jar /path/to/tacz
python tools/generate_mod_values.py client.jar /path/to/tacz travelersbackpack.jar
python tools/generate_armory.py
python tools/generate_ecosystem_values.py client.jar farmersdelight=fd.jar moredelight=more.jar rusticdelight=rustic.jar ubesdelight=ube.jar betternether=nether.jar betterend=end.jar
```

输入为固定的 26.2 原版客户端、TaCZ 源码提交 `05ec226310545d89cec9169a10105e32d1ed262b` 和旅行者背包 Fabric 26.2-11.3.2。背包 JAR 校验 SHA-256。工具仅读取物品、标签、配方事实，不捆绑上游代码或素材。

普通配方按最低可用材料价格和产出数量计算，自引用复制配方用代数求解。购买单价向上取整，转化保留分数、整批向下取整。每个方案检查全部可解析配方约束。背包条件配方保守采用已定义配方中的最低成本；未列入适配范围的 Comforts 配方跳过。

服务端按安装情况加载 `data/energyexchange/energyexchange/compat/` 下的 `tacz.json`、`travelersbackpack.json` 或 `tacz-travelersbackpack.json`。每个文件包含 `values` 和 `salvage`，边界同默认规则。单物品 `values/` 和型号 `variants/` 数据包随后覆盖，始终优先。该方案在启动／重载时选择，不会实时计算任意第三方机器配方。

0.3.4 另提供可叠加模块方案。服务端读取 `compat/modules.json`，检测已安装的模组 ID，再把 `compat/modules/` 下的对应文件叠加到 TaCZ／背包方案之后、单物品数据包规则之前。因此任意组合都可工作，不需要为每种组合预制一个文件。

| 模组 ID | 项目 | 物品 ID 数 |
| --- | --- | ---: |
| `farmersdelight` | Farmer's Delight Refabricated（农夫乐事） | 186 |
| `moredelight` | More Delight | 33 |
| `rusticdelight` | Rustic Delight | 154 |
| `ubesdelight` | Ube's Delight | 122 |
| `betternether` | BetterNether | 708 |
| `betterend` | BetterEnd | 850 |

生态定价生成器解析原版配方，并支持农夫乐事烹饪／切割、Ube 烘焙垫、BetterEnd 灌注，以及 BCLib 合金／锻造配方。多产物拆解需要联合分配价格，不作为独立等式；对应的正向配方仍会约束原料与成品。无法解析的可选跨模组标签只跳过相关配方。每个从 `assets/<模组>/items/*.json` 发现的物品身份都有正数后备价，不会因一条配方无法解析而消失。固定的上游版本与 SHA-256 记录在 `versions/price-sources.json`，不捆绑任何上游代码或素材。

| 示例 | 0.2 购买价 | 0.2.1 共同安装时购买价／转化收益 |
| --- | ---: | ---: |
| TaCZ AK-47 | 11584 | 11432／11432 |
| TaCZ 7.62×39 弹药 | 71 | 8／278÷35 |
| 安装 TaCZ 后的火药 | 192 | 58／514÷9 |
| 标准旅行者背包 | 不可交易 | 384／384 |
| 钻石等级升级 | 不可交易 | 65616／65616 |

有配方的枪械、配件按材料成本定价，额外枪包型号使用基础后备价。附加数据不增加价值，购买生成全新默认物品。新增枪包、定制配方、机器和村民交易循环仍需服主用数据包调整。


## 26.1.2

26.1.2 使用对应客户端（1,506 个原版 ID）、TaCZ 26.1.2 R3-hotfix 资源事实与旅行者背包 26.1.2-11.2.10。下载地址及 SHA-256 固定于 `versions/test-mods.json`。将 TaCZ JAR 中的 `data/`、`assets/` 解压到临时 `src/main/resources` 目录，作为生成命令的 TaCZ 源目录；执行上述三个生成命令时均设置 `EE_RESOURCE_ROOT=versions/26.1.2/resources`。生成后删除与公共资源相同的覆盖文件，缺省使用公共资源。不分发上游源码或素材；26.2 定价保持原样。
