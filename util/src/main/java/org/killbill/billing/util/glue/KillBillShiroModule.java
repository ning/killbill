/*
 * Copyright 2010-2013 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.util.glue;

import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.guice.ShiroModule;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.session.mgt.SessionManager;
import org.skife.config.ConfigSource;
import org.skife.config.ConfigurationObjectFactory;

import org.killbill.billing.util.config.RbacConfig;
import org.killbill.billing.util.security.shiro.dao.JDBCSessionDao;
import org.killbill.billing.util.security.shiro.realm.KillBillJndiLdapRealm;

import com.google.inject.binder.AnnotatedBindingBuilder;

// For Kill Bill library only.
// See org.killbill.billing.server.modules.KillBillShiroWebModule for Kill Bill server.
public class KillBillShiroModule extends ShiroModule {

    public static final String KILLBILL_LDAP_PROPERTY = "killbill.server.ldap";
    public static final String KILLBILL_RBAC_PROPERTY = "killbill.server.rbac";

    public static boolean isLDAPEnabled() {
        return Boolean.parseBoolean(System.getProperty(KILLBILL_LDAP_PROPERTY, "false"));
    }

    public static boolean isRBACEnabled() {
        return Boolean.parseBoolean(System.getProperty(KILLBILL_RBAC_PROPERTY, "true"));
    }

    private final ConfigSource configSource;

    public KillBillShiroModule(final ConfigSource configSource) {
        this.configSource = configSource;
    }

    protected void configureShiro() {
        final RbacConfig config = new ConfigurationObjectFactory(configSource).build(RbacConfig.class);
        bind(RbacConfig.class).toInstance(config);

        bindRealm().toProvider(IniRealmProvider.class).asEagerSingleton();

        if (isLDAPEnabled()) {
            bindRealm().to(KillBillJndiLdapRealm.class).asEagerSingleton();
        }
    }

    @Override
    protected void bindSecurityManager(final AnnotatedBindingBuilder<? super SecurityManager> bind) {
        super.bindSecurityManager(bind);

        // Magic provider to configure the cache manager
        bind(CacheManager.class).toProvider(EhCacheManagerProvider.class).asEagerSingleton();
    }

    @Override
    protected void bindSessionManager(final AnnotatedBindingBuilder<SessionManager> bind) {
        bind.to(DefaultSessionManager.class).asEagerSingleton();

        // Magic provider to configure the session DAO
        bind(JDBCSessionDao.class).toProvider(JDBCSessionDaoProvider.class).asEagerSingleton();
    }
}
