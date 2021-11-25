package com.ctrip.framework.apollo.util.parser;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;


public class StringCuttingParserTest {

    private Parsers.StringCuttingParser stringCuttingParser = Parsers.forStringCutting();

    @Test
    public void testParseStr() throws Exception {
        String text = "application,apollo";
        String[] expected = {"application", "apollo"};

        assertArrayEquals(expected, stringCuttingParser.parse(text));
    }
}
