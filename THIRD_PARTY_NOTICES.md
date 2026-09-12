# Third-party notices

[简体中文](THIRD_PARTY_NOTICES_zh-CN.md)

Energy Exchange code and original table artwork are MIT licensed. The offline pronunciation index `energyexchange/pinyin.tsv` is derived from Unicode 17.0.0 Unihan data, under **Unicode-3.0** (Copyright © 1991-2026 Unicode, Inc.). Its license is included at `licenses/Unicode-3.0.txt` and `META-INF/licenses/Unicode-3.0.txt` inside the runtime JAR.

Source: https://www.unicode.org/Public/17.0.0/ucd/Unihan.zip

`tools/generate_pinyin.py` verifies the archive SHA-256 and reproducibly extracts Mandarin readings, removes tones, and combines simplified/traditional variants. This is a modified subset, not the full Unihan database. It uses kMandarin, kHanyuPinyin, kXHC1983 and kHanyuPinlu; variant links come from kSimplifiedVariant and kTraditionalVariant.

No separate pinyin runtime libraries, ProjectE code/textures or TaCZ code/textures are bundled.

## ProjectE and Re-Avaritia assets / 装备素材

Energy Exchange 0.3 uses unchanged item and armor textures, animation metadata, and selected localized item names from:

- ProjectE, commit `f432b0c66837759fb0731c9144dc53176b949c5d`, https://github.com/sinkillerj/ProjectE — MIT, Copyright (c) 2020 Sin Tachikawa. Full license: `licenses/ProjectE-MIT.txt`.
- Re-Avaritia, commit `2d3199c4f23020794ecba5ebc18d73302d771a0a`, https://github.com/Nova-Committee/Re-Avaritia — MIT, Copyright (c) 2022 cnlimiter. Full license: `licenses/Re-Avaritia-MIT.txt`.

The path mapping is in `docs/armory-assets.json`. Original pixels and animation metadata are retained; asset paths and vanilla model/equipment definitions are adapted for Fabric 26.2. Java behavior and 3×3 recipes are independently implemented. No affiliation or endorsement is claimed.

0.3 使用上述 MIT 项目的物品、盔甲贴图、动画元数据及部分已本地化物品名称。保留原始像素和动画数据，按 26.2 调整资源路径与原版模型／装备定义。Java 行为与 3×3 配方独立实现，完整许可与路径来源随项目提供。
