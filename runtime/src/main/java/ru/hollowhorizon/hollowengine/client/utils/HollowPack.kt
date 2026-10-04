package ru.hollowhorizon.hollowengine.client.utils


import com.google.gson.JsonObject
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.PackLocationInfo
import net.minecraft.server.packs.PackResources
import net.minecraft.server.packs.PackSelectionConfig
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.metadata.MetadataSectionType
import net.minecraft.server.packs.repository.Pack
import net.minecraft.server.packs.repository.PackSource
import net.minecraft.server.packs.resources.IoSupplier
import ru.hollowhorizon.hollowengine.api.AutoModelType
import ru.hollowhorizon.hollowengine.common.data.PackMetadata
import ru.hollowhorizon.hollowengine.common.utils.literal
import ru.hollowhorizon.hollowengine.common.utils.rl
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.util.*

object HollowPack : PackResources {
    private val resourceMap = HashMap<Identifier, IoSupplier<InputStream>?>()

    init {
        close() // Uses as reload
    }

    private fun ofText(text: String) = IoSupplier<InputStream> { ByteArrayInputStream(text.toByteArray()) }
    fun generatePostShader(location: Identifier) {
        addCustomJSON(
            "${location.namespace}:shaders/post/${location.path}.json".rl,
            "{\"targets\": [\"swap\"],\"passes\": [{\"name\": \"$location\",\"intarget\": \"minecraft:main\",\"outtarget\": \"swap\",\"uniforms\": []},{\"name\": \"$location\",\"intarget\": \"swap\",\"outtarget\": \"minecraft:main\",\"uniforms\": []}]}"
        )

        addCustomJSON(
            "${location.namespace}:shaders/program/${location.path}.json".rl,
            "{\"blend\":{\"func\":\"add\",\"srcrgb\":\"one\",\"dstrgb\":\"zero\"},\"vertex\":\"sobel\",\"fragment\":\"$location\",\"attributes\":[\"Position\"],\"samplers\":[{\"name\":\"DiffuseSampler\"}],\"uniforms\":[{\"name\":\"ProjMat\",\"type\":\"matrix4x4\",\"count\":16,\"values\":[1.0,0.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,0.0,1.0]},{\"name\":\"InSize\",\"type\":\"float\",\"count\":2,\"values\":[1.0,1.0]},{\"name\":\"OutSize\",\"type\":\"float\",\"count\":2,\"values\":[1.0,1.0]},{\"name\":\"Time\",\"type\":\"float\",\"count\":1,\"values\":[0.0]}]}"
        )
    }

    fun addItemModel(location: Identifier, type: AutoModelType) = addCustomItemModel(
        location,
        if (type.blockStateId() == "default")
            "{\"parent\":\"${type.modelId()}\",\"textures\":{\"layer0\":\"" + location.namespace + ":item/" + location.path + "\"}}"
        else type.modelId()
    )

    fun addParticleModel(location: Identifier) {
        val particle = "${location.namespace}:particles/${location.path}.json".rl
        addCustomJSON(particle, "{\"textures\":[\"$location\"]}")
    }

    fun addBlockModel(location: Identifier, type: AutoModelType) {
        when (type.blockStateId()) {
            "default" -> addCustomBlockstate(
                location,
                "{\"variants\":{\"\":{\"model\":\"" + location.namespace + ":block/" + location.path + "\"}}}"
            )

            "directional" -> addCustomBlockstate(
                location, """
                {"variants":{
                    "facing=east":{"model":"${location.namespace}:block/${location.path}","y":90},
                    "facing=north":{"model":"${location.namespace}:block/${location.path}","y":0},
                    "facing=south":{"model":"${location.namespace}:block/${location.path}","y":180},
                    "facing=west":{"model":"${location.namespace}:block/${location.path}","y":270}
                }}
            """.trimIndent()
            )
        }
        addCustomBlock(
            location,
            "{\"parent\":\"${type.modelId()}\",\"textures\":{\"all\":\"" + location.namespace + ":block/" + location.path + "\"}}"
        )
    }

    fun addSoundJson(modid: String, sound: JsonObject) {
        addCustomJSON("$modid:sounds.json".rl, sound.toString())
    }

    fun addCustomJSON(modelPath: Identifier, content: String) {
        resourceMap[modelPath] = ofText(content)
    }

    fun addCustomItemModel(location: Identifier, content: String) {
        val model = "${location.namespace}:models/item/${location.path}.json".rl
        addCustomJSON(model, content)
        // since 26.1 an item is looked up by its client item definition, which points at the model
        addCustomJSON(
            "${location.namespace}:items/${location.path}.json".rl,
            "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"${location.namespace}:item/${location.path}\"}}"
        )
    }

    fun addCustomBlockstate(location: Identifier, content: String) {
        val blockstate = "${location.namespace}:blockstates/${location.path}.json".rl
        addCustomJSON(blockstate, content)
    }

    fun addCustomBlock(location: Identifier, content: String) {
        val model = "${location.namespace}:models/block/${location.path}.json".rl
        addCustomJSON(model, content)
    }

    override fun getRootResource(vararg fileName: String): IoSupplier<InputStream>? {
        return null
    }


    @Throws(IOException::class)
    override fun getResource(type: PackType, pLocation: Identifier): IoSupplier<InputStream>? {
        return resourceMap[pLocation]
    }

    override fun listResources(
        packType: PackType,
        namespace: String,
        prefix: String,
        output: PackResources.ResourceOutput,
    ) {
        resourceMap.forEach { (location, resource) ->
            if (resource != null && location.namespace == namespace && location.path.startsWith(prefix)) output.accept(location, resource)
        }
    }

    override fun getNamespaces(pType: PackType) = resourceMap.keys.map { it.namespace }.toSet()

    override fun <T : Any> getMetadataSection(type: MetadataSectionType<T>): T? =
        PackMetadata.section(type, "Generated resources for HollowCore")

    override fun location(): PackLocationInfo {
        return PackLocationInfo(packId(), packId().literal, PackSource.BUILT_IN, Optional.empty())
    }

    override fun packId() = "HollowEngine Embed Resources"
    override fun close() {}

    val resources = asPack()
}

fun PackResources.asPack() =
    Pack.readMetaAndCreate(
        location(), object : Pack.ResourcesSupplier {
            override fun openPrimary(location: PackLocationInfo): PackResources {
                return this@asPack
            }

            override fun openFull(
                location: PackLocationInfo,
                metadata: Pack.Metadata,
            ): PackResources {
                return this@asPack
            }

        }, PackType.CLIENT_RESOURCES, PackSelectionConfig(true, Pack.Position.TOP, true)
    )

