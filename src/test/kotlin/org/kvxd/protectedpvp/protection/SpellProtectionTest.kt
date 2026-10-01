package org.kvxd.protectedpvp.protection

import net.minecraft.SharedConstants
import net.minecraft.server.Bootstrap
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.TraceableEntity
import net.minecraft.world.level.storage.LevelResource
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.kvxd.protectedpvp.persistence.PvpPreferenceStore
import org.mockito.Mockito.*
import java.nio.file.Path
import java.util.UUID

class SpellProtectionTest {
    @TempDir
    lateinit var world: Path

    @Test
    fun `summon damage and summon projectiles resolve to the player`() {
        val caster = player()
        val summon = entity()
        val projectile = mock(Entity::class.java, withSettings().extraInterfaces(TraceableEntity::class.java))
        `when`(projectile.uuid).thenReturn(UUID.randomUUID())
        `when`((projectile as TraceableEntity).owner).thenReturn(summon)
        val attribution = DamageAttribution(ExplosiveInteractionTracker()) { if (it === summon) caster else null }

        assertSame(caster, attribution.findAttackingPlayer(player(), source(summon)))
        assertSame(caster, attribution.findAttackingPlayer(player(), source(projectile)))
    }

    @Test
    fun `owner cycles terminate and direct source still resolves`() {
        val first = entity()
        val second = entity()
        val caster = player()
        val attribution = DamageAttribution(ExplosiveInteractionTracker()) {
            when (it) {
                first -> second
                second -> first
                else -> null
            }
        }
        val source = source(first)
        `when`(source.directEntity).thenReturn(caster)
        assertSame(caster, attribution.findAttackingPlayer(player(), source))
    }

    @Test
    fun `protected spell and summon hits are rejected without enabling the attacker`() {
        val caster = player()
        val target = player()
        val summon = entity()
        val service = service { if (it === summon) caster else null }

        assertFalse(service.allowSpellDamage(target, source(caster), 10f))
        assertFalse(service.allowSpellDamage(target, source(summon), 10f))
        assertFalse(service.allowDamage(target, source(summon), 10f))
        assertFalse(preferences().isPvpEnabled(caster.uuid))
    }

    @Test
    fun `spell precheck does not enable PvP until incoming damage`() {
        val caster = player()
        val target = player()
        val service = service()
        service.enablePvp(target)

        assertTrue(service.allowSpellDamage(target, source(caster), 10f))
        assertFalse(preferences().isPvpEnabled(caster.uuid))
        assertTrue(service.allowDamage(target, source(caster), 10f))
        assertTrue(preferences().isPvpEnabled(caster.uuid))
        assertEquals(0, service.enableProtection(caster))
        assertEquals(0, service.enableProtection(target))
    }

    @Test
    fun `self damage mob spells and zero damage remain allowed`() {
        val caster = player()
        val service = service()
        assertTrue(service.allowSpellDamage(caster, source(caster), 10f))
        assertTrue(service.allowSpellDamage(caster, source(entity()), 10f))
        assertTrue(service.allowSpellDamage(player(), source(caster), 0f))
        assertFalse(preferences().isPvpEnabled(caster.uuid))
    }

    private fun service(owner: (Entity) -> Entity? = { null }): PvpProtectionService {
        val server = mock(MinecraftServer::class.java)
        `when`(server.getWorldPath(LevelResource.ROOT)).thenReturn(world)
        return PvpProtectionService(owner).also { it.start(server) }
    }

    private fun preferences() = PvpPreferenceStore.load(world.resolve("data/protected-pvp.json"))

    private fun player(): ServerPlayer = mock(ServerPlayer::class.java).also {
        `when`(it.uuid).thenReturn(UUID.randomUUID())
        `when`(it.serverLevel()).thenReturn(mock(ServerLevel::class.java))
    }

    private fun entity(): Entity = mock(Entity::class.java).also {
        `when`(it.uuid).thenReturn(UUID.randomUUID())
    }

    private fun source(attacker: Entity): DamageSource = mock(DamageSource::class.java).also {
        `when`(it.entity).thenReturn(attacker)
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun bootstrap() {
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
        }
    }
}
