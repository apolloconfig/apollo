package com.ctrip.framework.apollo.portal.component.config;

import com.ctrip.framework.apollo.common.config.RefreshableConfig;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.PortalDBPropertySource;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Set;

/**
 * This class handled environment related configs for portalConfig class
 */
@Component
public class EnvConfig extends RefreshableConfig {

    public EnvConfig(final PortalDBPropertySource portalDBPropertySource) {
        super(portalDBPropertySource);
    }

    public List<Env> portalSupportedEnvs() {
        String[] configurations = getArrayProperty(PortalConfigConstants.APOLLO_PORTAL_ENVS, new String[]{"FAT", "UAT", "PRO"});
        List<Env> envs = Lists.newLinkedList();

        for (String env : configurations) {
            envs.add(Env.addEnvironment(env));
        }

        return envs;
    }

    public int getPerEnvSearchMaxResults() {
        return getIntProperty(PortalConfigConstants.PER_ENV_MAX_RESULTS, 200);
    }

    public Set<Env> emailSupportedEnvs() {
        return getSupportedEnvs(PortalConfigConstants.EMAIL_SUPPORTED_ENVS);
    }

    public Set<Env> webHookSupportedEnvs() {
        return getSupportedEnvs(PortalConfigConstants.WEBHOOK_SUPPORTED_ENVS);
    }

    private Set<Env> getSupportedEnvs(String key) {
        String[] configurations = getArrayProperty(key, null);
        Set<Env> result = Sets.newHashSet();
        if (configurations != null) {
            for (String env : configurations) {
                result.add(Env.valueOf(env));
            }
        }
        return result;
    }

    public Set<Env> publishTipsSupportedEnvs() {
        String[] configurations = getArrayProperty(PortalConfigConstants.NAMESPACE_PUBLISH_TIPS_SUPPORTED_ENVS_KEY, null);

        Set<Env> result = Sets.newHashSet();
        if (configurations == null || configurations.length == 0) {
            return result;
        }

        for (String env : configurations) {
            result.add(Env.valueOf(env));
        }

        return result;
    }

}
