package agentic.triad.missioncontrol.ui.views

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// ══════════════════════════════════════════════════════════════════════════════════════════════════
// SUITE · Lab data — the matrix cohorts (MX) + the saved-experiment store.
//
// MX is the doc's snapshot cohort data (TRIAD-Suite-1.html `const MX`). A lab combo's generator maps to
// a shadow cohort (incl. rejected = WITHOUT-LLM lens) and a paper cohort (accepted only = WITH-LLM
// lens); the aggregate over all 45 symbols is the with-vs-without-LLM calculation. Filters are analytic
// slices (W-63 law: combos are cells, not tracks) and do NOT change the cohort in this snapshot.
// ══════════════════════════════════════════════════════════════════════════════════════════════════

/** One cohort aggregate — n candidates, resolved, weighted WR, net R, EV (= net/res). */
data class Agg(val n: Int, val res: Int, val wr: Double?, val net: Double, val ev: Double?)

private const val MX_JSON = """{"cohorts":{"TRIAD-A":{"BTC":[2882,159,19.5,-54.2,-0.341],"ETH":[2793,120,30.0,7.5,0.063],"XRP":[2050,107,41.1,39.4,0.368],"AVAX":[1959,180,27.8,-5.0,-0.028],"LINK":[1906,152,19.1,-56.2,-0.37],"UNI":[1718,85,23.5,-15.0,-0.176],"BCH":[1651,54,33.3,9.8,0.181],"ADA":[1634,24,37.5,6.0,0.252],"SOL":[1610,105,24.8,-18.6,-0.177],"AAVE":[1473,32,3.1,-28.5,-0.891],"WLD":[1434,10,0.0,-10.0,-1.0],"ETC":[1281,130,26.2,-16.3,-0.126],"FIL":[1183,64,32.8,1.7,0.027],"TRX":[1166,25,20.0,-9.9,-0.395],"DOGE":[1163,95,26.3,-17.0,-0.179],"LTC":[1159,93,38.7,27.0,0.29],"SUI":[1079,69,30.4,0.2,0.003],"ARB":[1049,9,66.7,12.0,1.333],"BNB":[1011,61,39.3,15.8,0.258],"NEAR":[1002,108,37.0,32.8,0.304],"ATOM":[925,77,33.8,10.7,0.139],"XLM":[914,18,38.9,6.5,0.361],"APT":[669,15,0.0,-15.0,-1.0],"ICP":[608,47,25.5,-5.7,-0.122],"HBAR":[540,67,28.4,0.4,0.007],"INJ":[405,29,34.5,3.1,0.107],"SEI":[22,0,0.0,0.0,null],"TIA":[17,1,0.0,-1.0,-1.0],"LDO":[14,0,0.0,0.0,null],"JUP":[2,0,0.0,0.0,null]},"P-MKT":{"ETH":[316,298,63.4,75.9,0.255],"BTC":[266,256,73.4,121.7,0.475],"XRP":[84,81,70.4,94.4,1.165],"LINK":[82,79,65.8,31.5,0.399],"SOL":[75,73,56.2,2.8,0.039],"LTC":[60,54,75.9,18.7,0.347],"BNB":[59,57,71.9,23.0,0.403],"SUI":[55,55,89.1,81.2,1.476],"DOGE":[54,51,100.0,34.4,0.675],"ADA":[46,46,91.3,53.1,1.155],"TRX":[35,23,82.6,3.6,0.156],"AVAX":[32,31,64.5,17.5,0.565],"NEAR":[23,23,73.9,27.0,1.175],"XLM":[14,14,92.9,8.4,0.604],"FIL":[4,4,50.0,2.8,0.707],"WLD":[4,4,50.0,3.2,0.795],"ETC":[4,4,0.0,0.0,0.0],"UNI":[3,3,0.0,0.0,0.0],"ICP":[2,2,0.0,0.0,0.0],"ATOM":[2,2,0.0,0.0,0.0],"APT":[1,1,0.0,0.0,0.0]},"M2":{"LTC":[129,23,34.8,2.4,0.105],"AXS":[115,17,17.6,-8.2,-0.483],"BNB":[113,13,7.7,-9.5,-0.731],"AAVE":[106,37,48.6,23.6,0.638],"NEAR":[106,15,33.3,2.5,0.167],"APT":[103,4,0.0,-4.0,-1.0],"ICP":[84,10,30.0,-1.1,-0.107],"RUNE":[81,9,44.4,0.6,0.071],"AVAX":[75,17,23.5,-5.7,-0.335],"LINK":[75,24,25.0,-4.8,-0.199],"DOT":[75,2,0.0,-2.0,-1.0],"BCH":[63,33,30.3,2.0,0.061],"GRT":[63,1,0.0,-1.0,-1.0],"ATOM":[62,5,60.0,3.3,0.67],"TIA":[58,8,37.5,2.5,0.312],"ORDI":[57,23,26.1,-6.9,-0.301],"LDO":[56,14,50.0,6.4,0.457],"VET":[55,2,100.0,2.7,1.373],"SOL":[54,29,3.4,-25.5,-0.879],"ETH":[53,26,38.5,9.0,0.346],"GALA":[53,6,33.3,1.0,0.167],"ADA":[53,9,55.6,8.5,0.944],"INJ":[52,28,25.0,-5.8,-0.206],"OP":[51,2,0.0,-2.0,-1.0],"UNI":[50,21,23.8,-3.5,-0.167],"DOGE":[49,0,0.0,0.0,null],"BTC":[47,19,21.1,-5.0,-0.263],"HBAR":[42,0,0.0,0.0,null],"ARB":[39,12,33.3,2.0,0.167],"IMX":[31,7,0.0,-7.0,-1.0],"SEI":[31,12,0.0,-12.0,-1.0],"WLD":[28,6,0.0,-6.0,-1.0],"MANA":[27,5,0.0,-5.0,-1.0],"ETC":[25,13,30.8,1.0,0.077],"JUP":[25,8,0.0,-8.0,-1.0],"DYDX":[25,1,0.0,-1.0,-1.0],"PYTH":[22,9,33.3,1.5,0.167],"ALGO":[20,6,16.7,-2.5,-0.417],"FIL":[14,6,16.7,-2.5,-0.417],"SAND":[12,4,0.0,-4.0,-1.0],"SUI":[11,5,0.0,-5.0,-1.0],"XLM":[11,2,0.0,-2.0,-1.0],"XRP":[5,0,0.0,0.0,null]},"M3":{"LINK":[234,20,15.0,-12.0,-0.598],"LTC":[233,22,45.5,7.8,0.356],"BNB":[232,25,44.0,4.2,0.166],"FIL":[229,23,47.8,11.9,0.516],"AAVE":[223,16,37.5,1.8,0.112],"TIA":[222,31,45.2,18.0,0.581],"MANA":[220,12,41.7,5.5,0.458],"DOGE":[219,27,40.7,0.2,0.007],"BTC":[217,31,64.5,-3.0,-0.095],"NEAR":[213,20,35.0,-1.0,-0.05],"UNI":[212,26,34.6,5.5,0.211],"ETH":[209,34,58.8,14.7,0.432],"INJ":[209,34,38.2,5.9,0.175],"XRP":[208,21,28.6,-0.3,-0.015],"BCH":[207,29,41.4,3.6,0.123],"AVAX":[206,31,22.6,-8.2,-0.263],"APT":[203,48,39.6,14.8,0.308],"IMX":[203,12,25.0,-1.8,-0.154],"ADA":[201,14,50.0,8.8,0.63],"SEI":[201,42,26.2,-6.6,-0.156],"ATOM":[197,24,25.0,-8.9,-0.369],"SAND":[197,34,29.4,1.4,0.041],"GRT":[197,20,25.0,-7.0,-0.349],"GALA":[195,23,39.1,3.6,0.158],"VET":[195,38,31.6,-7.6,-0.2],"TRX":[194,49,63.3,2.9,0.06],"ARB":[192,49,38.8,15.4,0.315],"SUI":[192,43,37.2,0.1,0.004],"RUNE":[189,41,46.3,8.1,0.198],"AXS":[180,19,0.0,-19.0,-1.0],"DYDX":[178,27,18.5,-9.5,-0.352],"OP":[177,24,54.2,19.1,0.796],"PYTH":[177,23,26.1,-6.6,-0.285],"SOL":[176,42,28.6,-11.6,-0.275],"WLD":[174,34,52.9,20.7,0.609],"LDO":[172,39,46.2,9.4,0.24],"ICP":[172,31,22.6,-13.5,-0.435],"XLM":[169,39,30.8,-10.1,-0.259],"HBAR":[169,35,45.7,6.1,0.176],"ETC":[165,30,40.0,-6.5,-0.218],"DOT":[164,20,15.0,-10.6,-0.531],"ORDI":[163,29,27.6,-5.2,-0.179],"JUP":[154,53,32.1,6.7,0.127],"ALGO":[136,24,29.2,-1.7,-0.072]},"M6":{"BTC":[28,3,0.0,-3.0,-1.0],"SOL":[26,20,15.0,-9.5,-0.475],"BCH":[21,16,25.0,-2.0,-0.125],"LTC":[21,2,100.0,5.0,2.5],"AAVE":[20,10,50.0,7.5,0.75],"INJ":[19,16,31.2,1.5,0.094],"UNI":[19,17,29.4,0.5,0.029],"ORDI":[18,16,12.5,-9.0,-0.562],"LINK":[17,15,6.7,-11.5,-0.767],"BNB":[16,1,0.0,-1.0,-1.0],"ETH":[16,4,50.0,3.0,0.75],"ETC":[16,12,41.7,5.5,0.458],"AVAX":[14,9,0.0,-9.0,-1.0]}},"genmap":{"G1":["fvg","TRIAD-A","P-MKT"],"G2":["sweep","TRIAD-A","P-MKT"],"G3":["order_block","M3",null],"G4":["bos","M2",null],"G5":["choch","M2",null],"G6":["momentum","M6",null]},"idx":["BTC","ETH","XRP","LINK","AVAX","SOL","SUI","ETC","BCH","ADA","XLM","UNI","AAVE","NEAR","ATOM","FIL","LTC","TRX","DOGE","WLD","BNB","ARB","ICP","APT","HBAR","INJ","TIA","LDO","SEI","JUP","ALGO","AXS","DOT","DYDX","EOS","GALA","GRT","IMX","MANA","OP","ORDI","PYTH","RUNE","SAND","VET"]}"""

