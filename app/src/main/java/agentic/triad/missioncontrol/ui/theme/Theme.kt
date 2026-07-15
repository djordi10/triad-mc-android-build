package agentic.triad.missioncontrol.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// The TRIAD palette — the exact tokens the web dashboard uses (v5.11), so the two clients read as
// one system. v5.11 facelift: the body is light paper with white cards (matching the web body), and
// the TRIAD brand strip stays dark pine on top.
val Paper = Color(0xFFF7F6F2)
val Card = Color(0xFFFFFFFF)
val Ink = Color(0xFF17251F)
val Ink2 = Color(0xFF51605A)
val Line = Color(0xFFE3E1D9)

val Pine = Color(0xFF0E211A)
val Pine2 = Color(0xFF16352A)
val PineLine = Color(0xFF2C4A3E)      // ghost-button border on the dark strip
val PineInk = Color(0xFFCFE0D8)       // ghost-button text on the dark strip
val PineVer = Color(0xFF7FA393)       // the mono "MISSION CONTROL ·" label

val Emerald = Color(0xFF0F8A5F)       // --em
val EmeraldBright = Color(0xFF59C99A) // the rail brand "AD" accent / .brand em
val EmeraldSoft = Color(0xFFE7F4EE)   // --em-soft
val Amber = Color(0xFFB45309)         // --am
val AmberSoft = Color(0xFFFDF3E5)     // --am-soft
val Red = Color(0xFFB0403A)           // --red
val RedSoft = Color(0xFFF9EBEA)       // --red-soft
val Blue = Color(0xFF1F5F8B)          // --blue
val BlueSoft = Color(0xFFE8F0F6)      // --blue-soft
val Unk = Color(0xFF9AA5A0)           // --unk
val UnkSoft = Color(0xFFECEAE4)       // --unk-soft
val Sev = Color(0xFF7D1D1A)           // --sev

// The web charts' plotting hues (the SVG bars): emerald / amber / red for status, plus a muted
// slate-blue for the "neutral series" bars (the web's #5b7fb5). Mono 10px labels sit under them.
val ChartBlue = Color(0xFF5B7FB5)

// The dark pine band's own palette (the `.stance` verdict banner + `.pill`s + `#rail`). On pine,
// text is off-white and accents are the brighter tints the web uses so they read on a dark field.
val PineText = Color(0xFFDFE9E4)      // .stance said default text (#dfe9e4)
val PineTextDim = Color(0xFFCFE0D8)   // .stance .said body (#cfe0d8)
val PineDivider = Color(0xFF2C4A3E)   // the .word right border / pill borders (#2c4a3e)
val PinePillBg = Color(0xFF12291F)    // .pill default background (#12291f)
val VerdictShadow = Color(0xFF8FD3B4) // .word.shadow / pill.ok value (#8fd3b4)
val VerdictArmed = Color(0xFFFFD08A)  // .word.armed / pill.am value (#ffd08a)
val VerdictHalted = Color(0xFFF0A19B) // .word.halted / pill.bad value (#f0a19b)
val VerdictUnknown = Color(0xFF9FB3AB)// .word.unknown / pill.unk value (#9fb3ab)

// Back-compat alias — earlier code referred to a single "Muted"; it maps to the web's --ink2.
val Muted = Ink2

// The cockpit body is a light desk surface (v5.11) — one scheme, not chased against the system
// theme, so the numbers read the same every time. The dark pine chrome is drawn explicitly where it
// belongs (the per-view brand strip).
private val TriadColors = lightColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    secondary = Pine,
    background = Paper,
    onBackground = Ink,
    surface = Card,
    onSurface = Ink,
    surfaceVariant = UnkSoft,
    onSurfaceVariant = Ink2,
    outline = Line,
    error = Red,
    onError = Color.White,
)

private val TriadType = Typography()  // Material defaults; swap in IBM Plex/Archivo when bundled.

@Composable
fun TriadTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TriadColors, typography = TriadType, content = content)
}
