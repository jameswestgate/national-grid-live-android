package com.crainiate.nationalgridlive.data.remote

/**
 * Open, key-less data sources — identical to the iOS app + backfill service.
 *
 *  - Live (last 24h) is fetched on-device from Elexon BMRS + the Carbon Intensity API.
 *  - History (Past year / All time) is pre-aggregated by the GitHub Action in the
 *    `national-grid-live-tools` repo and served as a single JSON over GitHub Pages.
 */
object Endpoints {
    /** Hosted backfill snapshot (Past year / All time aggregates), rebuilt daily. */
    const val SNAPSHOT =
        "https://jameswestgate.github.io/national-grid-live-tools/v1/snapshot.json"

    /** Cheap freshness check (ETag-friendly) before downloading the full snapshot. */
    const val MANIFEST =
        "https://jameswestgate.github.io/national-grid-live-tools/v1/manifest.json"

    /** Elexon BMRS — instantaneous generation by fuel (firm fuels + interconnectors). */
    const val FUELINST =
        "https://data.elexon.co.uk/bmrs/api/v1/datasets/FUELINST/stream"

    /** Elexon BMRS — market index price (APXMIDP). */
    const val MARKET_INDEX =
        "https://data.elexon.co.uk/bmrs/api/v1/balancing/pricing/market-index"

    /** National Grid ESO / Oxford — carbon intensity of generation. */
    const val CARBON_INTENSITY = "https://api.carbonintensity.org.uk/intensity"
    const val GENERATION_MIX = "https://api.carbonintensity.org.uk/generation"

    /**
     * NESO Data Portal — rolling ~2-month demand update CSV with embedded
     * (distribution-connected) solar + wind, which FUELINST does not include.
     * Settlement periods are in LOCAL UK time. (Resource id matches the iOS app.)
     */
    const val NESO_EMBEDDED =
        "https://api.neso.energy/dataset/7a12172a-939c-404c-b581-a6128b74f588/" +
            "resource/177f6fa4-ae49-4182-81ea-0c6b35f26ca6/download/demanddataupdate.csv"
}
