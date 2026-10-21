package app.kontakt.client.rec

import kotlin.math.min

/** Что именно пользователь разрешил скрывать. Каждое значение — отдельный переключатель в настройках. */
data class FilterConfig(
    /** Публикации с меткой рекламы VK (`marked_as_ads`). */
    val hideAds: Boolean = true,
    /** Эвристика мошенничества и «заработка без вложений». */
    val hideScam: Boolean = true,
    /** Снюс, никотиновые паучи, вейпы, казино, букмекеры. */
    val hideRestrictedGoods: Boolean = true,
)

enum class FilterCategory { Ad, Goods, Scam }

/**
 * Эвристический фильтр рекламы и мусора. Это НЕ надёжный детектор: он видит метку рекламы VK,
 * список явно нежелательных товаров и набор признаков мошенничества. Рекламу без метки он может
 * пропустить, а редкие обычные посты — зацепить.
 *
 * Обычные сборы на помощь людям и животным не скрываются: реквизиты, телефон и слова «срочно» в таких
 * постах дают лишь небольшой понижающий приоритет (и то, если нет названия фонда, ИНН или отчёта).
 */
object ContentFilter {
    const val DROP_THRESHOLD = 0.8
    private const val SHOWN_AD_SPAM = 0.4
    private const val HELP_SOFT_CAP = 0.25
    private const val TRUST_CREDIT = 0.6

    data class Verdict(
        val drop: Boolean,
        /** 0..1: используется в ранжировании (штраф), даже если публикация не скрыта. */
        val spam: Double,
        val category: FilterCategory?,
        val detail: String?,
    )

    private val GOODS_PREFIXES = listOf(
        "снюс", "снус", "snus", "никопауч", "никопоуч", "вейп", "vape", "одноразк",
        "казино", "casino", "1xbet", "мелбет", "букмекер",
    )
    private const val GOODS_EXCEPTION_PREFIX = "снусмум" // Снусмумрик из книг о Муми-троллях

    private val GOODS_PHRASES = listOf(
        "электронные сигареты", "электронная сигарета", "никотиновые паучи", "никотиновые пакетики",
        "ставки на спорт",
    )

    private val SCAM_PHRASES = listOf(
        "займ без отказа", "займы без отказа", "кредит без проверок", "заработок без вложений",
        "удвоим ваш", "люксовые копии", "копии брендов", "пробив по",
    )

    private val SPLIT = Regex("[^\\p{L}\\p{Nd}]+")
    private val CARD = Regex("(?<!\\d)(?:\\d[ -]?){15}\\d(?!\\d)")
    private val PHONE = Regex("(?:\\+7|(?<!\\d)8)[\\s(\\-]*\\d{3}")
    private val LINK = Regex("https?://|vk\\.cc/|t\\.me/")

    private val TRANSFER = listOf("на карту", "на карточку", "по номеру телефона", "на сбер", "на тинькофф", "на т-банк")
    private val URGENCY = listOf("срочно", "прямо сейчас", "только сегодня", "последний шанс")
    private val GUARANTEE = listOf("гарантирую", "гарантия", "100%", "без риска")
    private val EARNING = listOf("заработок", "доход от", "рублей в день", "работа на дому")
    private val CONTACT_BAIT = listOf("пишите в лс", "пишите в личку", "в директ", "в whatsapp", "в телеграм")
    private val COMMERCE = listOf("цена", "руб", "₽", "в наличии", "заказ", "доставка", "скидк", "промокод", "оптом")
    private val HELP_WORDS = listOf(
        "приют", "животн", "собак", "кошк", "котен", "котят", "щенк", "операци", "лечени",
        "помощь нужна", "помогите", "сбор средств", "фонд",
    )
    private val TRUST_WORDS = listOf("фонд", "инн", "огрн", "благотворительн", "отчет", "реквизиты")

    fun evaluate(text: String, markedAds: Boolean, config: FilterConfig = FilterConfig()): Verdict {
        if (markedAds) {
            return if (config.hideAds) {
                Verdict(true, 1.0, FilterCategory.Ad, "метка рекламы VK")
            } else {
                Verdict(false, SHOWN_AD_SPAM, FilterCategory.Ad, null)
            }
        }

        val normalized = text.lowercase().replace('ё', 'е')
        val words = normalized.split(SPLIT).filter { it.isNotEmpty() }

        val goodsWord = words.firstOrNull { word ->
            !word.startsWith(GOODS_EXCEPTION_PREFIX) && GOODS_PREFIXES.any { word.startsWith(it) }
        }
        val goods = goodsWord ?: GOODS_PHRASES.firstOrNull { normalized.contains(it) }
        if (goods != null && config.hideRestrictedGoods) {
            return Verdict(true, 1.0, FilterCategory.Goods, goods)
        }

        val scamPhrase = SCAM_PHRASES.firstOrNull { normalized.contains(it) }
        if (scamPhrase != null && config.hideScam) {
            return Verdict(true, 1.0, FilterCategory.Scam, scamPhrase)
        }

        val helpish = HELP_WORDS.any { normalized.contains(it) }
        val hasTrust = TRUST_WORDS.any { normalized.contains(it) }

        // «Сильные» признаки считаются всегда; «мягкие» в постах о помощи не ведут к скрытию.
        var strong = if (scamPhrase != null) 0.6 else 0.0
        var mild = 0.0

        if (CARD.containsMatchIn(normalized)) mild += 0.30
        if (TRANSFER.any { normalized.contains(it) } || normalized.contains("переведите")) mild += 0.20
        mild += min(URGENCY.count { normalized.contains(it) } * 0.15, 0.30)
        if (PHONE.containsMatchIn(normalized)) mild += 0.15
        mild += min(COMMERCE.count { normalized.contains(it) } * 0.10, 0.40)
        val bait = CONTACT_BAIT.any { normalized.contains(it) }
        if (bait) mild += 0.15
        if (LINK.findAll(normalized).count() >= 3) mild += 0.20
        if (normalized.count { it == '#' } >= 8) mild += 0.20
        val letters = text.filter { it.isLetter() }
        if (letters.length >= 40 && letters.count { it.isUpperCase() } > letters.length * 0.6) mild += 0.15

        if (GUARANTEE.any { normalized.contains(it) }) strong += 0.30
        val giveaway = normalized.contains("розыгрыш") || normalized.contains("вы выиграли")
        if (giveaway && (normalized.contains("переведите") || normalized.contains("оплатите") || normalized.contains("комисси"))) {
            strong += 0.60
        }
        val earning = EARNING.any { normalized.contains(it) }
        if (earning) strong += 0.40
        if (earning && bait) strong += 0.20

        if (helpish) {
            val credited = if (hasTrust) (mild - TRUST_CREDIT).coerceAtLeast(0.0) else mild
            val spam = (strong + min(credited, HELP_SOFT_CAP)).coerceIn(0.0, 1.0)
            return finish(strong, spam, config)
        }
        val spam = (strong + mild).coerceIn(0.0, 1.0)
        return finish(strong + mild, spam, config)
    }

    private fun finish(dropScore: Double, spam: Double, config: FilterConfig): Verdict =
        if (config.hideScam && dropScore >= DROP_THRESHOLD) {
            Verdict(true, spam.coerceAtLeast(DROP_THRESHOLD), FilterCategory.Scam, "признаки мошенничества или скрытой рекламы")
        } else {
            Verdict(false, spam, null, null)
        }
}
