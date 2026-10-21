# BUILD_NOTES — Kontakt

## Статус сборок
| Дата | Что | Результат |
|---|---|---|
| — | Ни одной сборки APK не выполнено | **APK НЕ существует** |
| 2026-10-21 | `bash tools/verify-core.sh` (настоящий kotlinc 2.0.21 из Gradle, «чистое» ядро + тесты) | OK, 65 тестов, 0 ошибок |

Android-часть (Compose, OkHttp, DataStore, VK ID SDK, `VkApi`, `VkChunkLoader`, `SettingsStore`, `NetworkMonitor`, все экраны) **ни разу не компилировалась**. Проверена только чтением и статическими проверками: все внутренние импорты `app.vgradesk.client.*` разрешаются в существующие объявления (грубый скрипт; он не ловит ошибки типов).

## Что известно об окружении (контейнер разработки)
- Linux, JDK 21, Gradle 8.14.3 (`/opt/gradle`), Maven; Android SDK отсутствует.
- Сеть до Gradle Plugin Portal, Maven Central, Google Maven, репозиториев VK и npm/PyPI недоступна.
- Внутри Gradle есть kotlin-compiler-embeddable 2.0.21, kotlin-stdlib, kotlinx-coroutines-core 1.6.4, kotlinx-serialization-core/json 1.6.2, junit 4.13.2, hamcrest-core 1.3 — на них собирается проверка «чистого» ядра (`tools/`).
- Плагина kotlinx.serialization для компилятора нет, поэтому классы с `@Serializable` в проверку ядра не входят; `PaginatorTest` и `VkResponseParserTest` (нужны coroutines-test и плагин) пока не запускались.

## Что входит в `tools/verify-core.sh`
Основной код: `net/VkError`, `net/Dto`, `data/{FeedPost,FeedMapping,ForYouRepository}`, `core/{Paging,Diagnostics,NavConfig,Appearance}`, `ui/theme/ColorMath`, `rec/{Tokenizer,InterestProfile,Ranker,Signals,ContentFilter,Recommender,RecommenderStorage}`. Тесты: ContentFilter, Tokenizer, Signals, NavConfig, Diagnostics, Recommender, FeedMapping, ForYouRepository, ColorMath.

## Версии инструментов проекта (по build-файлам, не проверены сборкой)
Kotlin 2.1.21, AGP 8.11.1, Gradle 8.14.3, JDK 17 (CI), compileSdk/targetSdk 35, **minSdk 34**, Compose BOM 2025.06.01, Material 3, Navigation 2.9.0, Lifecycle 2.9.1, DataStore 1.1.7, OkHttp 4.12.0, kotlinx-serialization-json 1.8.1, Coroutines 1.10.2, Coil 2.7.0, VK ID SDK 2.0.0 (**не проверено**, что такая версия есть и классы называются так, как в коде).

## Как получить APK (без компьютера)
1. Создать приватный репозиторий на GitHub (с телефона).
2. Загрузить в корень `kontakt-project.zip` и `.github/workflows/build.yml`.
3. Actions → «Build Kontakt APK» → Run workflow.
4. Скачать артефакт `Kontakt-debug-apk` (внутри `app-debug.apk`). Подробно — в README.md.
Порядок шагов в CI: сначала `assembleDebug` и загрузка APK, потом `testDebugUnitTest`: упавший тест не должен отнять готовый APK.

## Чего ждать от первой сборки
Вероятны ошибки компиляции в Android-части (имена API VK ID SDK, импорты Compose, типы). Нужно скопировать конец лога шага «Сборка debug APK» и исправлять по нему, не переписывая рабочее ядро.
