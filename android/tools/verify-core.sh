#!/bin/bash
# Проверка «чистого» ядра настоящей компиляцией и юнит-тестами БЕЗ Android SDK и сети.
# Использует компилятор Kotlin из дистрибутива Gradle (/opt/gradle-8.14.3/lib).
# Ограничения: нет плагина kotlinx.serialization (поэтому RecommenderStore, VkResponseParser, UI и сетевой
# слой сюда не входят), корутины 1.6.4 вместо 1.10.2, нет kotlinx-coroutines-test (PaginatorTest не запускается).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
L=/opt/gradle-8.14.3/lib
OUT="${1:-/tmp/kontakt-verify}"
rm -rf "$OUT"; mkdir -p "$OUT/main" "$OUT/test"
M="$ROOT/app/src/main/kotlin/app/vgradesk/client"
T="$ROOT/app/src/test/kotlin/app/vgradesk/client"
DEPS="$L/kotlin-stdlib-2.0.21.jar:$L/kotlinx-coroutines-core-jvm-1.6.4.jar:$L/kotlinx-serialization-core-jvm-1.6.2.jar:$L/kotlinx-serialization-json-jvm-1.6.2.jar:$L/junit-4.13.2.jar:$L/hamcrest-core-1.3.jar"

MAIN_FILES=(
  "$M/net/VkError.kt" "$M/net/Dto.kt"
  "$M/data/FeedPost.kt" "$M/data/FeedMapping.kt" "$M/data/ForYouRepository.kt" "$M/core/Paging.kt"
  "$M/core/Diagnostics.kt" "$M/core/NavConfig.kt" "$M/core/Appearance.kt" "$M/ui/theme/ColorMath.kt"
  "$M/rec/Tokenizer.kt" "$M/rec/InterestProfile.kt" "$M/rec/Ranker.kt" "$M/rec/Signals.kt"
  "$M/rec/ContentFilter.kt" "$M/rec/Recommender.kt" "$M/rec/RecommenderStorage.kt"
)
TEST_FILES=(
  "$T/TestSupport.kt" "$T/ContentFilterTest.kt" "$T/TokenizerTest.kt" "$T/SignalsTest.kt"
  "$T/NavConfigTest.kt" "$T/DiagnosticsTest.kt" "$T/RecommenderTest.kt" "$T/FeedMappingTest.kt" "$T/ForYouRepositoryTest.kt" "$T/ColorMathTest.kt"
)
echo "== компиляция основного кода"
bash "$ROOT/tools/kc.sh" -jvm-target 17 -cp "$DEPS" -d "$OUT/main" "${MAIN_FILES[@]}" 2>&1 | grep -v 'JAVA_TOOL_OPTIONS' || true
test -n "$(ls "$OUT/main" 2>/dev/null)" || { echo "ОШИБКА: основной код не скомпилировался"; exit 1; }
echo "== компиляция тестов"
bash "$ROOT/tools/kc.sh" -jvm-target 17 -cp "$DEPS:$OUT/main" -d "$OUT/test" "${TEST_FILES[@]}" 2>&1 | grep -v 'JAVA_TOOL_OPTIONS' || true
test -n "$(ls "$OUT/test" 2>/dev/null)" || { echo "ОШИБКА: тесты не скомпилировались"; exit 1; }
echo "== запуск тестов"
CLASSES=$(cd "$OUT/test" && find . -name '*Test.class' | sed 's#^\./##; s#\.class$##; s#/#.#g' | sort | tr '\n' ' ')
java -cp "$DEPS:$OUT/main:$OUT/test" org.junit.runner.JUnitCore $CLASSES 2>&1 | grep -v 'JAVA_TOOL_OPTIONS'
