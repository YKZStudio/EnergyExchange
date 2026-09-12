# 数据包规则

[English](DATAPACKS.md) · [首页](../README_zh-CN.md)

## 物品覆盖

每个物品一个文件：`data/<物品命名空间>/energyexchange/values/<物品路径>.json`。

例如 `minecraft:diamond` 对应 `data/minecraft/energyexchange/values/diamond.json`；`othermod:materials/dust` 对应 `data/othermod/energyexchange/values/materials/dust.json`。

```json
{ "value": "8192" }
```

正整数十进制字符串同时指定购买与转化单价。显式物品规则会替换该物品由生成器计算的分数转化收益。禁用物品：

```json
{ "enabled": false }
```

`enabled` 为可选布尔值，默认为 true；启用时必须有 `value`，禁用时必须省略它。数值范围 1 至 `10^128−1`。拒绝 JSON 数字、正负号、前导零、指数、空格、重复键、注释和未知字段。单文件最多 4,096 字符；物品规则和变体规则各最多 16,384 个；完整知识标识最多 256 字符。

## TaCZ 与 LRTactical 型号

知识标识组合基础物品与型号：`tacz:modern_kinetic_gun#tacz:ak47`。

逐型号覆盖路径：

`data/<型号命名空间>/energyexchange/variants/<基础物品命名空间>/<基础物品路径>/<型号路径>.json`

例如 `data/tacz/energyexchange/variants/tacz/modern_kinetic_gun/ak47.json`，内容仍使用 `value` 或 `enabled:false`。型号为 `mypack:rifles/example` 时，路径为 `data/mypack/energyexchange/variants/tacz/modern_kinetic_gun/rifles/example.json`。

此变体格式的基础物品路径为单层路径，与 TaCZ/LRTactical 的注册物品一致；型号路径可以包含子目录。**禁用基础物品会禁用全部型号**，包括有专门价格的型号。否则逐型号规则/默认价格优先；额外枪包已加载型号没有专门价格时，使用基础物品后备价格。只改基础价格不会改写现有型号专价。只有价格文件而没有安装对应模组或加载型号，不会生成可交易物品。

## 默认表与分数收益

内置 `data/energyexchange/energyexchange/defaults.json` 把完整物品/型号标识映射到正整数十进制字符串，共 1,738 条显式价格，其中原版 1,537 个 ID。`salvage.json` 用 `["分子", "分母"]` 指定部分物品的分数转化收益。这两个资源支持数据包按整个文件替换，上限各 2,000,000 字符；通常建议使用小型逐物品覆盖文件。

默认购买单价向上取整；转化使用精确整数计算 `floor(分子 × 数量 / 分母)`，即整批向下取整。收益为零时不消耗物品、不学习。账户余额始终为整数，转化收益不能超过购买价格。替换批量资源时保持标识及上限一致。最高优先级规则无效时锁定交易，不会偷偷退回低优先级数据包。

默认值是生成时参考普通原版配方的初始平衡方案，不是运行时配方求解器。其他数据包修改配方不会自动重算价格。特殊组件配方、酿造、村民交易和其他模组机器仍需单独审查。生成器只读取原版物品/配方及 TaCZ 索引/配方事实，不复制上游代码或素材。

## 覆盖与重载

1. 将完整[示例数据包](../examples/value-overrides)复制到 `<世界>/datapacks/`，根目录应有 `pack.mcmeta`。Minecraft 26.2 的数据包格式为 107.1。
2. 修改圆石价格；示例还会禁用钻石。
3. 管理员执行 `/reload`，必要时用 `/datapack list` 确认启用。
4. 手持物品执行 `/ee value`，或重新打开转化桌查看提示。

Minecraft 数据包优先级决定每个资源位置的最终文件。逐物品/型号资源覆盖批量默认表；字段不合并。删除覆盖文件会恢复默认值，禁止物品应使用 `enabled:false`。

重载期间暂停包括经验购买在内的全部交易；失败持续锁定，直到修正后成功重载。界面拒绝旧规则版本并刷新目录后再允许购买；输入格和已有知识保留。禁用或移除型号不删除知识，恢复型号及规则后可再次交换。新价格只影响今后的交易，不追溯改变既有余额。

## 组件与服务端设置

带数据物品按基础物品价格转化，附魔、磨损、命名、容器内容和其他数据均丢弃。购买时创建默认物品。TaCZ/LRTactical 只保留经过验证的型号身份，使用标准模板；弹药和配件数据会丢弃。没有 `allow_nbt` 或复制组件的开关。

经验开关与单价属于服务端配置，不是数据包规则。编辑 `config/energyexchange.json` 后重启世界/服务端。Mod Menu 修改的是本机下次启动服务端的默认值，不能改变远程服务器经济规则。


0.2.1：带数据物品按基础价值转化并丢弃数据；见[定价与兼容](PRICING_zh-CN.md)。仅正式版创建发行版，pre 版本保留 CI 构建产物。
