package com.crainiate.nationalgridlive.data.remote

import com.crainiate.nationalgridlive.data.model.FuelType
import com.crainiate.nationalgridlive.data.model.Interconnector

/**
 * Maps Elexon FUELINST codes to the app's [FuelType] (ported from the iOS
 * `FuelCodeMap`). Multiple physical codes group into one fuel (e.g. CCGT+OCGT+OIL
 * → Gas). `PS` (pumped storage), `BESS` (battery) and `OTHER` are intentionally
 * dropped — like the website, the displayed Generation total excludes them.
 * Solar is not in FUELINST at all; it comes from NESO embedded.
 */
object FuelCodeMap {
    fun fuel(code: String): FuelType? = when (code) {
        "CCGT", "OCGT", "OIL" -> FuelType.Gas
        "COAL" -> FuelType.Coal
        "WIND" -> FuelType.Wind        // transmission only; embedded wind added separately
        "NPSHYD" -> FuelType.Hydro
        "NUCLEAR" -> FuelType.Nuclear
        "BIOMASS" -> FuelType.Biomass
        else -> null                   // PS, BESS, OTHER, INT* …
    }

    /** Interconnector codes grouped by country (France = IFA+IFA2+ELECLINK, etc.). */
    fun interconnector(code: String): Interconnector? = when (code) {
        "INTFR", "INTIFA2", "INTELEC" -> Interconnector.France
        "INTIRL", "INTEW", "INTGRNL" -> Interconnector.Ireland
        "INTNED", "INTBRITNED" -> Interconnector.Netherlands
        "INTNEM" -> Interconnector.Belgium
        "INTNSL" -> Interconnector.Norway
        "INTVKL" -> Interconnector.Denmark
        else -> null
    }

    /** Pumped-storage code (reported under the Storage card, not generation). */
    const val PUMPED = "PS"
}
