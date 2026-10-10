# HollowEngine on Minecraft 26.2

This branch (`port/26.2`) is HollowEngine ported from Minecraft 1.21.1 to 26.2, for Fabric and NeoForge.
It started from the 2.4.0 release and now follows the original developer's `v2.5` branch (the work towards 2.5, 31 commits after 2.4.0 when it was merged on 2026-10-10).
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
git merge upstream/v2.5            # the branch this port follows; or a release tag such as v2.5.0
```

Do not merge the default branch by accident: upstream's `dev` can be ahead of `v2.5` (on 2026-10-10 it had one commit more, `48fa57a2` "Fix runtime hooks and NPC lifecycle", which is not in this port).
Merge with a merge commit, never rebase, so the next upstream merge finds its common ancestor.
Jars are named after `modVersion` in `gradle.properties` (upstream still says 2.4.0 on `v2.5`); `-PmodVersion=2.5.0-dev` on the Gradle command line gives distinctly named jars without touching the file.

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

What the `v2.5` merge needed on top of that (all in the commits after the merge commit):

- Entities that save through `ValueInput`/`ValueOutput` (world objects) use the vector and quaternion codecs; there is no UUID serializer any more, so the parent of a world object travels as text. `lerpTo` is `InterpolationHandler`, `hurt` is `hurtServer`, favourites are a `SavedDataType`.
- Vanilla's `Path` is final, so a jump step is marked on its `Node` (`JumpNode`) instead of a `Path` subclass. `Mth.sin` and `Mth.cos` take doubles, `LevelHeightAccessor.getMinBuildHeight` is `getMinY`.
- `ServerPlayer.level()`, `hurtOrSimulate` and `snapTo` replace the old colliders' calls; `EntityBodyMixin` wraps `canBeCollidedWith(Entity)` as 26.2 declares it.
- An arrow collects every entity on its way at once in `ProjectileUtil.getManyEntityHitResult`, so it never asked the nearest-hit variants the colliders wrap. `ProjectileUtilMixin` wraps it too: entities with collider targets are hidden from vanilla (their box is not their shape) and the nearest collider hit of each is added, the way spears already worked.
- The `GameTime` uniform of engine shaders follows the level's game time (it stayed 0 before); post effects rebuild the world position with `hollowengine_ndc_depth`, which knows the reversed depth of the level.

## How rendering works here

26.x renders in three steps (extract, submit, frame graph) and has no immediate-mode GL. The engine still draws with its own OpenGL, so
`client/render/legacy/` is a small compatibility layer that keeps the old calls working:

- `RenderSystem` and `GlStateManager` are shims over the current GL state; render calls made before the first frame are queued and replayed at the start of the next frame.
- `Shader`, `Vertex`, `Targets`, `Uploads` and `RawGlTexture` cover programs, vertex building, framebuffers and texture upload.
- `GuiDeferred` runs engine GL UI after vanilla's GUI has been extracted, and has a second pass for vanilla items and tooltips drawn inside engine UI.
- `RenderTypes.kt` records what the engine writes and hands it to vanilla's submit collector for entity geometry. `ImmediateBufferSource` is the other buffer source, for drawing at once inside a frame the engine draws by hand (the UI, VFX stages): plain colored types (debug lines and solid shapes) go through the plain program, entity types (the batched path of a model) through the glTF entity program.
- The depth flag of `RenderType.lines(name, depthTest)` and `RenderType.triangles(name, depthTest)` is honoured by the engine's own GL path only. Vanilla's `lines()` and `debugQuads()` are always depth-tested, so what `RecordingBufferSource` replays into the world (the F3+B skeleton overlay) is hidden by the mesh in front of it.

Because of this the engine **forces the OpenGL backend**; the new Vulkan backend is not supported.

### Drawing into the level

Hooks that draw into the level (VFX, particles, gizmos, debug renderers) get the same `RenderLevelStageEvent` as in 1.21, fed from Fabric's `LevelRenderEvents` by `LevelStageDispatcher`.
`RuntimeBridgeEntrypoint.onRenderLevelStage` makes the event behave like it did before:

- The camera rotation is the model view matrix of the legacy `RenderSystem`, and `event.poseStack` starts out empty, as in 1.21. Putting the rotation in both places turns everything twice, which only shows once the camera is not at the effect.
- The level is drawn with a **reversed depth in the zero-to-one range** (near and far swapped, depth cleared to 0, `GEQUAL`). `RenderSystem.reverseDepth` is on inside the hook, so `RenderSystem.nearerDepthFunc` and `RenderSystem.farDepth` give the right function and value; use them instead of `LEQUAL` and `1f`. Shaders that read depth convert it with `hollowengine_view_depth` from `hollowengine_vfx.glsl`, which knows both conventions.
- Vanilla binds a framebuffer only while one of its render passes runs, and a pass can leave its scissor on. The hook binds the engine's wrapper of the main target and switches scissor off while the event runs, then puts both back.
- Vanilla leaves sampler objects bound on the texture units, and their filters override those of the engine's own textures (a non-mipmapped texture behind a mipmapping sampler samples as zeros). `RenderSystem.bindTexture`, `GlStateManager._bindTexture` and `LegacyGl.bindTexture` unbind the sampler of the unit they bind to; raw `glBindTexture` calls do not, so the UI's glyph atlases (`UiAnalyticRectRenderer`) unbind it themselves.
- `Uniform.setSafe` writes only as many components as the uniform has, like the 1.21 `Uniform`, so engine shaders can keep passing four values.

### Depth in the UI

Vanilla creates its context with `glClipControl(GL_LOWER_LEFT, GL_ZERO_TO_ONE)`, so clip space z runs from 0 to w, not from -w to w.
The engine's UI projections are therefore built with JOML's `zZeroToOne` flag (`MinecraftUiRenderer.configureLayerProjection`, `GuiDeferred.flush`): `setOrtho(..., -1000f, 1000f, true)` puts `z = 0` at depth 0.5. With the old range everything in front of `z = 0` is clipped, which is invisible for flat UI and wrong for anything 3D in a screen: the front of a model preview and half of each bone went missing. Any new orthographic matrix for UI drawing must pass `true`.
Vanilla also leaves `glClearDepth(0.0)` set (the level is reversed depth), so `MinecraftUiRenderer.clearDepthOf` clears to 1.0 and puts the previous value back; a canvas that draws with `LEQUAL` over a buffer cleared to 0 sees nothing pass.
A samples-passed query around a draw (`glBeginQuery(GL_SAMPLES_PASSED)`) is the quickest way to tell "clipped or culled" from "drawn but invisible": the model preview showed 0 samples before the fix and about 90,000 after it.

### The main target

Vanilla draws the frame into one render target, and `MainTarget.get()` hands out a wrapper of it: a framebuffer of the engine's own with vanilla's colour and depth textures attached (vanilla's framebuffers live in a cache that is not for use). Vanilla creates those textures anew whenever the frame changes size: a window resize, fullscreen, or a mod that resizes the game view (Axiom's editor does, several times per toggle). GL may hand the freed names to the new textures, and on Mesa the colour and depth names swap the first time and come back unchanged the next. Attaching by name does nothing then, and the wrapper used to keep drawing into the old, orphaned textures: engine screens and level hooks came out empty, with no GL error and the samples-passed counters still counting. The wrapper now follows the texture objects and builds a new framebuffer for every new pair.

To see the old failure on Mesa (revert `Targets.kt`): open a HollowEngine screen, close it, resize the window twice in a row, open it again. With Axiom installed, toggling its editor on and off once is enough.

### The UI on a 3.3 context

Vanilla asks for a 3.3 core context, and some drivers (an AMD card on Windows, for one) give exactly that. There is no compute shader there, so the UI's tiled path renderer does its coarse pass on the CPU (the log says `CPU coarse pass`; `GPU compute` is what a 4.3+ context logs). To see that path on Mesa, start the client with `MESA_GL_VERSION_OVERRIDE=3.3 MESA_GLSL_VERSION_OVERRIDE=330`; `4.0` and `400` give a context that still has no compute but is new enough for shader packs that need GLSL 400.

`GuiDeferred.flush` runs the engine's UI inside `LegacyGl.scope`, switches off the states other mods leave on that vanilla never looks at (stencil test, sRGB framebuffer, colour logic op) and puts them back. `UiGlDiagnostics` logs GL errors raised during the UI frame, and the ones that were already pending before it, with the state the frame ran in (the first few only). `-Dhollowengine.ui.debug` also logs the state a UI frame runs in, once a second.

`-Dhollowengine.vfx.debug` makes the VFX renderers log what a frame holds, the GL state it is drawn in, GL errors they leave behind and how many fragments each draw let through; it is the quickest way to tell "not drawn" from "drawn but invisible".

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
- Mixin scripts target the class that declares the method: `Player` no longer declares `jumpFromGround` (use `LivingEntity` and check `self is Player`), and `Player.displayClientMessage` is `sendOverlayMessage`. The examples in the mixin docs are updated and compile against 26.2.
- Everything else scripts touch is plain 26.2 API: `Item.use` returns `InteractionResult`, NBT getters return `Optional`, and so on.

## Checked

- Whole project compiles: runtime, bridge, both bootstraps, compiler, mcp, physics and video addons.
- Unit tests: runtime 1236 of 1237 pass (the one failure is a Windows-path clipboard test that cannot pass on Linux), compiler addon 66 of 66 (they read Java 25 class files, so their classpath carries ASM 9.10), mcp addon 7 of 7, physics addon 46 of 46.
- Fabric and NeoForge dedicated servers start from the packaged universal jar with the compiler and mcp addons, and a `.node.kts` script compiles and runs on both.
- Fabric client (singleplayer and multiplayer) and NeoForge client start in a dev environment; the in-game IDE opens; glTF models render on NPCs and the player; generated items work.
- The packaged universal jar with all four addons loads on a Fabric client: the IDE opens, a client script compiles and draws a HUD, `/he model attach` puts a model on an NPC.
- Video addon in the packaged Fabric client: an H.264/AAC file plays full screen with correct colours (the cloud container has no sound card, so audio ran against OpenAL Soft's `null` device: `ALSOFT_DRIVERS=null`; real audio output is not checked).
- Physics addon: its unit tests pass with the Jolt natives on Linux.
- Mixin scripts: a `.mixin.kts` compiles on a Fabric dedicated server from the packaged jar, its injections apply, and a `LivingEntity.jumpFromGround` hook shows its overlay message on a client (the documented examples with `replaceCall`, `beforeCall`, `inject` at a call and `modifyReturnValue` compile and bind too).
- NeoForge dev client joins a NeoForge dedicated server that runs the packaged jar, and the `magic_shield` VFX renders in the world there as well (with Sodium and Iris in the dev environment).
- The IDE's VFX preview (a `.vfx` file opened from the project tree) renders `magic_shield` with its grid and gizmos.
- F3+B hitboxes on the packaged Fabric client: entity boxes, eye line and view vector draw, with an NPC and an engine model in view. Vanilla's line format needs a width on every vertex, so `RecordingBufferSource` records it.
- The UI on a 3.3 core context (Mesa override, so the CPU coarse pass) with Sodium 0.9.0 and Iris 1.11.1 installed, at 1920x1080: the demo UI, the IDE overlay with its game view, and a script HUD draw. With Complementary Reimagined r5.9.3 on a 4.0 context (it needs GLSL 400) the HUD, hitboxes and the demo UI draw as well.
- The engine UI after vanilla recreated the main target (see "The main target"): with Axiom 5.5.0 and fabric-gui-imgui installed the demo UI draws after the Axiom editor was toggled on and off, and without Axiom it draws after two window resizes in a row, with the script HUD and the `explosion` and `magic_shield` VFX drawing as well. Before the fix the UI stayed invisible in both cases (3.3 core context on Mesa, Sodium 0.9.0, Iris 1.11.1).
- VFX in the packaged Fabric client: post effects (`soul`, `explosion` dimming), world surfaces (the `magic_shield` bubble with its ground contact glow, ribbons, particles, smoke and sparks of `explosion`), sky nodes (`sky_strike`), seen from first and third person.

Checked for the `v2.5` merge, from the packaged universal jar:

- Fabric dedicated server: the mixin audit is clean; colliders can be placed, survive a restart, and are hit by an arrow (see above) and by a held item; world objects and their favourites persist; an NPC walks across a trench two blocks wide with its jump steps.
- NeoForge dedicated server: starts with a clean mixin audit; a collider takes an arrow hit (health 10 to 8); the same jump navigation works.
- Fabric client: no mixin errors; F3+B draws the collider outlines; the IDE opens with its History panel, the material graph and the rig editor (bones, colliders, the model's mesh with its texture; the glTF viewer uses the same widget, see "Depth in the UI"); the seven post effects render; the debug renderer of NPC paths draws jump arcs and markers.

## Known gaps

- Not exercised yet: physics in-game (ragdolls on a client), real audio output of the video addon, the packaged jar on a NeoForge client, NPC models and scripts on a NeoForge client. Of what `v2.5` added: animation blending and the IK stepping in game, the nameplate and interaction wraps, and undo beyond the History panel's display.
- The F3+B skeleton overlay is drawn through vanilla's depth-tested debug pipelines, so the mesh hides the inner bones where it is in front of them (see "How rendering works here"). Making it see-through needs a pipeline of the engine's own.
- A report from an AMD RX 480 on Windows (3.3 core context, Sodium, Iris, Axiom with ImGui, JourneyMap) says the UI sometimes does not draw or stops responding after the two `Initialized UI ...` log lines. One cause was found on Linux and fixed: after Axiom's editor was toggled or the window was resized twice, the engine drew into the old textures of the main target (see "The main target"). That fits "does not display", but it is not confirmed on that machine, and a real freeze was never reproduced. The AMD driver itself cannot be tested here. Hardened on the way: sampler objects on the glyph atlas units, stray GL switches, GL error logging. If it still happens, `latest.log` with `-Dhollowengine.ui.debug` shows the GL state and errors of the UI frame.
- The vanilla model preview in the IDE is a stub.
- The Iris integration is stubbed (Iris 1.11 changed its API); shader packs are not coordinated with the engine's rendering.
- Vanilla items and tooltips inside engine UI are drawn in a second GUI pass, so their z-order against engine GL UI is approximate.
- Fractional GUI scale is rounded to an integer.
- The "fire tick" world rule no longer exists in 26.x; the IDE world menu maps it to the fire spread radius around players (0 means off), which is close but not identical.
- Docs outside `scripting/startup` and `scripting/mixins` still show 1.21 APIs in their Kotlin examples.
- On NeoForge servers netty logs a harmless-looking `kqueue` class initialisation error to the debug log at startup.
