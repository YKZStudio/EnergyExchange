# 可选模组定价

[English](PRICING.md) · [首页](../README_zh-CN.md)

按顺序运行以下命令重建价格：

```sh
python tools/generate_values.py client.jar /path/to/tacz
python tools/generate_mod_values.py client.jar /path/to/tacz travelersbackpack.jar
```

输入为固定的 26.2 原版客户端、TaCZ 源码提交 `05ec226310545d89cec9169a10105e32d1ed262b` 和旅行者背包 Fabric 26.2-11.3.2。背包 JAR 校验 SHA-256。工具仅读取物品、标签、配方事实，不捆绑上游代码或素材。

普通配方按最低可用材料价格和产出数量计算，自引用复制配方用代数求解。购买单价向上取整，转化保留分数、整批向下取整。每个方案检查全部可解析配方约束。背包条件配方保守采用已定义配方中的最低成本；未列入适配范围的 Comforts 配方跳过。

服务端按安装情况加载 `data/energyexchange/energyexchange/compat/` 下的 `tacz.json`、`travelersbackpack.json` 或 `tacz-travelersbackpack.json`。每个文件包含 `values` 和 `salvage`，边界同默认规则。单物品 `values/` 和型号 `variants/` 数据包随后覆盖，始终优先。该方案在启动／重载时选择，不会实时计算任意第三方机器配方。

| 示例 | 0.2 购买价 | 0.2.1 共同安装时购买价／转化收益 |
| --- | ---: | ---: |
| TaCZ AK-47 | 11584 | 11432／11432 |
| TaCZ 7.62×39 弹药 | 71 | 8／278÷35 |
| 安装 TaCZ 后的火药 | 192 | 58／514÷9 |
| 标准旅行者背包 | 不可交易 | 384／384 |
| 钻石等级升级 | 不可交易 | 65616／65616 |

有配方的枪械、配件按材料成本定价，额外枪包型号使用基础后备价。附加数据不增加价值，购买生成全新默认物品。新增枪包、定制配方、机器和村民交易循环仍需服主用数据包调整。
