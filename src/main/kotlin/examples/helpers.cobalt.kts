/**
 * Send messages to discord easily! replace the string of numbers with the channel id!
 */
fun discordPrint(message: String){
    val jda = BotController.INSTANCE.jda
    val channel = jda.getTextChannelById("1480993849991106643")
    channel?.sendMessage(message)?.queue()
}

/**
 * Get the same colors for each type as is used everywhere, with a variant for legacy and minimessage color codes!
 */
fun String.typeColor(): String{
    when (this.lowercase()) {
        "normal" -> { return "&f${this.capitalize()}" }
        "fighting" -> { return "&4${this.capitalize()}" }
        "flying" -> { return "&b${this.capitalize()}" }
        "poison" -> { return "&5${this.capitalize()}" }
        "ground" -> { return "&6${this.capitalize()}" }
        "rock" -> { return "&6${this.capitalize()}" }
        "bug" -> { return "&a${this.capitalize()}" }
        "ghost" -> { return "&5${this.capitalize()}" }
        "steel" -> { return "&7${this.capitalize()}" }
        "fire" -> { return "&4${this.capitalize()}" }
        "water" -> { return "&3${this.capitalize()}" }
        "grass" -> { return "&2${this.capitalize()}" }
        "electric" -> { return "&e${this.capitalize()}" }
        "psychic" -> { return "&d${this.capitalize()}" }
        "ice" -> { return "&b${this.capitalize()}" }
        "dragon" -> { return "&9${this.capitalize()}" }
        "dark" -> { return "&8${this.capitalize()}" }
        "fairy" -> { return "&d${this.capitalize()}" }
    }
    return this
}

fun String.typeColorMinimessage(): String{
    when (this.lowercase()) {
        "normal" -> { return "<white>${this.capitalize()}" }
        "fighting" -> { return "<dark_red>${this.capitalize()}" }
        "flying" -> { return "<aqua>${this.capitalize()}" }
        "poison" -> { return "<dark_purple>${this.capitalize()}" }
        "ground" -> { return "<gold>${this.capitalize()}" }
        "rock" -> { return "<gold>${this.capitalize()}" }
        "bug" -> { return "<green>${this.capitalize()}" }
        "ghost" -> { return "<dark_purple>${this.capitalize()}" }
        "steel" -> { return "<gray>${this.capitalize()}" }
        "fire" -> { return "<dark_red>${this.capitalize()}" }
        "water" -> { return "<dark_aqua>${this.capitalize()}" }
        "grass" -> { return "<dark_green>${this.capitalize()}" }
        "electric" -> { return "<yellow>${this.capitalize()}" }
        "psychic" -> { return "<light_purple>${this.capitalize()}" }
        "ice" -> { return "<aqua>${this.capitalize()}" }
        "dragon" -> { return "<blue>${this.capitalize()}" }
        "dark" -> { return "<dark_gray>${this.capitalize()}" }
        "fairy" -> { return "<light_purple>${this.capitalize()}" }
    }
    return this
}

/**
 *  Save peoples positions and tp them back later!
 */
val savedCoordinates = hashMapOf<UUID, Coordinates>()

data class Coordinates(val world: class_3218, val x: Double, val y: Double, val z: Double, val yaw: Float, val pitch: Float)

fun UUID.savePos(){
    val player = getPlayer() ?: return
    val world = player.method_51469()
    val x = player.method_23317()
    val y = player.method_23318()
    val z = player.method_23321()
    val yaw = player.method_5791()
    val pitch = player.method_36455()
    savedCoordinates[this] = Coordinates(world, x, y, z, yaw, pitch)
}

fun UUID.loadPos(){
    val player = getPlayer() ?: return
    val coordinates = savedCoordinates[this] ?: return
    player.method_14251(coordinates.world, coordinates.x, coordinates.y, coordinates.z, coordinates.yaw, coordinates.pitch)
    savedCoordinates.remove(this)
}

/**
 * Check if a player is spectating a battle
 */
fun ServerPlayerEntity.isSpectating(): Boolean{
    val playerUUID = method_5667()
    val registry = Class.forName("com.cobblemon.mod.common.battles.BattleRegistry").getDeclaredField("battleMap").also { it.isAccessible = true }.get(null) as ConcurrentHashMap<UUID, PokemonBattle>
    return registry.values.any { it.spectators.contains(playerUUID) }
}

/**
 * Get item stack displays for a pokemon, for use in GUIs
 */
fun Pokemon.itemConvert(): ItemStack {
    val itemMon = PokemonItem.from(this).withName("&b${this.species.name}")
    return itemMon
}

/**
 * Functions for sgui to fill all empty slots with an item, and to fill every slot with an item
 */
fun SimpleGui.fillBackground(background: ItemStack){
    while (this.firstEmptySlot >= 0){
        this.setSlot(this.firstEmptySlot, background)
    }
}

fun SimpleGui.resetBackground(background: ItemStack){
    for (x in (0..<this.size)){
        this.setSlot(x, background)
    }
}

/**
 * For general currency rewards that don't count towards the weekly caps
 */
fun ServerPlayerEntity.giveDollar(int: Int){
    EconomyService.instance().account(EconomyService.instance().currencies().currency(Key.key("impactor:dollars")).get(), method_5667() //get uuid
    ).get().deposit(int.toBigDecimal())
}

fun ServerPlayerEntity.giveToken(int: Int){
    EconomyService.instance().account(EconomyService.instance().currencies().currency(Key.key("impactor:tokens")).get(), method_5667() //get uuid
    ).get().deposit(int.toBigDecimal())
}

fun ServerPlayerEntity.giveBP(int: Int){
    EconomyService.instance().account(EconomyService.instance().currencies().currency(Key.key("impactor:battlepoints")).get(), method_5667() //get uuid
    ).get().deposit(int.toBigDecimal())
}

fun ServerPlayerEntity.giveGem(int: Int){
    EconomyService.instance().account(EconomyService.instance().currencies().currency(Key.key("impactor:gems")).get(), method_5667() //get uuid
    ).get().deposit(int.toBigDecimal())
}

fun ServerPlayerEntity.currencyRewards(dollars: Int, tokens: Int, battlePoints: Int, gems: Int?){
    giveDollar(dollars)
    giveBP(battlePoints)
    giveToken(tokens)
    if (gems == null){
        tell("&eYou got &a$${formatNumberWithCommas(dollars)}&e, ${formatNumberWithCommas(tokens)} Tokens, and &6${formatNumberWithCommas(battlePoints)} Battle Points&e.")
    }
    else {
        giveGem(gems)
        tell("&eYou got &a$${formatNumberWithCommas(dollars)}&e, ${formatNumberWithCommas(tokens)} Tokens, &6${formatNumberWithCommas(battlePoints)} Battle Points&e, and &c${formatNumberWithCommas(gems)} Gems&e.")
    }
}