package com.ctrip.framework.apollo.portal.component.config;

import com.ctrip.framework.apollo.common.config.RefreshableConfig;
import com.ctrip.framework.apollo.portal.service.PortalDBPropertySource;
import com.ctrip.framework.apollo.portal.service.SystemRoleManagerService;
import com.google.common.collect.Lists;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class handled security related configs for portalConfig class
 */
@Component
public class SecurityConfig extends RefreshableConfig {

    private static final List<String> DEFAULT_USER_PASSWORD_NOT_ALLOW_LIST = Arrays.asList(
            "111", "222", "333", "444", "555", "666", "777", "888", "999", "000",
            "001122", "112233", "223344", "334455", "445566", "556677", "667788", "778899", "889900",
            "009988", "998877", "887766", "776655", "665544", "554433", "443322", "332211", "221100",
            "0123", "1234", "2345", "3456", "4567", "5678", "6789", "7890",
            "0987", "9876", "8765", "7654", "6543", "5432", "4321", "3210",
            "1q2w", "2w3e", "3e4r", "5t6y", "abcd", "qwer", "asdf", "zxcv"
    );

    public SecurityConfig(final PortalDBPropertySource portalDBPropertySource) {
        super(portalDBPropertySource);
    }

    public List<String> getUserPasswordNotAllowList() {
        String[] value = getArrayProperty(PortalConfigConstants.AUTH_USER_PASSWORD_NOT_ALLOW_LIST_KEY, null);
        if (value == null || value.length == 0) {
            return DEFAULT_USER_PASSWORD_NOT_ALLOW_LIST;
        }
        return Lists.newArrayList(value);
    }

    public boolean canAppAdminCreatePrivateNamespace() {
        return getBooleanProperty(PortalConfigConstants.ADMIN_PRIVATE_NAMESPACE_SWITCH, true);
    }

    public boolean isCreateApplicationPermissionEnabled() {
        return getBooleanProperty(SystemRoleManagerService.CREATE_APPLICATION_LIMIT_SWITCH_KEY, false);
    }

    public boolean isManageAppMasterPermissionEnabled() {
        return getBooleanProperty(SystemRoleManagerService.MANAGE_APP_MASTER_LIMIT_SWITCH_KEY, false);
    }

    public boolean isConfigViewMemberOnly(String env) {
        String[] configViewMemberOnlyEnvs = getArrayProperty(PortalConfigConstants.CONFIG_VIEW_MEMBER_ONLY_ENVS_KEY, new String[0]);

        for (String memberOnlyEnv : configViewMemberOnlyEnvs) {
            if (memberOnlyEnv.equalsIgnoreCase(env)) {
                return true;
            }
        }

        return false;
    }
}
