import com.bedrockk.molang.runtime.struct.VariableStruct
import com.bedrockk.molang.runtime.value.MoValue
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.scheduling.ScheduledTask
import com.cobblemon.mod.common.api.scheduling.ServerTaskTracker
import com.cobblemon.mod.common.api.scheduling.afterOnServer
import com.cobblemon.mod.common.api.scheduling.taskBuilder
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.util.server
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import com.mojang.authlib.properties.PropertyMap
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.component.ComponentChanges
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.LoreComponent
import net.minecraft.component.type.NbtComponent
import net.minecraft.component.type.ProfileComponent
import net.minecraft.entity.boss.BossBar
import net.minecraft.entity.boss.ServerBossBar
import net.minecraft.item.Item
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.nbt.StringNbtReader
import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.text.NumberFormat
import java.util.ArrayList
import java.util.Locale
import java.util.Optional
import java.util.UUID
import kotlin.collections.plus
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.KCallable

// logs a value
fun debug(any: Any) {
    Cobalt.Companion.LOGGER.info(any)
}
fun ServerPlayerEntity.increment(variables: List<String>){
    val data = loadMolangData()
    for (variable in variables){
        val value = data.map[variable]
        val number = if (value == null){
            MoValue.of(1)
        }
        else{
            MoValue.of(value.asInt() + 1)
        }
        data.setDirectly(variable, number)
    }
    saveMolangData()
}

fun MoValue?.increment(): MoValue{
    return if (this == null){
        MoValue.of(1)
    }
    else {
        MoValue.of(this.asInt() + 1)
    }
}
// logs a value (alias for debug())
fun println(any: Any) {
    debug(any)
}

// logs a value (alias for debug())
fun print(any: Any) {
    debug(any)
}

// logs a value (alias for debug())
fun log(any: Any) {
    debug(any)
}

// lists the members of a class (useful for debugging/finding methods)
fun debugMembers(any: Any): Collection<KCallable<*>> {
    return any::class.members
}

fun getAllPlayers(): List<ServerPlayerEntity?> {
    return server()!!.playerManager.playerList
}

// registers an admin sub-command /cobalt run <name>
fun onCallCommand(name: String, function: () -> Unit) {
    val wrapper: (ServerPlayerEntity?) -> Unit = { function.invoke() }
    CobaltCommand.cobaltCommands[name] = wrapper
}

// registers an admin sub-command /cobalt run <name> <player>
fun onCallPlayerCommand(name: String, function: (ServerPlayerEntity) -> Unit) {
    val wrapper: (ServerPlayerEntity?) -> Unit = { function.invoke(it!!) }
    CobaltCommand.cobaltCommands[name] = wrapper
}

// registers an event listener that fires when a player joins the server, (player)
fun onPlayerJoin(function: (ServerPlayerEntity) -> Unit) {
    CobaltReloadableCallbacks.add("playerJoin",function)
}

// registers an event listener that fires when a player joins the server, (player)
fun postStartup(function: () -> Unit) {
    CobaltReloadableCallbacks.add("postStartup",function)
}

// registers an event listener that fires after a player changes dimensions (player, origin, destination)
fun afterPlayerChangeWorld(function: (ServerPlayerEntity, ServerWorld, ServerWorld) -> Unit) {
    CobaltReloadableCallbacks.add("afterPlayerChangeWorld",function)
}

// registers an event listener that fires before a player breaks a block (world, player, position, state, blockEntity), return false to cancel
fun beforePlayerBreaksBlock(function: (World, ServerPlayerEntity, BlockPos, BlockState, BlockEntity?) -> Boolean) {
    CobaltReloadableCallbacks.add("beforePlayerBreakBlock",function)
}

// registers an event listener that fires before a player breaks a block (context, state), return false to cancel
fun beforePlayerPlacesBlock(function: (ItemPlacementContext, BlockState) -> Boolean) {
    CobaltReloadableCallbacks.add("beforePlayerPlacesBlock",function)
}

// registers an event listener that fires before a player interacts with a block (player, context), return false to cancel
// context fields:
// state, hand, heldItem, blockHitResult, blockPos, direction
fun beforePlayerUsesBlock(function: (ServerPlayerEntity, BlockInteractionContext) -> Boolean) {
    CobaltReloadableCallbacks.add("beforePlayerUsesBlock",function)
}

// registers an event listener that fires when a command is sent (context)
// context fields:
// message, player, params
fun onCommandExecution(function: (ExecuteCommandContext) -> Boolean) {
    CobaltReloadableCallbacks.add("executeCommand",function)
}

// runs a command as the player ("/" not included)
fun ServerPlayerEntity.runCommand(command: String) {
    server!!.commandManager.executeWithPrefix(commandSource, command)
}

