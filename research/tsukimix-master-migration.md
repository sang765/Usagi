# TsukiMix master migration notes

## Verified upstream revision

- Repository: https://github.com/UsagiApp/TsukiMix
- Current Usagi pin before migration: `77d6e12cbd54654b4d4aef5c7891c003c8a1be01`
- TsukiMix master checked on 2026-08-27: `93a458e66de4e4424f32f3069961776a4cdf7bb8`
- Commit subject: `Refactor code, reorganize package / class`

## Verified API rename map

| Pinned API | TsukiMix master API |
|---|---|
| `TachiyomiInjektBridge` | `ExtensionBridge` |
| `TachiyomiExtensionLoader` | `ExtensionLoader` |
| `TachiyomiExtensionManager` | `ExtensionManager` |
| `DirectTachiyomiExtensionManager` | `NativeExtManager` |
| `TachiyomiRuntime` (library runtime) | `ExtRuntime` |
| `TachiyomiMangaSource` | `external.model.Manga` |
| `TachiyomiLoadResult` | `external.model.MangaResult` |
| `TachiyomiSourceSettings` | `ExtensionSourceSettings` |
| `TachiyomiExtensionCatalogProvider` | `ExtensionProvider` |
| `TachiyomiExtensionArtifact` | `external.model.ExtArtifact` |
| `TachiyomiCatalogSource` | `external.model.ExtSource` |
| `DirectTachiyomiInstalled` | `external.model.ExtInstalled` |
| `TachiyomiInjektBridge` | `ExtensionBridge` |
| old `model.to*` converters | `external.model.MangaConverter` |

TsukiMix master moves the implementation from `core.parser.tachiyomi` to `core.parser.external`, adds `ExtRuntime`, and introduces `NativeExtManager`/`ExtensionProvider` responsibilities. `ExtArtifact` uses `ExtSource` entries with `language` and `homeUrl`; the old catalog repository's manually parsed legacy data can map to these fields. The new `ExtensionProvider` has `loadSavedCached()` but no old `loadCached(url)` method, so Usagi's catalog repository must use its own cache lookup or adapt that call rather than assume a direct rename.

The new `ExtensionManager` still exposes `installedExtensions`, `failedExtensions`, `isLoading`, `isReady`, and `sources`, while `NativeExtManager` still exposes `sources`, `installed`, `failed`, `ensureReady`, `install`, `remove`, `getActiveSources`, `owns`, and source lookup. The new `ExtensionBridge`, `ExtensionLoader`, and `NativeExtManager` retain application-context usage and the existing injected network/evaluator responsibilities.

## Migration caution

The Usagi branch must update the TsukiMix dependency to the master commit and migrate all old imports/call sites. The current Usagi class `org.draken.usagi.core.TachiyomiRuntime` is an application facade and should remain; it should wrap the new TsukiMix `ExtensionManager` and `NativeExtManager`, not be replaced by the library's `ExtRuntime` until all source publishing semantics are reviewed.
