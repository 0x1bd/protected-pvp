package org.kvxd.protectedpvp.compat

import net.neoforged.fml.ModList
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.kvxd.protectedpvp.ProtectedPVPMod

class OptionalCompatibilityTest {
    @Test
    fun `mod initializes with spell mods absent`() {
        assertFalse(ModList.get().isLoaded("irons_spellbooks"))
        assertFalse(ModList.get().isLoaded("hazennstuff"))
        assertFalse(ModList.get().isLoaded("hazentouvelib"))
        assertDoesNotThrow { ProtectedPVPMod() }
    }
}
