package com.ctrip.framework.apollo.util.parser;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringCuttingParserTest {

    private Parsers.StringCuttingParser stringCuttingParser = Parsers.forStringCutting();

    @Test
    public void testParseMilliSeconds() throws Exception {
        String text = "application,demo";
        String[] expected = {"application", "demo"};

        assertEquals(expected, stringCuttingParser.parse(text));
    }
}
