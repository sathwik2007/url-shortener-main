package com.pm.urlshortenerbackend.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Author: sathwikpillalamarri
 * Date: 9/14/25
 * Project: url-shortener-backend
 */
public class Base62UtilTest {
    @Test
    void testEncodeAndDecode() {
        long value = 125L;
        String encoded = Base62Util.encode(value);
        long decoded = Base62Util.decode(encoded);

        assertEquals(value, decoded, "Decoded value should match the original value");
    }

    @Test
    void testEncodeZero() {
        assertEquals("0", Base62Util.encode(0));
    }

    @Test
    void testDecodeSingleCharacter() {
        assertEquals(0L, Base62Util.decode("0"));
        assertEquals(1L, Base62Util.decode("1"));
        assertEquals(61L, Base62Util.decode("z"));
    }

    @Test
    void testEncodeLargeNumber() {
        long value = Long.MAX_VALUE;
        String encoded = Base62Util.encode(value);
        long decoded = Base62Util.decode(encoded);

        assertEquals(value, decoded);
    }

    @Test
    void testDecodeInvalidInput() {
        assertThrows(StringIndexOutOfBoundsException.class, () -> Base62Util.decode("!"));
    }
}
