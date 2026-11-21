package app.redflag.data

/**
 * Well-known apps (among the most installed on the Play Store) and the catalog issues they are linked to. During the welcome tour,
 * an installed app linked to the issue on screen is ticked in advance and listed first; the user can still untick it.
 *
 * Only catalog issues are covered: a custom issue has no suggestion.
 */
object AppPreselection {
    private val facebook = listOf(
        "addictive_design", "social_comparison", "fomo", "racism", "hate_content", "subcontractor_conditions", "excessive_data",
        "data_resale", "surveillance", "monopoly", "tax_evasion", "misinformation", "polarization",
    )
    private val instagram = listOf(
        "addictive_design", "social_comparison", "fomo", "sleep_impact", "sexism", "harassment", "carbon_footprint",
        "subcontractor_conditions", "excessive_data", "surveillance", "monopoly", "tax_evasion", "polarization",
    )
    private val tiktok = listOf(
        "addictive_design", "social_comparison", "fomo", "sleep_impact", "sexism", "carbon_footprint", "subcontractor_conditions",
        "excessive_data", "surveillance", "predatory_monetization", "misinformation", "polarization", "authoritarian_funding",
    )

    /** Package name → catalog issue keys. */
    val byPackage: Map<String, List<String>> = mapOf(
        // Social networks & messaging
        "com.google.android.youtube" to listOf(
            "addictive_design", "sleep_impact", "hate_content", "carbon_footprint", "surveillance", "monopoly", "tax_evasion",
            "misinformation", "polarization",
        ),
        "com.google.android.apps.youtube.kids" to listOf("addictive_design", "excessive_data"),
        "com.whatsapp" to listOf("excessive_data", "monopoly", "misinformation"),
        "com.facebook.katana" to facebook,
        "com.facebook.lite" to facebook,
        "com.facebook.orca" to listOf("excessive_data", "surveillance"),
        "com.instagram.android" to instagram,
        "com.instagram.lite" to instagram,
        "com.instagram.barcelona" to listOf("excessive_data", "misinformation", "polarization"),
        "com.zhiliaoapp.musically" to tiktok,
        "com.zhiliaoapp.musically.go" to tiktok,
        "com.snapchat.android" to listOf(
            "addictive_design", "social_comparison", "fomo", "sleep_impact", "harassment", "excessive_data", "surveillance",
        ),
        "com.twitter.android" to listOf("addictive_design", "racism", "harassment", "hate_content", "misinformation", "polarization"),
        "org.telegram.messenger" to listOf("hate_content", "misinformation"),
        "com.reddit.frontpage" to listOf("addictive_design", "harassment", "hate_content"),
        "com.discord" to listOf("sleep_impact", "harassment"),
        "com.pinterest" to listOf("social_comparison", "overconsumption"),
        "com.linkedin.android" to listOf("social_comparison", "excessive_data"),
        "video.like" to listOf("addictive_design", "excessive_data"),
        "com.kwai.video" to listOf("addictive_design", "excessive_data"),
        "com.bereal.ft" to listOf("fomo", "social_comparison"),
        "com.tencent.mm" to listOf("excessive_data", "surveillance", "authoritarian_funding"),
        "jp.naver.line.android" to listOf("excessive_data"),
        "com.viber.voip" to listOf("excessive_data"),
        "com.truecaller" to listOf("excessive_data", "data_resale"),
        "us.zoom.videomeetings" to listOf("excessive_data"),
        "com.microsoft.teams" to listOf("monopoly"),
        // Dating
        "com.tinder" to listOf("sexism", "data_resale", "predatory_monetization"),
        "com.bumble.app" to listOf("predatory_monetization"),
        "com.badoo.mobile" to listOf("excessive_data", "predatory_monetization"),
        "co.hinge.app" to listOf("predatory_monetization"),
        "com.grindrapp.android" to listOf("data_resale", "excessive_data"),
        // Video, music & streaming
        "com.netflix.mediaclient" to listOf("addictive_design", "sleep_impact", "carbon_footprint"),
        "com.amazon.avod.thirdpartyclient" to listOf("carbon_footprint", "monopoly", "tax_evasion"),
        "com.disney.disneyplus" to listOf("carbon_footprint"),
        "tv.twitch.android.app" to listOf("sleep_impact", "harassment", "carbon_footprint"),
        "com.spotify.music" to listOf("worker_exploitation", "excessive_data"),
        "com.google.android.apps.youtube.music" to listOf("monopoly"),
        "com.mxtech.videoplayer.ad" to listOf("excessive_data"),
        // Shopping
        "com.amazon.mShop.android.shopping" to listOf(
            "overconsumption", "planned_obsolescence", "worker_exploitation", "monopoly", "tax_evasion",
        ),
        "com.zzkko" to listOf("fomo", "overconsumption", "child_labor", "subcontractor_conditions", "excessive_data"),
        "com.einnovation.temu" to listOf(
            "fomo", "overconsumption", "planned_obsolescence", "subcontractor_conditions", "excessive_data", "surveillance",
            "authoritarian_funding",
        ),
        "com.alibaba.aliexpresshd" to listOf("overconsumption", "planned_obsolescence"),
        "com.contextlogic.wish" to listOf("overconsumption", "planned_obsolescence"),
        "com.mercadolibre" to listOf("overconsumption"),
        "de.zalando.mobile" to listOf("overconsumption"),
        "fr.vinted" to listOf("overconsumption"),
        "com.lidl.eci.lidlplus" to listOf("excessive_data"),
        "com.binance.dev" to listOf("predatory_monetization"),
        // Transport, delivery & travel
        "com.ubercab" to listOf("worker_exploitation", "monopoly", "tax_evasion"),
        "com.ubercab.eats" to listOf("overconsumption", "worker_exploitation"),
        "com.deliveroo.orderapp" to listOf("worker_exploitation"),
        "com.glovo" to listOf("worker_exploitation"),
        "ee.mtakso.client" to listOf("worker_exploitation"),
        "com.waze" to listOf("surveillance"),
        "com.booking" to listOf("monopoly"),
        "com.airbnb.android" to listOf("racism"),
        "com.ryanair.cheapflights" to listOf("carbon_footprint", "worker_exploitation"),
        "com.strava" to listOf("social_comparison", "surveillance"),
        // Games
        "com.king.candycrushsaga" to listOf("addictive_design", "predatory_monetization"),
        "com.king.candycrushsodasaga" to listOf("addictive_design", "predatory_monetization"),
        "com.kiloo.subwaysurf" to listOf("addictive_design"),
        "com.imangi.templerun2" to listOf("addictive_design"),
        "com.h8games.helixjump" to listOf("addictive_design"),
        "com.roblox.client" to listOf("addictive_design", "harassment", "predatory_monetization", "worker_exploitation"),
        "com.dts.freefireth" to listOf("harassment", "predatory_monetization"),
        "com.dts.freefiremax" to listOf("harassment", "predatory_monetization"),
        "com.tencent.ig" to listOf("predatory_monetization", "authoritarian_funding"),
        "com.activision.callofduty.shooter" to listOf("harassment", "predatory_monetization"),
        "com.mobile.legends" to listOf("harassment", "predatory_monetization", "authoritarian_funding"),
        "com.supercell.clashofclans" to listOf("predatory_monetization"),
        "com.supercell.clashroyale" to listOf("predatory_monetization"),
        "com.supercell.brawlstars" to listOf("predatory_monetization"),
        "com.supercell.hayday" to listOf("predatory_monetization"),
        "com.miHoYo.GenshinImpact" to listOf("predatory_monetization"),
        "com.moonactive.coinmaster" to listOf("addictive_design", "predatory_monetization"),
        "com.scopely.monopolygo" to listOf("addictive_design", "predatory_monetization"),
        "com.dreamgames.royalmatch" to listOf("addictive_design", "predatory_monetization"),
        "com.playrix.homescapes" to listOf("predatory_monetization"),
        "com.playrix.gardenscapes" to listOf("predatory_monetization"),
        "com.miniclip.eightballpool" to listOf("predatory_monetization"),
        "com.kitkagames.fallbuddies" to listOf("predatory_monetization"),
        "com.outfit7.mytalkingtomfree" to listOf("excessive_data", "predatory_monetization"),
        "com.nianticlabs.pokemongo" to listOf("surveillance", "predatory_monetization"),
        "com.innersloth.spacemafia" to listOf("harassment"),
        // Tools & AI
        "com.openai.chatgpt" to listOf("carbon_footprint", "subcontractor_conditions", "excessive_data"),
        "com.google.android.apps.bard" to listOf("carbon_footprint", "excessive_data"),
        "com.lightricks.facetune.free" to listOf("social_comparison", "sexism"),
        "com.duolingo" to listOf("addictive_design"),
    )

    private val byIssue: Map<String, Set<String>> = byPackage.entries
        .flatMap { (pkg, keys) -> keys.map { it to pkg } }
        .groupBy({ it.first }, { it.second })
        .mapValues { it.value.toSet() }

    /** The packages suggested for [ref]: none for a custom issue. */
    fun suggestedFor(ref: ProblemRef): Set<String> = ref.catalogKey?.let { byIssue[it] }.orEmpty()
}
