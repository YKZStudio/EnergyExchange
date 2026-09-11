# Third-party notices

[简体中文](THIRD_PARTY_NOTICES_zh-CN.md)

Energy Exchange code and original table artwork are MIT licensed. The offline pronunciation index `energyexchange/pinyin.tsv` is derived from Unicode 17.0.0 Unihan data, under **Unicode-3.0** (Copyright © 1991-2026 Unicode, Inc.). Its license is included at `licenses/Unicode-3.0.txt` and `META-INF/licenses/Unicode-3.0.txt` inside the runtime JAR.

Source: https://www.unicode.org/Public/17.0.0/ucd/Unihan.zip

`tools/generate_pinyin.py` verifies the archive SHA-256 and reproducibly extracts Mandarin readings, removes tones, and combines simplified/traditional variants. This is a modified subset, not the full Unihan database. It uses kMandarin, kHanyuPinyin, kXHC1983 and kHanyuPinlu; variant links come from kSimplifiedVariant and kTraditionalVariant.

No separate pinyin runtime libraries, ProjectE code/textures or TaCZ code/textures are bundled.
