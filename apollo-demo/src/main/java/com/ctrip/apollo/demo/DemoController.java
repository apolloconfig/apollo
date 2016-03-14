package com.ctrip.apollo.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RestController
@RequestMapping("/demo")
public class DemoController {
    @Autowired
    private Environment env;

    @Value("${apollo.foo}")
    private String foo;

    @RequestMapping(value = "/config/{configName:.*}", method = RequestMethod.GET)
    public String queryConfig(@PathVariable String configName) {
        return env.getProperty(configName, "undefined");
    }
}
