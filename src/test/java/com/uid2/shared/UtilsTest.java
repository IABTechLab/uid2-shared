package com.uid2.shared;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UtilsTest {

    @Test public void upperBound_EmptyList() {
        List<Integer> list = Arrays.asList();
        assertEquals(0, Utils.upperBound(list, 5, (value, item) -> value < item));
    }

    @Test public void upperBound_SingleElement_Less() {
        List<Integer> list = Arrays.asList(4);
        assertEquals(1, Utils.upperBound(list, 5, (value, item) -> value < item));
    }

    @Test public void upperBound_SingleElement_Equal() {
        List<Integer> list = Arrays.asList(5);
        assertEquals(1, Utils.upperBound(list, 5, (value, item) -> value < item));
    }

    @Test public void upperBound_SingleElement_Greater() {
        List<Integer> list = Arrays.asList(6);
        assertEquals(0, Utils.upperBound(list, 5, (value, item) -> value < item));
    }

    @Test public void upperBound_MultipleElements_AllLess() {
        List<Integer> list = Arrays.asList(2, 3, 4);
        assertEquals(3, Utils.upperBound(list, 5, (value, item) -> value < item));
    }

    @Test public void upperBound_MultipleElements_AllEqual() {
        List<Integer> list = Arrays.asList(5, 5, 5);
        assertEquals(3, Utils.upperBound(list, 5, (value, item) -> value < item));
    }

    @Test public void upperBound_MultipleElements_AllGreater() {
        List<Integer> list = Arrays.asList(6, 7, 8);
        assertEquals(0, Utils.upperBound(list, 5, (value, item) -> value < item));
    }

    @Test public void upperBound_MultipleElements_WithSingleExactMatch() {
        List<Integer> list = Arrays.asList(3, 4, 5, 6, 7, 8);
        assertEquals(3, Utils.upperBound(list, 5, (value, item) -> value < item));
    }

    @Test public void upperBound_MultipleElements_WithNoExactMatch() {
        List<Integer> list = Arrays.asList(3, 4, 6, 7, 8);
        assertEquals(2, Utils.upperBound(list, 5, (value, item) -> value < item));
    }

    @Test public void upperBound_MultipleElements_WithMultipleExactMatches() {
        List<Integer> list = Arrays.asList(3, 4, 5, 5, 5, 6, 7, 8);
        assertEquals(5, Utils.upperBound(list, 5, (value, item) -> value < item));
    }
}
