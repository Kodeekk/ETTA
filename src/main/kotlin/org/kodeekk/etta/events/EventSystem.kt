package org.kodeekk.etta.events

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.BlockItem
import net.minecraft.world.level.dimension.BuiltinDimensionTypes
import org.slf4j.LoggerFactory

object EventSystem {
    private val logger = LoggerFactory.getLogger("ETTA-Events")
    private val eventConditions = mutableMapOf<String, EventCondition>()
    private val activeEvents = mutableSetOf<String>()
    private val eventCache = mutableMapOf<String, Boolean>()

    init {
        registerBuiltInEvents()
    }

    fun registerEvent(eventId: String, condition: EventCondition) {
        eventConditions[eventId.lowercase()] = condition
        logger.debug("Registered event: $eventId")
    }

    fun isEventActive(eventId: String): Boolean {
        val normalized = eventId.lowercase()
        return eventCache.getOrPut(normalized) {
            eventConditions[normalized]?.check() ?: false
        }
    }

    fun tick() {
        eventCache.clear()
        val previousActiveEvents = activeEvents.toSet()
        activeEvents.clear()

        eventConditions.forEach { (id, condition) ->
            if (condition.check()) {
                activeEvents.add(id)
            }
        }

        // Log event changes for debugging
        val activated = activeEvents - previousActiveEvents
        val deactivated = previousActiveEvents - activeEvents

        activated.forEach { event ->
            logger.debug("EVENT ACTIVATED: $event")
        }
        deactivated.forEach { event ->
            logger.debug("EVENT DEACTIVATED: $event")
        }
    }

    fun getActiveEvents(): Set<String> = activeEvents.toSet()

