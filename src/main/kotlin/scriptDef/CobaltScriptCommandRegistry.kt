package io.github.jeefdevelopment.cobalt.event

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.CommandNode
import io.github.jeefdevelopment.cobalt.Cobalt
import net.minecraft.server.command.ServerCommandSource
import java.util.concurrent.ConcurrentHashMap

object CobaltScriptCommandRegistry {
    // Maps Script Name -> Set of registered command literal names
    private val activeScriptCommands = ConcurrentHashMap<String, MutableSet<String>>()

    var serverDispatcher: CommandDispatcher<ServerCommandSource>? = null

    /**
     * Scripts should use this function to register their custom commands.
     * Example inside a script:
     * CobaltScriptCommandRegistry.register(CommandManager.literal("myscriptcmd").executes { ... })
     */
    fun register(commandBuilder: LiteralArgumentBuilder<ServerCommandSource>) {
        val dispatcher = serverDispatcher ?: run {
            println("[Cobalt] Cannot register script command: Dispatcher is null!")
            return
        }

        val node = dispatcher.register(commandBuilder)

        // Track the command under this specific script's name
        activeScriptCommands.getOrPut(Cobalt.currentScriptName) { mutableSetOf() }.add(node.name)
    }

    /**
     * Wipes commands registered ONLY by the specified script.
     */
    fun unregisterScriptCommands(scriptName: String) {
        val root = serverDispatcher?.root ?: return
        val commandsToRemove = activeScriptCommands[scriptName] ?: return

        if (commandsToRemove.isEmpty()) return

        removeCommandsFromDispatcher(root, commandsToRemove)
        activeScriptCommands.remove(scriptName)
        println("[Cobalt] Successfully unregistered ${commandsToRemove.size} commands for script '$scriptName'.")
    }

    /**
     * Wipes ALL script commands (used during a full global reload).
     */
    fun unregisterAllScriptCommands() {
        val root = serverDispatcher?.root ?: return
        if (activeScriptCommands.isEmpty()) return

        val allCommands = activeScriptCommands.values.flatten().toSet()
        if (allCommands.isEmpty()) return

        removeCommandsFromDispatcher(root, allCommands)
        activeScriptCommands.clear()
        println("[Cobalt] Successfully unregistered ${allCommands.size} total script commands.")
    }

    /**
     * Helper function to execute the Brigadier reflection wipes.
     */
    @Suppress("UNCHECKED_CAST")
    private fun removeCommandsFromDispatcher(root: CommandNode<*>, commandsToRemove: Set<String>) {
        try {
            val childrenField = CommandNode::class.java.getDeclaredField("children")
            childrenField.isAccessible = true
            val children = childrenField.get(root) as MutableMap<String, *>

            val literalsField = CommandNode::class.java.getDeclaredField("literals")
            literalsField.isAccessible = true
            val literals = literalsField.get(root) as MutableMap<String, *>

            val argumentsField = CommandNode::class.java.getDeclaredField("arguments")
            argumentsField.isAccessible = true
            val arguments = argumentsField.get(root) as MutableMap<String, *>

            for (commandName in commandsToRemove) {
                children.remove(commandName)
                literals.remove(commandName)
                arguments.remove(commandName)
            }
        } catch (e: Exception) {
            println("[Cobalt] Failed to unregister script commands: ${e.message}")
            e.printStackTrace()
        }
    }
}