package com.ctrip.framework.apollo.portal.environment;

import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wxq
 */
public class Env {
    
    private static final Logger logger = LoggerFactory.getLogger(Env.class);

    // name of environment, cannot be null
    private final String name;

    // use to cache Env
    private static final Map<String, Env> STRING_ENV_MAP = new ConcurrentHashMap<>();

    // default environments
    public static final Env LOCAL = addEnvironment("LOCAL");
    public static final Env DEV = addEnvironment("DEV");
    public static final Env FWS = addEnvironment("FWS");
    public static final Env FAT = addEnvironment("FAT");
    public static final Env UAT = addEnvironment("UAT");
    public static final Env LPT = addEnvironment("LPT");
    public static final Env PRO = addEnvironment("PRO");
    public static final Env TOOLS = addEnvironment("TOOLS");
    public static final Env UNKNOWN = addEnvironment("UNKNOWN");

    /**
     * Cannot create by other
     * @param name
     */
    private Env(String name) {
        this.name = name;
    }

    /**
     * add some change to environment name
     * trim and to upper
     * @param environmentName
     * @return
     */
    private static String getWellFormName(String environmentName) {
        return environmentName.trim().toUpperCase();
    }

    /**
     * logic same as
     * @see com.ctrip.framework.apollo.core.enums.EnvUtils transformEnv
     * @param envName
     * @return
     */
    public static Env transformEnv(String envName) {
        if(Env.exists(envName)) {
            return Env.valueOf(envName);
        }
        if (StringUtils.isBlank(envName)) {
            return Env.UNKNOWN;
        }
        switch (envName.trim().toUpperCase()) {
            case "LPT":
                return Env.LPT;
            case "FAT":
            case "FWS":
                return Env.FAT;
            case "UAT":
                return Env.UAT;
            case "PRO":
            case "PROD": //just in case
                return Env.PRO;
            case "DEV":
                return Env.DEV;
            case "LOCAL":
                return Env.LOCAL;
            case "TOOLS":
                return Env.TOOLS;
            default:
                return Env.UNKNOWN;
        }
    }
    
    /**
     * a environment name exist or not
     * @param name
     * @return
     */
    public static boolean exists(String name) {
        name = getWellFormName(name);
        return STRING_ENV_MAP.containsKey(name);
    }

    /**
     * add an environment
     * @param name
     * @return
     */
    public static Env addEnvironment(String name) {
        if (StringUtils.isBlank(name)) {
            throw new RuntimeException("Cannot add a blank environment: " + "[" + name + "]");
        }

        name = getWellFormName(name);
        if(STRING_ENV_MAP.containsKey(name)) {
            // has been existed
            logger.debug("{} already exists.", name);
        } else {
            // not existed
            STRING_ENV_MAP.put(name, new Env(name));
        }
        return STRING_ENV_MAP.get(name);
    }

    /**
     * replace valueOf in enum
     * But what would happened if environment not exist?
     *
     * @param name
     * @throws IllegalArgumentException if this existed environment has no Env with the specified name
     * @return
     */
    public static Env valueOf(String name) {
        name = getWellFormName(name);
        if(exists(name)) {
            return STRING_ENV_MAP.get(name);
        } else {
            throw new IllegalArgumentException(name + " not exist");
        }
    }

    /**
     * Please use {@code Env.valueOf} instead this method
     * @param env
     * @return
     */
    @Deprecated
    public static Env fromString(String env) {
        Env environment = transformEnv(env);
        Preconditions.checkArgument(environment != UNKNOWN, String.format("Env %s is invalid", env));
        return environment;
    }

    /**
     * Not just name in Env,
     * the address of Env must be same,
     * or it will throw {@code RuntimeException}
     * @param o
     * @throws RuntimeException When same name but different address
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Env env = (Env) o;
        if(getName().equals(env.getName())) {
            throw new RuntimeException(getName() + " is same environment name, but their Env not same");
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }

    /**
     * a Env convert to string, ie its name.
     * @return
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Backward compatibility with enum's name method
     * @return
     */
    @Deprecated
    public String name() {
        return name;
    }

    public String getName() {
        return name;
    }

    /**
     * conversion key from {@link String} to {@link Env}
     * @param metaServerAddresses key is environment, value is environment's meta server address
     * @return relationship between {@link Env} and meta server address
     */
    static Map<Env, String> transformToEnvMap(Map<String, String> metaServerAddresses) {
        // add to domain
        Map<Env, String> map = new ConcurrentHashMap<>();
        for(Map.Entry<String, String> entry : metaServerAddresses.entrySet()) {
            // add new environment
            Env env = Env.addEnvironment(entry.getKey());
            // get meta server address value
            String value = entry.getValue();
            // put pair (Env, meta server address)
            map.put(env, value);
        }
        return map;
    }
}