/** Parses MX once and aggregates cohorts exactly as the doc `agg()` does. */
object SuiteMx {
    private val root: JsonObject = Json.parseToJsonElement(MX_JSON).jsonObject
    val idx: List<String> = root["idx"]!!.jsonArray.map { it.jsonPrimitive.content }
    private val cohorts: JsonObject = root["cohorts"]!!.jsonObject
    // gen -> [label, shadowCohort, paperCohort?]
    val genmap: Map<String, List<String?>> = root["genmap"]!!.jsonObject.mapValues { (_, v) ->
        v.jsonArray.map { (it as? JsonPrimitive)?.contentOrNull }
    }

    /** One symbol's raw row in a cohort → [n, res, wr, net, ev] (null if absent). */
    fun symRow(coh: String?, sym: String): List<Double?>? {
        if (coh == null) return null
        val d = cohorts[coh] as? JsonObject ?: return null
        val a = d[sym] as? JsonArray ?: return null
        return a.map { (it as? JsonPrimitive)?.doubleOrNull }
    }

    /** Fold a cohort over all symbols → Agg (null if the cohort is absent / paper is null). */
    fun agg(coh: String?): Agg? {
        if (coh == null) return null
        val d = cohorts[coh] as? JsonObject ?: return null
        var n = 0.0; var res = 0.0; var wsum = 0.0; var net = 0.0
        for (s in idx) {
            val t = (d[s] as? JsonArray) ?: continue
            val cn = t[0].dbl(); val cres = t[1].dbl(); val cwr = t[2].dbl(); val cnet = t[3].dbl()
            n += cn; res += cres; wsum += cwr / 100.0 * cres; net += cnet
        }
        if (res == 0.0) return Agg(n.toInt(), 0, null, 0.0, null)
        return Agg(n.toInt(), res.toInt(), round1(100 * wsum / res), round1(net), round3(net / res))
    }

