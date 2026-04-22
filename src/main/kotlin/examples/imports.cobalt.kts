import net.minecraft.class_3222 as ServerPlayerEntity
import net.minecraft.class_1799 as ItemStack
import java.util.UUID
import java.nio.file.Path
import kotlin.collections.random

// commands
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.class_2170.method_9247 as literal
import net.minecraft.class_2170.method_9244 as argument
import net.minecraft.class_2186.method_9305 as player
import net.minecraft.class_2186.method_9313 as getEntity
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.cobblemon.mod.common.api.permission.CobblemonPermission
import com.cobblemon.mod.common.api.permission.PermissionLevel
import com.mojang.brigadier.arguments.IntegerArgumentType

// gooeylibs
import ca.landonjw.gooeylibs2.api.button.Button
import ca.landonjw.gooeylibs2.api.page.GooeyPage
import ca.landonjw.gooeylibs2.api.UIManager
import ca.landonjw.gooeylibs2.api.button.ButtonClick
import ca.landonjw.gooeylibs2.api.button.GooeyButton
import ca.landonjw.gooeylibs2.api.button.PlaceholderButton
import ca.landonjw.gooeylibs2.api.button.linked.LinkType
import ca.landonjw.gooeylibs2.api.button.linked.LinkedPageButton
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper
import ca.landonjw.gooeylibs2.api.page.LinkedPage
import ca.landonjw.gooeylibs2.api.page.PageAction
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate

// sgui
import eu.pb4.sgui.api.elements.GuiElementInterface
import eu.pb4.sgui.api.gui.SimpleGui
import eu.pb4.sgui.api.ClickType
import eu.pb4.sgui.api.gui.AnvilInputGui


// molang
import com.bedrockk.molang.runtime.value.DoubleValue
import com.bedrockk.molang.runtime.value.StringValue
import com.cobblemon.mod.common.util.asArrayValue
import com.bedrockk.molang.runtime.value.MoValue

// misc non cobble
import net.luckperms.api.LuckPermsProvider
import com.hypherionmc.sdlink.core.discord.BotController
import net.impactdev.impactor.api.economy.EconomyService
import net.impactdev.impactor.api.economy.accounts.Account
import io.github.jeefdevelopment.cobaltafk.CobaltAfk.Companion.isAfk
import me.drex.vanish.api.VanishAPI

// misc cobble

import com.cobblemon.mod.common.pokemon.Species
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.api.pokemon.PokemonPropertyExtractor
import com.cobblemon.mod.common.api.scheduling.ScheduledTask
import com.cobblemon.mod.common.api.scheduling.taskBuilder
import com.cobblemon.mod.common.item.PokemonItem
import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.util.server
import com.cobblemon.mod.common.util.party
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.pc
import com.cobblemon.mod.common.util.toProperties
import com.cobblemon.mod.common.util.asTranslated
import com.cobblemon.mod.common.util.getPlayer
import com.cobblemon.mod.common.util.isInBattle
import com.cobblemon.mod.common.util.isPartyBusy
import com.cobblemon.mod.common.util.removeIf
