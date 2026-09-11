# 第三方声明

[English](THIRD_PARTY_NOTICES.md)

能量交换自身代码与原创转化桌素材使用 MIT 许可证。离线读音索引 `energyexchange/pinyin.tsv` 派生自 Unicode 17.0.0 Unihan 数据，采用 **Unicode-3.0** 许可证（Copyright © 1991-2026 Unicode, Inc.）。许可证原文位于 `licenses/Unicode-3.0.txt`，并随运行 JAR 分发至 `META-INF/licenses/Unicode-3.0.txt`。

来源：https://www.unicode.org/Public/17.0.0/ucd/Unihan.zip

`tools/generate_pinyin.py` 校验压缩包 SHA-256，可重复提取普通话读音、去声调并合并简繁体变体。该索引是经过修改的子集，不是完整 Unihan 数据库。使用 kMandarin、kHanyuPinyin、kXHC1983、kHanyuPinlu，以及 kSimplifiedVariant、kTraditionalVariant 变体关系。

不捆绑额外拼音运行库，也不包含 ProjectE 或 TaCZ 代码与贴图。
