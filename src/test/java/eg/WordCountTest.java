/*
 * Copyright 2012-2018 Chronicle Map Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eg;

import com.google.common.io.ByteStreams;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.values.Values;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;
import static org.junit.Assert.assertEquals;

public class WordCountTest {

    static String[] words;
    static Map<CharSequence, Integer> expectedMap;

    static {
        // english version of war and peace ->  ascii
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream zippedIS = Objects.requireNonNull(cl.getResourceAsStream("war_and_peace.txt.gz"));
             GZIPInputStream binaryIS = new GZIPInputStream(zippedIS);) {
            String fullText =
                    new String(ByteStreams.toByteArray(binaryIS), UTF_8);
            words = fullText.split("\\s+");
            expectedMap = Arrays.stream(words)
                    .map(CharSequence.class::cast)
                    .collect(groupingBy(
                            Function.identity(),
                            reducing(0, e -> 1, Integer::sum))
                    );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    ///@Ignore("https://github.com/OpenHFT/Chronicle-Map/issues/376")
    @Test
    public void wordCountTest() {
        try (ChronicleMap<CharSequence, IntValue> map = ChronicleMap
                .of(CharSequence.class, IntValue.class)
                .averageKeySize(7) // average word is 7 ascii bytes long (text in english)
                .entries(expectedMap.size())
                .create()) {
            IntValue v = Values.newNativeReference(IntValue.class);

            for (String word : words) {
                try (Closeable ignored = map.acquireContext(word, v)) {
                    v.addValue(1);
                }
            }

            assertEquals(expectedMap.size(), map.size());
            expectedMap.forEach((key, value) ->
                    assertEquals((int) value, map.get(key).getValue())
            );
        }
    }
}