package com.uid2.shared.encryption;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Uid2Base64UrlCoderTest {
    @Test
    public void testDecryption() {
        String token = "A4AAAAABO8lY6RZgA7tt3NmNJ_Ua--eIkGxLuL8CE-BwaZPIxbpgtdhplCtT_9dOkINUKMod2nX4pNXmVv09g8Rv4DYAQVd7Yp4LHH8LgFvgF3TQxRZPJSPUpKf9l86txoI1PDCDJYkeSvP9j1iqcTq3P32-bX50efO102TEpu30cqQa-UrrJyeJ0QEgnr2U0Zr6FRaHzNW9s-hVu-u6zw1gyJE";
        byte[] expected = new byte[]{3, -128, 0, 0, 0, 1, 59, -55, 88, -23, 22, 96, 3, -69, 109, -36, -39, -115, 39, -11, 26, -5, -25, -120, -112, 108, 75, -72, -65, 2, 19, -32, 112, 105, -109, -56, -59, -70, 96, -75, -40, 105, -108, 43, 83, -1, -41, 78, -112, -125, 84, 40, -54, 29, -38, 117, -8, -92, -43, -26, 86, -3, 61, -125, -60, 111, -32, 54, 0, 65, 87, 123, 98, -98, 11, 28, 127, 11, -128, 91, -32, 23, 116, -48, -59, 22, 79, 37, 35, -44, -92, -89, -3, -105, -50, -83, -58, -126, 53, 60, 48, -125, 37, -119, 30, 74, -13, -3, -113, 88, -86, 113, 58, -73, 63, 125, -66, 109, 126, 116, 121, -13, -75, -45, 100, -60, -90, -19, -12, 114, -92, 26, -7, 74, -21, 39, 39, -119, -47, 1, 32, -98, -67, -108, -47, -102, -6, 21, 22, -121, -52, -43, -67, -77, -24, 85, -69, -21, -70, -49, 13, 96, -56, -111};
        byte[] results = Uid2Base64UrlCoder.decode(token);
        assertEquals(Arrays.toString(results), Arrays.toString(expected));
    }

    @Test
    public void testEncryption() {
        String expected = "A4AAAAABO8lY6RZgA7tt3NmNJ_Ua--eIkGxLuL8CE-BwaZPIxbpgtdhplCtT_9dOkINUKMod2nX4pNXmVv09g8Rv4DYAQVd7Yp4LHH8LgFvgF3TQxRZPJSPUpKf9l86txoI1PDCDJYkeSvP9j1iqcTq3P32-bX50efO102TEpu30cqQa-UrrJyeJ0QEgnr2U0Zr6FRaHzNW9s-hVu-u6zw1gyJE";
        byte[] byteArray = new byte[]{3, -128, 0, 0, 0, 1, 59, -55, 88, -23, 22, 96, 3, -69, 109, -36, -39, -115, 39, -11, 26, -5, -25, -120, -112, 108, 75, -72, -65, 2, 19, -32, 112, 105, -109, -56, -59, -70, 96, -75, -40, 105, -108, 43, 83, -1, -41, 78, -112, -125, 84, 40, -54, 29, -38, 117, -8, -92, -43, -26, 86, -3, 61, -125, -60, 111, -32, 54, 0, 65, 87, 123, 98, -98, 11, 28, 127, 11, -128, 91, -32, 23, 116, -48, -59, 22, 79, 37, 35, -44, -92, -89, -3, -105, -50, -83, -58, -126, 53, 60, 48, -125, 37, -119, 30, 74, -13, -3, -113, 88, -86, 113, 58, -73, 63, 125, -66, 109, 126, 116, 121, -13, -75, -45, 100, -60, -90, -19, -12, 114, -92, 26, -7, 74, -21, 39, 39, -119, -47, 1, 32, -98, -67, -108, -47, -102, -6, 21, 22, -121, -52, -43, -67, -77, -24, 85, -69, -21, -70, -49, 13, 96, -56, -111};
        String results = Uid2Base64UrlCoder.encode(byteArray);
        assertEquals(results, expected);
    }
}
