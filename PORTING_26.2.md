# HollowEngine on Minecraft 26.2

This branch (`port/26.2`) is HollowEngine 2.4.0 ported from Minecraft 1.21.1 to 26.2, for Fabric and NeoForge.
The fork's `dev` branch is kept identical to the original developer's default branch; everything specific to 26.2 lives here.

## Versions

| | |
|---|---|
| Minecraft | 26.2 (unobfuscated, Mojang names) |
| Java | 25 |
| Kotlin / Gradle | 2.4.0 / 9.5.1 |
| Fabric Loader / API | 0.19.5 / 0.161.0+26.2 |
| NeoForge | 26.2.0.88 |
| Loom | `dev.architectury.loom-no-remap` 1.17 |
| Dev-only | Sodium 0.9.2, Iris 1.11.4, JEI 30.39 |

## Taking new upstream commits

```sh
git remote add upstream https://github.com/BMProjects-Development/HollowEngine.git
git fetch upstream --tags          # tags cannot be pushed to the fork through the proxy, fetch them from upstream
git checkout port/26.2
git merge upstream/<default branch>   # or a release tag such as v2.4.1
```

Upstream code is written against 1.21.1, so after a merge the build will mostly complain about the same few things.
Fix them in the merge commit:

- `ResourceLocation` is now `Identifier`.
- Direct OpenGL drawing: use `ru.hollowhorizon.hollowengine.client.render.legacy.*` (see below) instead of `RenderSystem`, `GlStateManager`, `ShaderInstance`, `RenderType` and `MultiBufferSource` from vanilla.
- Calls that moved or changed in 26.x are wrapped in `common/utils/compat/Minecraft26.kt`; add an extension there and import it rather than editing call sites twice.
- NBT getters return `Optional`: use `getIntOr`, `getFloatOr` and friends.
- GUI code draws through `GuiGraphicsExtractor`; `Screen.render` became `extractRenderState`, key and mouse events are event objects (`KeyEvent`, ...).
- Items need a client item definition (`assets/<ns>/items/<id>.json`); `HollowPack.addCustomItemModel` writes it for generated items.
- Item components are bound when data loads, not at registry freeze. Unit tests that touch stacks call `MinecraftTestBootstrap`.
- NeoForge clientbound payload handlers have to be given at registration.

## How rendering works here

26.x renders in three steps (extract, submit, frame graph) and has no immediate-mode GL. The engine still draws with its own OpenGL, so
`client/render/legacy/` is a small compatibility layer that keeps the old calls working:

- `RenderSystem` and `GlStateManager` are shims over the current GL state; render calls made before the first frame are queued and replayed at the start of the next frame.
- `Shader`, `Vertex`, `Targets`, `Uploads` and `RawGlTexture` cover programs, vertex building, framebuffers and texture upload.
- `GuiDeferred` runs engine GL UI after vanilla's GUI has been extracted, and has a second pass for vanilla items and tooltips drawn inside engine UI.
- `RenderTypes.kt` records what the engine writes and hands it to vanilla's submit collector for entity geometry.

Because of this the engine **forces the OpenGL backend**; the new Vulkan backend is not supported.

## Packaging and the script compiler

Packaging is unchanged: one universal jar for both loaders (`universalJar`, collected into `merged/` by `buildAndCollect`) and separate addon jars (`buildAddons`, in `build/addon-jars/`).
The bootstrap still embeds the isolated runtime and loads it through a child-first class loader.

Since the game now ships with its own names there is nothing to remap:

- `addons/compiler/src/main/resources/mappings-26.2.tiny` is an empty tiny file (header only), and `remapJars` returns its inputs untouched when the mappings are empty.
- `CommonEnvironment.loadMappings` reads whichever `mappings-*.tiny` the compiler addon carries.
- On NeoForge the script classpath also gets the `neoforge` jar, because the `I...Extension` interfaces of game classes live there.

## Changes visible to script authors

- Items and blocks must carry their registry id when they are built. In startup scripts `register("ruby") { Item(itemProperties(it)) }` replaces `Item(Item.Properties())`.
- `block("id") { props -> Block(props.strength(5f, 6f)) }` receives properties that already carry the id, and item properties are tuned with `configureItem = { it.fireResistant() }` instead of `itemProperties = ...`.
- Everything else scripts touch is plain 26.2 API: `Item.use` returns `InteractionResult`, NBT getters return `Optional`, and so on.

## Checked

- Whole project compiles: runtime, bridge, both bootstraps, compiler, mcp, physics and video addons.
- Unit tests: 1138 of 1139 pass. The one failure is a Windows-path clipboard test that cannot pass on Linux.
- Fabric and NeoForge dedicated servers start from the packaged universal jar with the compiler and mcp addons, and a `.node.kts` script compiles and runs on both.
- Fabric client (singleplayer and multiplayer) and NeoForge client start in a dev environment; the in-game IDE opens; glTF models render on NPCs and the player; generated items work.

## Known gaps

- Not exercised yet: VFX and particles, video playback and physics natives on a client, `.mixin.kts` scripts, NeoForge client in-world behaviour, the packaged jar on a client.
- The vanilla model preview in the IDE is a stub.
- The Iris integration is stubbed (Iris 1.11 changed its API); shader packs are not coordinated with the engine's rendering.
- Vanilla items and tooltips inside engine UI are drawn in a second GUI pass, so their z-order against engine GL UI is approximate.
- Fractional GUI scale is rounded to an integer.
- The "fire tick" world rule no longer exists in 26.x; the IDE world menu maps it to the fire spread radius around players (0 means off), which is close but not identical.
- Docs outside `scripting/startup` still show 1.21 APIs in their Kotlin examples.
- On NeoForge servers netty logs a harmless-looking `kqueue` class initialisation error to the debug log at startup.
