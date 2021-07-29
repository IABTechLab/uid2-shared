// Copyright (c) 2021 The Trade Desk, Inc
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package com.uid2.shared;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class UtilsTest {

    @Test public void upperBound_EmptyList() {
        List<Integer> list = Arrays.asList();
        Assert.assertEquals(0, Utils.upperBound(list, 5, (value, item) -> value < item));
    }

    @Test public void upperBound_SingleElement_Less() {
        List<Integer> list = Arrays.asList(4);
        Assert.assertEquals(1, Utils.upperBound(list, 5, (value, item) -> value < item));
    }

    @Test public void upperBound_SingleElement_Equal() {
        List<Integer> list = Arrays.asList(5);
        Assert.assertEquals(1, Utils.upperBound(list, 5, (value, item) -> value < item));
    }

    @Test public void upperBound_SingleElement_Greater() {
        List<Integer> list = Arrays.asList(6);
        Assert.assertEquals(0, Utils.upperBound(list, 5, (value, item) -> value < item));
    }

    @Test public void upperBound_MultipleElements_AllLess() {
        List<Integer> list = Arrays.asList(2, 3, 4);
        Assert.assertEquals(3, Utils.upperBound(list, 5, (value, item) -> value < item));
    }

    @Test public void upperBound_MultipleElements_AllEqual() {
        List<Integer> list = Arrays.asList(5, 5, 5);
        Assert.assertEquals(3, Utils.upperBound(list, 5, (value, item) -> value < item));
    }

    @Test public void upperBound_MultipleElements_AllGreater() {
        List<Integer> list = Arrays.asList(6, 7, 8);
        Assert.assertEquals(0, Utils.upperBound(list, 5, (value, item) -> value < item));
    }

    @Test public void upperBound_MultipleElements_WithSingleExactMatch() {
        List<Integer> list = Arrays.asList(3, 4, 5, 6, 7, 8);
        Assert.assertEquals(3, Utils.upperBound(list, 5, (value, item) -> value < item));
    }

    @Test public void upperBound_MultipleElements_WithNoExactMatch() {
        List<Integer> list = Arrays.asList(3, 4, 6, 7, 8);
        Assert.assertEquals(2, Utils.upperBound(list, 5, (value, item) -> value < item));
    }

    @Test public void upperBound_MultipleElements_WithMultipleExactMatches() {
        List<Integer> list = Arrays.asList(3, 4, 5, 5, 5, 6, 7, 8);
        Assert.assertEquals(5, Utils.upperBound(list, 5, (value, item) -> value < item));
    }
}