    private fun registerBuiltInEvents() {
        // Player movement states
        registerEvent("player_sneaking") {
            Minecraft.getInstance().player?.isCrouching == true
        }
        registerEvent("player_sprinting") {
            Minecraft.getInstance().player?.isSprinting == true
        }
        registerEvent("player_swimming") {
            Minecraft.getInstance().player?.isSwimming == true
        }
        registerEvent("player_jumping") {
            Minecraft.getInstance().player?.let {
                !it.onGround  && it.deltaMovement.y > 0.1
            } == true
        }
        registerEvent("player_falling") {
            Minecraft.getInstance().player?.let {
                !it.onGround && it.deltaMovement.y < -0.1
            } == true
        }
        registerEvent("player_flying") {
            Minecraft.getInstance().player?.abilities?.flying == true
        }
        // REMOVED: player_gliding (isFallFlying not available in all versions)

        // Environmental states
        registerEvent("player_in_water") {
            Minecraft.getInstance().player?.isInWater == true
        }
        registerEvent("player_in_lava") {
            Minecraft.getInstance().player?.isInLava == true
        }
        registerEvent("player_on_fire") {
            Minecraft.getInstance().player?.isOnFire == true
        }
        registerEvent("player_underwater") {
            Minecraft.getInstance().player?.isUnderWater == true
        }

        // Time/weather
        registerEvent("daytime") {
            Minecraft.getInstance().level?.isBrightOutside == true
        }
        registerEvent("nighttime") {
            Minecraft.getInstance().level?.isDarkOutside == true
        }
        registerEvent("raining") {
            Minecraft.getInstance().level?.isRaining == true
        }
        registerEvent("thundering") {
            Minecraft.getInstance().level?.isThundering == true
        }

        // Health states
        registerEvent("low_health") {
            Minecraft.getInstance().player?.let {
                it.health / it.maxHealth <= 0.25f
            } == true
        }
        registerEvent("critical_health") {
            Minecraft.getInstance().player?.let {
                it.health / it.maxHealth <= 0.1f
            } == true
        }
        registerEvent("full_health") {
            Minecraft.getInstance().player?.let {
                it.health >= it.maxHealth
            } == true
        }

        // Combat
        registerEvent("player_attacking") {
            Minecraft.getInstance().player?.let {
                it.swinging && it.swingTime < 3
            } == true
        }

        // Equipment
        registerEvent("wearing_helmet") {
            Minecraft.getInstance().player?.getItemBySlot(EquipmentSlot.HEAD)?.isEmpty == false
        }
        registerEvent("wearing_elytra") {
            Minecraft.getInstance().player?.getItemBySlot(EquipmentSlot.CHEST)?.let {
                it.item.name.toString().lowercase().contains("elytra")
            } == true
        }
        registerEvent("player_idle") {
            Minecraft.getInstance().player?.let {
                it.deltaMovement.lengthSqr() < 0.0001 && it.onGround
            } == true
        }

        registerEvent("player_moving") {
            Minecraft.getInstance().player?.let {
                it.deltaMovement.horizontalDistanceSqr() > 0.001
            } == true
        }

        registerEvent("player_airborne") {
            Minecraft.getInstance().player?.onGround == false
        }

        registerEvent("player_crouch_walking") {
            Minecraft.getInstance().player?.let {
                it.isCrouching && it.deltaMovement.horizontalDistanceSqr() > 0.001
            } == true
        }
        registerEvent("first_person") {
            Minecraft.getInstance().options.cameraType.isFirstPerson
        }

        registerEvent("third_person") {
            !Minecraft.getInstance().options.cameraType.isFirstPerson
        }
        registerEvent("has_effects") {
            Minecraft.getInstance().player?.activeEffects?.isNotEmpty() == true
        }

        registerEvent("invisible") {
            Minecraft.getInstance().player?.isInvisible == true
        }

        registerEvent("on_ground_stationary") {
            Minecraft.getInstance().player?.let {
                it.onGround && it.deltaMovement.lengthSqr() < 0.0001
            } == true
        }
        registerEvent("hungry") {
            Minecraft.getInstance().player?.foodData?.foodLevel ?: 20 < 10
        }

        registerEvent("starving") {
            Minecraft.getInstance().player?.foodData?.foodLevel ?: 20 <= 6
        }

        registerEvent("full_hunger") {
            Minecraft.getInstance().player?.foodData?.foodLevel == 20
        }
        registerEvent("armor_full") {
            Minecraft.getInstance().player?.let {
                !it.getItemBySlot(EquipmentSlot.HEAD).isEmpty &&
                        !it.getItemBySlot(EquipmentSlot.CHEST).isEmpty &&
                        !it.getItemBySlot(EquipmentSlot.LEGS).isEmpty &&
                        !it.getItemBySlot(EquipmentSlot.FEET).isEmpty
            } == true
        }

        registerEvent("armor_empty") {
            Minecraft.getInstance().player?.let {
                it.getItemBySlot(EquipmentSlot.HEAD).isEmpty &&
                        it.getItemBySlot(EquipmentSlot.CHEST).isEmpty &&
                        it.getItemBySlot(EquipmentSlot.LEGS).isEmpty &&
                        it.getItemBySlot(EquipmentSlot.FEET).isEmpty
            } == true
        }


        registerEvent("holding_item") {
            Minecraft.getInstance().player?.mainHandItem?.isEmpty == false
        }

        registerEvent("holding_block") {
            Minecraft.getInstance().player?.mainHandItem?.item is BlockItem
        }
        registerEvent("hurt_recently") {
            Minecraft.getInstance().player?.hurtTime ?: 0 > 0
        }

        registerEvent("invulnerable") {
            Minecraft.getInstance().player?.isInvulnerable == true
        }

        registerEvent("near_death") {
            Minecraft.getInstance().player?.health ?: 20f <= 2f
        }
        registerEvent("in_nether") {
            Minecraft.getInstance().level?.dimensionType() == BuiltinDimensionTypes.NETHER
        }

        registerEvent("in_end") {
            Minecraft.getInstance().level?.dimensionType() == BuiltinDimensionTypes.END
        }

        registerEvent("overworld") {
            Minecraft.getInstance().level?.dimensionType() == BuiltinDimensionTypes.OVERWORLD
        }
        registerEvent("sunrise") {
            Minecraft.getInstance().level?.dayTime?.let { it in 23000..24000 || it in 0..1000 } == true
        }

        registerEvent("sunset") {
            Minecraft.getInstance().level?.dayTime?.let { it in 12000..13000 } == true
        }

        registerEvent("midnight") {
            Minecraft.getInstance().level?.dayTime?.let { it in 18000..19000 } == true
        }
        registerEvent("gui_open") {
            Minecraft.getInstance().screen != null
        }

        registerEvent("chat_open") {
            Minecraft.getInstance().screen?.javaClass?.simpleName == "ChatScreen"
        }
    }
}

fun EventSystem.registerEvent(eventId: String, check: () -> Boolean) {
    registerEvent(eventId, EventCondition { check() })
}