# 开发与验证

[English](DEVELOPMENT.md) · [返回首页](../README_zh-CN.md)

## 工具链

Java 25、Gradle Wrapper 9.5.1、Loom 1.17.12、Fabric Loader 0.19.5、Fabric API 0.159.0+26.2。依赖固定在 `gradle.properties`。26.2 使用官方未混淆名称和 `net.fabricmc.fabric-loom`，不配置 Yarn、不使用旧 `modImplementation` 或 `remapJar`。

```sh
./gradlew build
./gradlew runClient
./gradlew runServer
```

开发服务器首次运行需自行阅读并接受 Minecraft EULA。Windows 使用 `gradlew.bat`。网络受限时应在本机设置 Gradle 代理，不把个人代理、令牌或机器路径提交到仓库。

## 结构与扩展约束

| 模块 | 职责 |
| --- | --- |
| `core/Energy` | 十进制解析、乘法和余额上限 |
| `core/Account` | 不可变账户与纯交易逻辑 |
| `core/AccountJson` | 版本化存档；损坏/未来版本拒绝交易且原文保留 |
| `core/ValueRule` | 数据包单项规则解析 |
| `Rules` | 优先级解析后的资源加载、不可变快照、重载锁定 |
| `ExchangeService` | 服务端玩家、组件、数量、容量校验以及实际变更 |
| `ExchangeCommands` / `Messages` | 非 OP 命令入口与客户端本地化文本 |

持久化为玩家 `energyexchange:account` 数据附加项，内部是含 `schema:1`、字符串 `energy`、`learned` ID 数组的 JSON 字符串。字符串 Codec 避免业务解析失败时 Fabric 自动丢弃旧账户。账户是不可变值，更新必须通过 `setAttached`；使用 `copyOnDeath`，不向其他玩家广播账户。

交易顺序：获取服务端当前状态 → 验证规则和组件 → 计算新账户 → 验证可序列化 → 模拟插入 → 在同一服务端线程更新物品和账户 → 同步背包。失败路径不得先扣物品或能量。交易中不异步等待；后续 GUI 必须调用同一服务端服务，并加入请求限速，不能信任客户端报价。

这是服务端内存中的整笔交易，持久化跟随 Minecraft 玩家保存，不是数据库事务日志。第三方模组抛异常或修改库存、服主改存档、断电回滚不提供跨系统事务保证。

## 验证范围

JUnit 覆盖完整纯逻辑闭环、大于浮点精度/long 范围的余额、上限、非法输入、未知知识、余额不足、知识容量、存档往返与损坏、未来版本、规则禁用/错误字段。自动化构建以真实 Minecraft/Fabric 依赖检查 Java 编译并产出 JAR。GameTest（游戏内测试）启动临时服务器，验证命令闭环、玩家隔离、背包预检、溢出、组件拒绝、游戏模式、NBT 存档往返、死亡复制、损坏账户保护和重载快照。`build` 自动包含服务端游戏测试；测试配置为临时服务器接受 EULA，不修改真实世界。

发布前需在 26.2 测试副本中完成以下人工验收；编译或单元测试通过不代表这些场景已人工验证：

- 两位非 OP 玩家分别分解、学习、兑换，账户隔离，无作弊权限可用。
- 完整闭环后死亡重生、跨维度、退服重进、`save-all` 后正常重启，余额和知识保持。
- 背包全满但存在可合堆空间、完全无空间、最大堆叠数量为 1 的物品；失败不扣钱，不落地。
- 命名/损坏/附魔物品、装物品的容器、药水及自定义组件被拒绝；重复学习不消耗。
- 在容器打开、死亡、创造、旁观状态拒绝交易；连续重复命令只能按实际库存/余额结算。
- 两个不同优先级数据包覆盖同一 ID；禁用/移除/恢复规则；非法 JSON、超大数与重载失败锁定及修复后恢复。
- 简体中文、英文与双端界面；Tab 补全与分页。

## 后续方向

0.2 已加入转化桌、搜索界面、私人目录同步、经验购买以及 TaCZ/Mod Menu 适配；后续考虑更多组件变体、实时配方审查和经济日志。不要默认启用任意组件复制、创造模式交易、合成环定价或无限长数字。

参考：[Fabric 26.2 更新说明](https://fabricmc.net/2026/06/15/262.html)、[数据附加项](https://docs.fabricmc.net/develop/serialization/data-attachments)、[自动测试](https://docs.fabricmc.net/develop/automatic-testing)、[官方示例](https://github.com/FabricMC/fabric-example-mod/tree/26.2)。

## 0.2 新增验证

`./gradlew build` 增加便携/放置菜单有效性、关闭返还、经验点、全部原版定价覆盖与分数收益测试。`xvfb-run -a ./gradlew runClientGameTest` 验证真正的服务端目录下发与客户端转化/购买请求，然后打开配置界面；CI 上传截图和日志。

可选兼容测试：将固定的 TaCZ 26.2 R3-hotfix 运行 JAR 下载到 `test-mods/tacz.jar`，执行 `./gradlew runGameTest -PwithTacz`。Gradle 为本次测试解析 Forge Config API Port 26.2.1 与 Cloth Config 26.2.155。适配测试枚举型号类别、检查模板身份和正数价格、买回空枪，并拒绝装弹枪。

仍需人工验收：真实设备上的中文字体与界面缩放、多位真实玩家同时操作、TaCZ 拆装配件及第三方枪包、存档过程异常退出、额外机器/配方的经济平衡。自动测试不替代这些检查。

main 上每个正式版本附带运行 JAR 与对应双语 `docs/releases/<版本>.md`，已有资产不能静默覆盖。

## pre2

新增拼音全拼／首字母／混拼、堆叠限制单元测试和真实客户端伪造购买检查。`tools/generate_table_assets.py` 使用 Pillow 可重复生成原创贴图与模型。所有十个语言区域设置的键和占位符通过一致性检查。
