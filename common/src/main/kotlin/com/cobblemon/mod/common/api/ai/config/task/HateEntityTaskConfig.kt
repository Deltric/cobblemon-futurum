/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.ai.config.task

import com.bedrockk.molang.Expression
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.ai.BehaviourConfigurationContext
import com.cobblemon.mod.common.api.ai.ExpressionOrEntityVariable
import com.cobblemon.mod.common.api.molang.MoLangFunctions.asMostSpecificMoLangValue
import com.cobblemon.mod.common.api.npc.configuration.MoLangConfigVariable
import com.cobblemon.mod.common.util.asExpression
import com.cobblemon.mod.common.util.mainThreadRuntime
import com.cobblemon.mod.common.util.resolveBoolean
import com.cobblemon.mod.common.util.withQueryValue
import com.cobblemon.mod.common.world.gamerules.CobblemonGameRules
import com.mojang.datafixers.util.Either
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.behavior.BehaviorControl
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder
import net.minecraft.world.entity.ai.behavior.declarative.Trigger
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.entity.ai.sensing.SensorType

/**
 * Immediately gets angry at the nearest visible entity that matches the [entityCondition] and is within the [range].
 *
 * @author Hiroku
 * @since June 23rd, 2025
 */
class HateEntityTaskConfig : SingleTaskConfig {
    val entityCondition: Expression = "true".asExpression()
    val range: ExpressionOrEntityVariable = Either.left("24".asExpression())
    val chance: Float = Cobblemon.config.predationChance
    private val coercedChance = chance.coerceIn(0f, 1f)

    override fun getVariables(entity: LivingEntity, behaviourConfigurationContext: BehaviourConfigurationContext) = emptyList<MoLangConfigVariable>()
    override fun createTask(
        entity: LivingEntity,
        behaviourConfigurationContext: BehaviourConfigurationContext
    ): BehaviorControl<in LivingEntity>? {
        val range = range.resolveFloat(behaviourConfigurationContext.runtime)
        behaviourConfigurationContext.addMemories(
            MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
            MemoryModuleType.ANGRY_AT,
            MemoryModuleType.ATTACK_TARGET
        )
        behaviourConfigurationContext.addSensors(SensorType.NEAREST_LIVING_ENTITIES)
        return BehaviorBuilder.create { instance ->
            instance.group(
                instance.present(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES),
                instance.registered(MemoryModuleType.ANGRY_AT),
                instance.registered(MemoryModuleType.ATTACK_TARGET),
            ).apply(instance) { entities, angryAt, attackTarget ->
                Trigger { world, entity, _ ->
                    // bail if gamerule is disabled
                    if (!world.gameRules.get(CobblemonGameRules.DO_POKEMON_PREDATION)) return@Trigger false
                    val target = instance.get(entities).findClosest { hated ->
                        mainThreadRuntime.withQueryValue("entity", hated.asMostSpecificMoLangValue())
                        return@findClosest mainThreadRuntime.resolveBoolean(entityCondition)
                    }.orElse(null)

                    var shouldTrigger = false
                    if (target != null && target.distanceTo(entity) <= range) {
                        shouldTrigger = when {
                            // set always attacking if activity chance is equal to 1.0
                            coercedChance == 1f -> {
                                angryAt.set(target.uuid)
                                attackTarget.set(target)
                                true
                            }
                            // roll the dice in other way
                            coercedChance != 1f && world.random.nextFloat() < coercedChance -> {
                                angryAt.set(target.uuid)
                                attackTarget.set(target)
                                true
                            }
                            // fallback to peaceful behavior in unattended cases
                            else -> false
                        }
                    }
                    return@Trigger shouldTrigger
                }
            }
        }
    }
}
