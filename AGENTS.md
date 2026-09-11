# Project conventions / 项目约定

- Target Minecraft 26.2 Fabric; verify later versions before widening compatibility. / 目标为 Minecraft 26.2 Fabric，验证后再扩大兼容范围。
- All player-facing text and project documentation must support Simplified Chinese and English. / 玩家文字和项目文档必须包含简体中文与英文。
- Every stable version such as 0.1.0 must have a GitHub Release with the runtime JAR and bilingual changelog. Never replace an existing release's assets silently. / 每个正式大版本（如 0.1.0）必须发布 GitHub 发行版，包含运行 JAR 和双语更新日志；禁止静默替换已有资产。
- Pre-release versions use identifiers such as 0.2.0-pre1. / 预发布版本使用 0.2.0-pre1 等标识。
- Prices, knowledge, inventory, XP and permissions are server-authoritative. Client preferences cannot change multiplayer economy rules. / 定价、知识、库存、经验和权限以服务端为准，客户端偏好不能改变多人服务器经济规则。
- Keep transaction preflight before mutation; reject unsupported component variants rather than silently deleting or copying contents. / 先预检再变更，未支持的组件变体必须拒绝，不能悄悄删除或复制内容。
- Preserve existing saves or migrate them explicitly; test respawn through the full server lifecycle. / 保留现有存档或显式迁移；使用完整服务端流程验证重生。