// runs a command silently as the player ("/" not included)
fun ServerPlayerEntity.runCommandSilent(command: String) {
    server!!.commandManager.executeWithPrefix(commandSource.withSilent(), command)
}

// runs a command via console and with %player% replaced with the players name ("/" not included)
fun ServerPlayerEntity.runConsoleCommand(command: String) {
    server!!.commandManager.executeWithPrefix(server!!.commandSource, command.replace("%player%", name.string))
}

// runs a command via console ("/" not included)
fun runConsoleCommand(command: String) {
    server()!!.commandManager.executeWithPrefix(server()!!.commandSource, command)
}

// runs a command via console and with %player% replaced with the players name ("/" not included)
fun ServerPlayerEntity.runConsoleCommand(command: String, callback: (String) -> Unit) {
    server()!!.commandManager.executeWithPrefix(
        DummyCommandOutput(callback, server()!!).createCommandSource(),
        command.replace("%player%", name.string)
    )
}

// retrieves player name as a string
fun ServerPlayerEntity.name() = name.literalString!!

// retrieves world id as a string (e.g. "minecraft:overworld")
fun World.name() = this.registryKey.value.toString()

// sends the player a message in chat or above hot bar if overlay is set to true
fun ServerPlayerEntity.tell(message: String, overlay: Boolean = false) {
    sendMessage(message.text(), overlay)
}
//
//    // sends the player a message in chat (alias for tell(message, false))
//    fun ServerPlayerEntity.tell(message: String) {
//        sendMessage(message.text(), false)
//    }

// sends the player a message in chat or above hot bar if overlay is set to true
fun ServerPlayerEntity.tell(message: Text, overlay: Boolean = false) {
    sendMessage(message, overlay)
}
//
//    // sends the player a message in chat (alias for tell(message, false))
//    fun ServerPlayerEntity.tell(message: Text) {
//        sendMessage(message, false)
//    }

fun broadcast(message: Text, overlay: Boolean = false){
    for (player in getAllPlayers()){
        player?.tell(message, overlay)
    }
}
fun broadcast(message: String, overlay: Boolean = false){
    for (player in getAllPlayers()){
        player?.tell(message, overlay)
    }
}

fun ServerPlayerEntity.playSound(sound: SoundEvent, category: SoundCategory = SoundCategory.MASTER,  volume: Float = 1.0f,pitch: Float = 1.0f ){
    playSoundToPlayer(sound, category, volume, pitch)
}

fun MoValue.asInt(): Int {
    return asDouble().toInt()
}
fun ServerPlayerEntity.loadMolangData(): VariableStruct {
    return Cobblemon.molangData.load(uuid)
}

fun ServerPlayerEntity.saveMolangData() {
    Cobblemon.molangData.save(uuid)
}
fun String.saveMolangData() {
    Cobblemon.molangData.save(getCachedUserId()?: return)
}
fun String.loadMolangData(): VariableStruct? {
    return Cobblemon.molangData.load(getCachedUserId()?: return null)
}
fun UUID.saveMolangData() {
    Cobblemon.molangData.save(this)
}
fun UUID.loadMolangData(): VariableStruct {
    return Cobblemon.molangData.load(this)
}
fun getBlock(id: String): Block {
    return Registries.BLOCK.get(Identifier.of(id))
}

fun getItem(id: String): Item {
    return Registries.ITEM.get(Identifier.of(id))
}

fun getSoundEvent(id: String): SoundEvent? {
    return Registries.SOUND_EVENT.get(Identifier.of(id))
}

fun Item.asItemStack(): ItemStack {
    return ItemStack(this)
}

fun ItemStack.withName(name: String): ItemStack {
    return withName(name.text())
}

fun ItemStack.withName(name: Text): ItemStack {
    applyChanges(ComponentChanges.builder().add(DataComponentTypes.CUSTOM_NAME, name).build())
    return this
}
fun ItemStack.enchantGlow(): ItemStack{
    val itemStack = this
    itemStack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
    return itemStack
}

