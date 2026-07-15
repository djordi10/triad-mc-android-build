package agentic.triad.missioncontrol.ui.components

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * JSON reading + formatting helpers for the LIVE-wired views. A missing key degrades to an honest
 * em-dash (the no-nulls law, carried to the UI) — never a fabricated zero. These mirror the web
 * dashboard's field access so the tool-map in each wiring doc ports directly.
 */

/** Format a number to [dp] decimals, or an em-dash when null (honest unavailable). */
fun fmt(v: Double?, dp: Int = 1): String = v?.let { String.format("%.${dp}f", it) } ?: "—"

/** Field access tolerant of a null object — `obj.field("k")` returns the field or null. */
fun JsonObject?.field(key: String): JsonElement? = this?.get(key)

/** Read a field as Double (null when absent/non-numeric). */
fun JsonObject?.num(key: String): Double? =
    (this?.get(key) as? JsonPrimitive)?.content?.toDoubleOrNull()

/** Read a field as Int (null when absent/non-numeric). */
fun JsonObject?.int(key: String): Int? = num(key)?.toInt()

/** Read a field as a display string (em-dash default when absent). */
fun JsonObject?.text(key: String, default: String = "—"): String =
    (this?.get(key) as? JsonPrimitive)?.content ?: default

/** Read a field as Boolean (false when absent). */
fun JsonObject?.bool(key: String): Boolean =
    (this?.get(key) as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false

/** Read a nested object field. */
fun JsonObject?.obj(key: String): JsonObject? = this?.get(key) as? JsonObject

/** Read an array field (empty when absent). */
fun JsonObject?.arr(key: String): JsonArray = this?.get(key) as? JsonArray ?: JsonArray(emptyList())

/** Any JSON scalar as a display string, em-dash when absent/null. */
fun JsonElement?.str(default: String = "—"): String = when (this) {
    null, JsonNull -> default
    is JsonPrimitive -> content
    else -> toString()
}

/** A JSON value (array) as a list of objects — the common "rows" case. Non-objects dropped. */
fun JsonElement?.rows(): List<JsonObject> =
    (this as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()

/** A JSON value as a raw element list (arrays of scalars or pairs, e.g. capture_top `[[k,n],…]`). */
fun JsonElement?.list(): List<JsonElement> = (this as? JsonArray) ?: emptyList()

/** Entries of a nested object as (key, number) pairs — for the by-check / miss bar panels. */
fun JsonObject?.numEntries(key: String): List<Pair<String, Double>> =
    obj(key)?.entries?.map { it.key to ((it.value as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0) }
        ?: emptyList()

/** Sum the numeric values of a nested object (⇔ web `Object.values(x).reduce(+)`). */
fun JsonObject?.sumValues(key: String): Int =
    obj(key)?.values?.sumOf { (it as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0 }?.toInt() ?: 0

/**
 * Crash-proof derive: run a view's model derivation and, if a malformed live payload makes any inline
 * law throw (indexing, casts, numeric conversion, division, …), degrade to [fallback] instead of
 * throwing out of composition and blanking the whole screen. This is the blank-screen guard applied
 * across every view (mirrors the TopologyScreen fix): the derive is fallible, the screen is not.
 */
inline fun <T> guardDerive(fallback: T, derive: () -> T): T =
    try { derive() } catch (_: Throwable) { fallback }
