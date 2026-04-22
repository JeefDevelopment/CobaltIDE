package io.github.jeefdevelopment.cobalt.event

import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.util.removeIf
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.github.jeefdevelopment.cobalt.Cobalt
import io.github.jeefdevelopment.cobalt.Cobalt.Companion.LOGGER
import io.github.jeefdevelopment.cobalt.Cobalt.Companion.evalFile
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import org.apache.logging.log4j.Level
import java.io.File
import java.util.*
import kotlin.script.experimental.api.ScriptDiagnostic


object CobaltCommand {
    val cobaltCommands = HashMap<String, (ServerPlayerEntity?) -> Unit>()

    fun register(
        dispatcher: CommandDispatcher<ServerCommandSource?>,
        registryAccess: CommandRegistryAccess,
        environment: CommandManager.RegistrationEnvironment
    ) {
        val SCRIPT_NAME_SUGGESTIONS =
            SuggestionProvider { context: CommandContext<ServerCommandSource?>?, builder: SuggestionsBuilder? ->
                val scriptsDirectory = File("config/cobalt/scripts")
                val scripts = scriptsDirectory.walk().filter { it.name.endsWith(".cobalt.kts") }

                for (script in scripts) {
                    builder!!.suggest(script.name.replace(".cobalt.kts", ""))
                }
                builder!!.buildFuture()
            }
        val FUNCTION_NAME_SUGGESTIONS =
            SuggestionProvider { context: CommandContext<ServerCommandSource?>?, builder: SuggestionsBuilder? ->

                for ((callbackName, _) in cobaltCommands) {
                    builder!!.suggest(callbackName)
                }
                builder!!.buildFuture()
            }
        val CALLBACK_SUGGESTIONS =
            SuggestionProvider { context: CommandContext<ServerCommandSource?>?, builder: SuggestionsBuilder? ->

                for ((callbackName, _) in CobaltReloadableCallbacks.effectiveCobaltEvents.reloadableCallbacks) {
                    builder!!.suggest(callbackName)

                }
                builder!!.buildFuture()
            }
        dispatcher.register(
            CommandManager.literal("cobalt")
                .requires { it.hasPermissionLevel(4) }
                .then(
                    CommandManager.literal("run")
                        .then(
                            CommandManager.argument("function", StringArgumentType.word()).suggests (FUNCTION_NAME_SUGGESTIONS)
                                .then(CommandManager.argument("player", EntityArgumentType.player()).executes {
                                    val functionName = StringArgumentType.getString(it, "function")
                                    if (!cobaltCommands.contains(functionName)) {
                                        return@executes 0

                                    }
                                    val player = EntityArgumentType.getPlayer(it, "player")
                                    cobaltCommands[functionName]!!.invoke(player)
                                    return@executes 1
                                })
                                .executes {
                                    val functionName = StringArgumentType.getString(it, "function")
                                    cobaltCommands[functionName]!!.invoke(null)
                                    return@executes 1
                                })
                ).then(CommandManager.literal("clear").executes {
                    CobaltReloadableCallbacks.effectiveCobaltEvents.clearFunctions()
                    return@executes 1
                }.then(CommandManager.argument("callback", StringArgumentType.word()).suggests (CALLBACK_SUGGESTIONS).executes {
                    val scriptName = StringArgumentType.getString(it, "callback")
                    CobaltReloadableCallbacks.effectiveCobaltEvents.reloadableCallbacks.removeIf { it.key == scriptName }
                    return@executes 1
                }))
                .then(CommandManager.literal("reload").executes {
                    it.source.sendMessage("&9Reloading Cobalt Scripts".text())

                    try {
                        Cobalt.instance.reloadScripts()
                    } catch (e: Error) {
                        it.source.sendError(e.message!!.text())
                        return@executes 0
                    }

                    CommandRegistrationCallback.EVENT.invoker().register(dispatcher,registryAccess,environment)

                    //Update Command Tree
                    for (e in it.getSource().server.playerManager.playerList) {
                        it.getSource().server.playerManager.sendCommandTree(e)
                    }

                    it.source.sendMessage("&9Reloaded Cobalt Scripts".text())

                    return@executes 1
                }.then(CommandManager.argument("script", StringArgumentType.word()).suggests (SCRIPT_NAME_SUGGESTIONS).executes {

                    val scriptName = StringArgumentType.getString(it, "script")
                    CobaltReloadableCallbacks.effectiveCobaltEvents.clearFunctions(scriptName)

                    val scriptFile = File("config/cobalt/scripts/$scriptName.cobalt.kts")
                    if(!scriptFile.exists()){
                        it.source.sendError("Script $scriptFile does not exist!".text())
                        return@executes 0
                    }
                    println("Executing script $scriptFile")
                    val res = evalFile(scriptFile)
                    res.reports.forEach {
                        if (it.severity > ScriptDiagnostic.Severity.DEBUG) {
                            LOGGER.log(
                                Level.getLevel(it.severity.name),
                                " : ${it.message}" + if (it.exception == null) "" else ": ${it.exception}"
                            )
                        }
                        if (it.severity >= ScriptDiagnostic.Severity.ERROR) {
                            println(it.render(true,true,true,true))
                            throw(Exception(it.render(
                                withSeverity = true,
                                withLocation = true,
                                withException = true,
                                withStackTrace = true
                            )))
                        }
                    }
                    CobaltReloadableCallbacks.reloadFunctions(scriptName)
                    CommandRegistrationCallback.EVENT.invoker().register(dispatcher,registryAccess,environment)

                    //Update Command Tree
                    for (e in it.getSource().server.playerManager.playerList) {
                        it.getSource().server.playerManager.sendCommandTree(e)
                    }

                    it.source.sendMessage("&9Reloaded $scriptFile".text())
                    return@executes 1
                }))
        )
    }
}