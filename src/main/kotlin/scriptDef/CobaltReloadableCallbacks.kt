package io.github.jeefdevelopment.cobalt.event

import com.cobblemon.mod.common.api.scheduling.ScheduledTask
import com.cobblemon.mod.common.util.removeIf
import io.github.jeefdevelopment.cobalt.Cobalt
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.command.CommandSource
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class CobaltReloadableCallbacks {
    data class PlayerReceivedMessageEvent(val serverPlayerEntity: ServerPlayerEntity, val text: Text, val overlay: Boolean, val isSystemMessage: Boolean, val sender: ServerPlayerEntity? = null)
    val reloadableCallbacks = ConcurrentHashMap<String, ConcurrentHashMap<String, CopyOnWriteArrayList<Any>>>()
    val scheduledTaskBuilders = ConcurrentHashMap<String, CopyOnWriteArrayList<ScheduledTask.Builder>>()
    val scheduledTasks = ConcurrentHashMap<String, CopyOnWriteArrayList<ScheduledTask>>()
    // TODO:
//      player kill/damage/interact entity
    fun clearFunctions() {
        for (value in scheduledTasks.values)
            for (task in value)
                task.expire()
        scheduledTasks.clear()
        scheduledTaskBuilders.clear()
        for ((identifier, callbacks) in reloadableCallbacks){
            callbacks.clear()
        }
    }
    fun clearFunctions(name: String) {
        for ((key, value) in scheduledTasks)
            if(key == name)
                for (task in value)
                    task.expire()
        scheduledTasks.removeIf{it.key == name}
        scheduledTaskBuilders.removeIf{it.key == name}
        for ((identifier, callbacks) in reloadableCallbacks){
            callbacks.removeIf{it.key == name}
        }
    }

    companion object {
        var effectiveCobaltEvents = CobaltReloadableCallbacks()
        fun reloadFunctions() {
            for (value in effectiveCobaltEvents.scheduledTaskBuilders.values)
                for(builder in value)
                    effectiveCobaltEvents.scheduledTasks.getOrPut(Cobalt.currentScriptName, { CopyOnWriteArrayList()}).add(builder.build())
        }
        fun reloadFunctions(name: String) {
            for (value in effectiveCobaltEvents.scheduledTaskBuilders.values)
                for(builder in value)
                    effectiveCobaltEvents.scheduledTasks.getOrPut(Cobalt.currentScriptName, { CopyOnWriteArrayList()}).add(builder.build())
        }
        fun registerEvents() {

            CommandRegistrationCallback.EVENT.register { dispatcher, context, environment ->
                CobaltCommand.register(dispatcher,context,environment)
            }
            ServerPlayerEvents.JOIN.register { event ->
                forEach("playerJoin") { function ->
                    function as (ServerPlayerEntity)-> Unit
                    function.invoke(event)
                }
            }
            ServerLifecycleEvents.SERVER_STARTED.register {
                forEach("postStartup") { function ->
                    function as () -> Unit
                    function.invoke()
                }
            }

            ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register { player, origin, destination ->
                forEach("afterPlayerChangeWorld") { function ->
                    function as (ServerPlayerEntity, ServerWorld, ServerWorld )-> Unit
                    function.invoke(player, origin, destination)
                }
            }
            PlayerBlockBreakEvents.BEFORE.register { world, player, pos, state, entity ->
                var returnVal = true
                forEach("beforePlayerBreakBlock") { function ->
                    function as (World, ServerPlayerEntity, BlockPos, BlockState, BlockEntity?) -> Boolean
                    val result: Boolean = function.invoke(world, player as ServerPlayerEntity, pos, state, entity)
                    if (!result) {
                        returnVal = false
                        return@forEach
                    }
                }
                return@register returnVal
            }

        }
        fun add(name: String, value: Any){
            effectiveCobaltEvents.reloadableCallbacks.getOrPut(name, { ConcurrentHashMap()}).getOrPut(Cobalt.currentScriptName, { CopyOnWriteArrayList() }).add(value)
        }
        fun forEach(name: String, function: (Any)->Unit){
            effectiveCobaltEvents.reloadableCallbacks.get(name)?.let {
                for(value in it.values) {
                    for (callback in value) {
                        function.invoke(callback)
                    }
                }
            }
        }
        fun executeCommandEvent(command: String, source: CommandSource): Boolean {
            var returnVal = true
            forEach("executeCommand") {
                it as (ExecuteCommandContext) -> Boolean
                val result: Boolean = it.invoke(ExecuteCommandContext(command, source))
                if (!result) {
                    returnVal = false
                    return@forEach
                }
            }
            return returnVal
        }

        fun playerPlaceBlockEvent(itemPlacementContext: ItemPlacementContext, blockState: BlockState): Boolean {
            var returnVal = true
            forEach("playerPlaceBlock") {
                it as (ItemPlacementContext, BlockState) -> Boolean
                val result: Boolean = it.invoke(itemPlacementContext, blockState)
                if (!result) {
                    returnVal = false
                    return@forEach
                }
            }
            return returnVal
        }

        fun playerInteractBlockEvent(
            player: ServerPlayerEntity,
            hand: Hand,
            heldItem: ItemStack,
            blockHitResult: BlockHitResult,
            blockPos: BlockPos,
            face: Direction
        ): Boolean {
            var returnVal = true
            forEach("playerInteractBlock") {
                it as (ServerPlayerEntity, BlockInteractionContext) -> Boolean
                val result: Boolean = it.invoke(
                    player,
                    BlockInteractionContext(player, hand, heldItem, blockHitResult, blockPos, face)
                )
                if (!result) {
                    returnVal = false
                    return@forEach
                }
            }
            return returnVal
        }
        fun playerReceivedMessageEvent(
            playerMessageReceivedEvent: PlayerReceivedMessageEvent
        ): Boolean {
            var returnVal = true
            forEach("playerReceivedMessageEvent") {
                it as (PlayerReceivedMessageEvent) -> Boolean
                val result: Boolean = it.invoke(
                    playerMessageReceivedEvent
                )
                if (!result) {
                    returnVal = false
                    return@forEach
                }
            }
            return returnVal
        }
    }
}