fun formatNumberWithCommas(number: Number): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return formatter.format(number)
}
fun ItemStack.withLore(lore: String): ItemStack {
    withLore(*lore.split("\n").map { it.text() }.toTypedArray())
    return this
}
fun String.getCachedUser(): GameProfile?{
    return server()?.userCache?.findByName(this)?.getOrNull()
}
fun String.getCachedUserId(): UUID?{
    return getCachedUser()?.id
}
fun ItemStack.withLore(vararg text: Text): ItemStack{
    set(DataComponentTypes.LORE, LoreComponent(text.toList()))
    return this
}
fun ItemStack.appendLore(vararg text: Text): ItemStack{
    val lines = (get(DataComponentTypes.LORE)?: LoreComponent(listOf())).lines
    set(DataComponentTypes.LORE, LoreComponent(lines + text))
    return this
}
fun ItemStack.prependLore(vararg text: Text): ItemStack{
    val lines = (get(DataComponentTypes.LORE)?: LoreComponent(listOf())).lines
    set(DataComponentTypes.LORE, LoreComponent(text.toList() + lines))
    return this
}
fun ItemStack.withoutTooltip() : ItemStack{
    set(DataComponentTypes.HIDE_TOOLTIP, net.minecraft.util.Unit.INSTANCE)
    return this
}
fun ItemStack.withoutAdditionalTooltip() : ItemStack{
    set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, net.minecraft.util.Unit.INSTANCE)
    return this
}

fun ItemStack.withNBT(nbt: String): ItemStack {
    applyChanges(
        ComponentChanges.builder().add(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(StringNbtReader.parse(nbt)))
            .build()
    )
    return this
}

fun ItemStack.withCount(i: Int): ItemStack {
    count = i
    return this
}

fun ItemStack.withProfileTexture(textures: String): ItemStack {
    val map = PropertyMap()
    map.put("textures", Property("textures", textures))
    applyChanges(
        ComponentChanges.builder().add(
            DataComponentTypes.PROFILE,
            ProfileComponent(Optional.empty<String>(), Optional.empty<UUID>(), map)
        ).build()
    )
    return this
}

fun ServerPlayerEntity.asServerPlayer(): ServerPlayerEntity {
    return this as ServerPlayerEntity
}

fun runAfter(seconds: Float, callback: () -> Unit) {
    afterOnServer(seconds = seconds, callback)
}

fun recurring(seconds: Float, callback: () -> Unit) {
    CobaltReloadableCallbacks.effectiveCobaltEvents.scheduledTaskBuilders.getOrPut(Cobalt.currentScriptName, {ArrayList()}).add(
        taskBuilder().infiniteIterations().delay(seconds).interval(seconds).execute { callback.invoke() }
            .tracker(ServerTaskTracker)
    )
}
//  Possible Boss Bar colors:
//    pink
//    blue
//    red
//    green
//    yellow
//    purple
//    white
//  Possible Boss Bar styles:
//    progress
//    notched_6
//    notched_10
//    notched_12
//    notched_20

//  Useful ServerBossBar methods:
//    bossbar.clearPlayers()
//    bossbar.addPlayer(serverPlayer)
//    bossbar.removePlayer(serverPlayer)
//    bossbar.setName(name)
//    bossbar.setPercent(percent/100f)

fun bossbar(name: String, color: String, style: String): ServerBossBar {
    return ServerBossBar(name.text(), BossBar.Color.byName(color), BossBar.Style.byName(style))
}

fun timer(initialTicks: Int, expiredCallback: () -> Unit): io.github.jeefdevelopment.cobalt.script.Timer {
    return Timer(initialTicks, expiredCallback)
}

fun bossbarTimer(initialTicks: Int, expiredCallback: () -> Unit, bossbar: BossBar): BossBarTimer {
    return BossBarTimer(initialTicks, expiredCallback, bossbar)
}
fun String.minimessageText(): MutableText? {
    val minimessage = MiniMessage.miniMessage().deserialize(this)
    val gson = GsonComponentSerializer.gson().serialize(minimessage)
    return Text.Serialization.fromJson(gson, server()!!.registryManager)
}
fun getCrossScriptVariable(name: String): Any?{
    return Cobalt.crossScriptVariables[name]
}
fun setCrossScriptVariable(name: String, value: Any){
    Cobalt.crossScriptVariables[name] = value
}
fun clearCrossScriptVariable(name: String){
    Cobalt.crossScriptVariables.remove(name)
}
fun loadServerData(): VariableStruct {
    val loadServerData = Cobblemon.molangData.load(UUID(0L, 0L))
    return loadServerData
}

fun saveServerData(){
    Cobblemon.molangData.save(UUID(0L, 0L))
}

open class Timer(var ticks: Int, val expiredCallback: () -> Unit) {

    val server = server()!!

    val startTick: Int = server.ticks
    val task: ScheduledTask = taskBuilder()
        .interval(.1f) // Run every half second
        .execute { task ->
            if (!isRunning()) {
                task.expire()
                expiredCallback.invoke()
                expire()
                return@execute
            }
            tick()
        }
        .tracker(ServerTaskTracker)
        .infiniteIterations()
        .build()

    fun isRunning(): Boolean {
        return ticks > server.ticks - startTick
    }

    fun percentLeft(): Float {
        return (server.ticks - startTick) / ticks.toFloat()
    }

    open fun tick() {
    }

    open fun expire() {

    }
}
