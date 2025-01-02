package com.ctrip.framework.apollo.portal.component.config;

import com.ctrip.framework.apollo.common.config.RefreshableConfig;
import com.ctrip.framework.apollo.portal.service.PortalDBPropertySource;
import com.google.common.base.Strings;
import org.springframework.stereotype.Component;

/**
 * This class handled email related configs for portalConfig class
 */
@Component
public class EmailConfig extends RefreshableConfig {

    public EmailConfig(final PortalDBPropertySource portalDBPropertySource) {
        super(portalDBPropertySource);
    }

    public String emailSender() {
        String value = getValue(PortalConfigConstants.EMAIL_SENDER, "");
        return Strings.isNullOrEmpty(value) ? emailConfigUser() : value;
    }

    public String emailConfigHost() {
        return getValue(PortalConfigConstants.EMAIL_CONFIG_HOST, "");
    }

    public String emailConfigPassword() {
        return getValue(PortalConfigConstants.EMAIL_CONFIG_PASSWORD, "");
    }

    public String emailTemplateFramework() {
        return getValue(PortalConfigConstants.EMAIL_TEMPLATE_FRAMEWORK, "");
    }

    public String emailReleaseDiffModuleTemplate() {
        return getValue(PortalConfigConstants.EMAIL_RELEASE_MODULE_DIFF, "");
    }

    public String emailRollbackDiffModuleTemplate() {
        return getValue(PortalConfigConstants.EMAIL_ROLLBACK_MODULE_DIFF, "");
    }

    public String emailGrayRulesModuleTemplate() {
        return getValue(PortalConfigConstants.EMAIL_RELEASE_MODULE_RULES, "");
    }

    public String emailConfigUser() {
        return getValue(PortalConfigConstants.EMAIL_CONFIG_USER, "");
    }

    public boolean isEmailEnabled() {
        return getBooleanProperty(PortalConfigConstants.EMAIL_ENABLED, false);
    }
}
