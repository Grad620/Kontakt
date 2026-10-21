package app.vgradesk.client

import app.vgradesk.client.rec.ContentFilter
import app.vgradesk.client.rec.FilterCategory
import app.vgradesk.client.rec.FilterConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentFilterTest {

    @Test
    fun postMarkedAsAdsIsDroppedAndCountedAsAd() {
        val v = ContentFilter.evaluate("Обычный текст", markedAds = true)
        assertTrue(v.drop)
        assertEquals(FilterCategory.Ad, v.category)
    }

    @Test
    fun markedAdsAreKeptButPenalizedWhenUserTurnsFilterOff() {
        val v = ContentFilter.evaluate("Обычный текст", markedAds = true, config = FilterConfig(hideAds = false))
        assertFalse(v.drop)
        assertTrue(v.spam > 0.0)
    }

    @Test
    fun snusAndVapesAreDropped() {
        listOf(
            "Снюс оптом, доставка по городу",
            "Никопауч новых вкусов в наличии",
            "Одноразки и вейпы со скидкой",
            "Лучшее онлайн казино ждёт вас",
            "Ставки на спорт с бонусом",
        ).forEach { text ->
            val v = ContentFilter.evaluate(text, markedAds = false)
            assertTrue("должно быть скрыто: $text", v.drop)
            assertEquals(FilterCategory.Goods, v.category)
        }
    }

    @Test
    fun snusmumrikFromMoominBooksIsNotAffected() {
        val v = ContentFilter.evaluate("Снусмумрик играет на губной гармошке у палатки", markedAds = false)
        assertFalse(v.drop)
    }

    @Test
    fun restrictedGoodsStayVisibleWhenUserTurnsFilterOff() {
        val v = ContentFilter.evaluate("Снюс оптом", markedAds = false, config = FilterConfig(hideRestrictedGoods = false))
        assertFalse(v.drop)
    }

    @Test
    fun obviousScamIsDropped() {
        val v = ContentFilter.evaluate(
            "СРОЧНО! Заработок без вложений, гарантирую 100% доход, пишите в лс",
            markedAds = false,
        )
        assertTrue(v.drop)
        assertEquals(FilterCategory.Scam, v.category)
    }

    @Test
    fun fakeGiveawayWithPaymentIsDropped() {
        val v = ContentFilter.evaluate(
            "Вы выиграли розыгрыш! Для получения приза оплатите комиссию на карту 2200 1234 5678 9012",
            markedAds = false,
        )
        assertTrue(v.drop)
    }

    @Test
    fun ordinaryHelpPostForAnimalWithCardAndPhoneIsNeverDropped() {
        val v = ContentFilter.evaluate(
            "Срочно нужна помощь! Котёнок попал под машину, нужна операция. Переведите на карту " +
                "2202 2002 3456 7890 или по номеру телефона +7 900 123-45-67. Любая сумма важна, пишите в лс.",
            markedAds = false,
        )
        assertFalse(v.drop)
        assertTrue("небольшой штраф допустим", v.spam <= 0.25)
    }

    @Test
    fun helpPostForPersonWithFoundationDetailsGetsNoPenalty() {
        val v = ContentFilter.evaluate(
            "Сбор средств на лечение. Благотворительный фонд, ИНН 7700000000, отчёт о расходах опубликован. " +
                "Реквизиты: карта 2202 2002 3456 7890. Срочно!",
            markedAds = false,
        )
        assertFalse(v.drop)
        assertEquals(0.0, v.spam, 1e-9)
    }

    @Test
    fun ordinaryTextHasNoSpamScore() {
        val v = ContentFilter.evaluate("Вчера гуляли по набережной, закат был потрясающий.", markedAds = false)
        assertFalse(v.drop)
        assertEquals(0.0, v.spam, 1e-9)
        assertNull(v.category)
    }

    @Test
    fun shopPostWithPriceAndDeliveryIsNotDroppedAlone() {
        val v = ContentFilter.evaluate("Свежий хлеб, цена 80 руб, доставка по району", markedAds = false)
        assertFalse(v.drop)
    }

    @Test
    fun scamFilterCanBeTurnedOff() {
        val v = ContentFilter.evaluate(
            "Заработок без вложений, гарантирую доход, пишите в лс",
            markedAds = false,
            config = FilterConfig(hideScam = false),
        )
        assertFalse(v.drop)
        assertTrue(v.spam > 0.5)
    }
}
