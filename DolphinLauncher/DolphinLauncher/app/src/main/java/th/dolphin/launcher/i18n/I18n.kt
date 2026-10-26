package th.dolphin.launcher.i18n

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import java.util.Locale

/**
 * ระบบหลายภาษา (ไทย / English)
 *
 * ข้อความในโค้ดเขียนเป็นภาษาไทย แล้วแปลตอนแสดงผลผ่าน [Text] ของแพ็กเกจนี้ (ใช้แทน material3.Text ทั้งแอป)
 *  1) ตรงทั้งข้อความ → ใช้คำแปลนั้น
 *  2) ข้อความที่มีตัวเลข/ค่าแทรก → แทนที่ทีละวลี (วลียาวก่อน) จากตาราง [EnglishTable]
 * ผลลัพธ์ถูกแคชไว้ จึงไม่กระทบความลื่นของหน้าจอ
 */
object I18n {
    /** ภาษาที่ใช้จริง: "th" หรือ "en" */
    @Volatile var lang: String = "th"

    /** ค่าในตั้งค่า: system | th | en */
    fun resolve(setting: String): String = when (setting) {
        "th", "en" -> setting
        else -> if (Locale.getDefault().language == "th") "th" else "en"
    }

    fun dateLocale(): Locale = if (lang == "th") Locale("th", "TH") else Locale.ENGLISH

    private val thai = Regex("[฀-๿]")

    /** วลียาว (หรือมีช่องว่างหัว/ท้าย) — แทนที่ได้ทุกตำแหน่ง */
    private val phrases: List<Pair<String, String>> by lazy {
        EnglishTable.map.entries
            .filter { it.key.length >= 4 || it.key.first().isWhitespace() || it.key.last().isWhitespace() }
            .sortedByDescending { it.key.length }
            .map { it.key to it.value }
    }

    /** คำสั้น — แทนที่เฉพาะเมื่ออยู่ติดตัวเลข/ช่องว่าง เพื่อไม่ให้ไปแทนกลางคำอื่น */
    private val shortWords: List<Pair<Regex, String>> by lazy {
        EnglishTable.map.entries
            .filter { it.key.length < 4 && !it.key.first().isWhitespace() && !it.key.last().isWhitespace() }
            .sortedByDescending { it.key.length }
            .map { Regex("(?<![^0-9\\s(])" + Regex.escape(it.key) + "(?![^\\s0-9.,)·:])") to Regex.escapeReplacement(it.value) }
    }

    private val cache = object : LinkedHashMap<String, String>(512, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 3000
    }

    fun tr(s: String, language: String = lang): String {
        if (language != "en" || !thai.containsMatchIn(s)) return s
        synchronized(cache) { cache[s]?.let { return it } }
        val out = translate(s)
        synchronized(cache) { cache[s] = out }
        return out
    }

    private fun translate(s: String): String {
        EnglishTable.map[s]?.let { return it }
        var r = s
        for ((k, v) in phrases) {
            if (r.contains(k)) r = r.replace(k, v)
        }
        if (thai.containsMatchIn(r)) {
            for ((re, v) in shortWords) r = re.replace(r, v)
        }
        return r
    }
}

/** ภาษาปัจจุบันใน Compose (เปลี่ยนแล้วหน้าจอแปลใหม่ทันที) */
val LocalLang = staticCompositionLocalOf { "th" }

/** ใช้แทน androidx.compose.material3.Text — แปลข้อความตามภาษาที่เลือกก่อนแสดง */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    style: TextStyle = LocalTextStyle.current,
) {
    androidx.compose.material3.Text(
        text = I18n.tr(text, LocalLang.current),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        style = style,
    )
}