    private fun kotlinx.serialization.json.JsonElement.dbl(): Double =
        (this as? JsonPrimitive)?.doubleOrNull ?: 0.0
}

private fun round1(x: Double) = kotlin.math.round(x * 10) / 10
private fun round3(x: Double) = kotlin.math.round(x * 1000) / 1000

// ── the saved-experiment store — one source feeding both the Lab books and the Tables view ──────────
@Serializable
data class AggS(val n: Int, val res: Int, val wr: Double? = null, val net: Double, val ev: Double? = null)

@Serializable
data class SavedLab(
    val id: String,
    val composition: String,
    val savedUtc: String,
    val known: Boolean,
    val paper: AggS?,   // WITH LLM (accepted)
    val shadow: AggS?,  // WITHOUT LLM (incl rejected)
    val shadowCoh: String? = null, // cohort names — the per-symbol matrix is recomputed from these
    val paperCoh: String? = null,
)

fun Agg.toS() = AggS(n, res, wr, net, ev)

/**
 * A process-wide, device-persisted registry of saved lab experiments — the doc's `LABS` + its
 * "this device keeps a local copy" (window.storage). SAVE appends here (and also files a
 * propose_action for the authoritative registry); both the Lab books and the Tables view read it.
 */
object LabStore {
    private const val PREFS = "triad_suite"
    private const val KEY = "saved_labs"
    private val JSON = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    val saved = mutableStateListOf<SavedLab>()
    private var loaded = false

    fun load(ctx: Context) {
        if (loaded) return
        loaded = true
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return
        runCatching { JSON.decodeFromString<List<SavedLab>>(raw) }.getOrNull()?.let {
            saved.clear(); saved.addAll(it)
        }
    }

    fun add(ctx: Context, lab: SavedLab) {
        saved.add(0, lab)
        runCatching {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY, JSON.encodeToString(saved.toList())).apply()
        }
    }
}
