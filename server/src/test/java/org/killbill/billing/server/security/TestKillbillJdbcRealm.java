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

package org.killbill.billing.server.security;

import java.util.UUID;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.support.DelegatingSubject;
import org.killbill.billing.jaxrs.TestJaxrsBase;
import org.killbill.billing.tenant.api.DefaultTenant;
import org.killbill.billing.tenant.dao.DefaultTenantDao;
import org.killbill.billing.tenant.dao.TenantModelDao;
import org.killbill.billing.util.dao.DefaultNonEntityDao;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;

public class TestKillbillJdbcRealm extends TestJaxrsBase {

    private SecurityManager securityManager;
    private DefaultTenant tenant;

    @Override
    @BeforeMethod(groups = "slow")
    public void beforeMethod() throws Exception {
        super.beforeMethod();

        // Create the tenant
        final DefaultTenantDao tenantDao = new DefaultTenantDao(dbi, clock, cacheControllerDispatcher, new DefaultNonEntityDao(dbi));
        tenant = new DefaultTenant(UUID.randomUUID(), null, null, UUID.randomUUID().toString(),
                                   UUID.randomUUID().toString(), UUID.randomUUID().toString());
        tenantDao.create(new TenantModelDao(tenant), internalCallContext);

        // Setup the security manager
        final BoneCPConfig dbConfig = new BoneCPConfig();
        dbConfig.setJdbcUrl(helper.getJdbcConnectionString());
        dbConfig.setUsername(helper.getUsername());
        dbConfig.setPassword(helper.getPassword());

        final KillbillJdbcRealm jdbcRealm;
        jdbcRealm = new KillbillJdbcRealm(daoConfig);
        jdbcRealm.setDataSource(new BoneCPDataSource(dbConfig));

        securityManager = new DefaultSecurityManager(jdbcRealm);
    }

    @Test(groups = "slow")
    public void testAuthentication() throws Exception {
        final DelegatingSubject subject = new DelegatingSubject(securityManager);

        // Good combo
        final AuthenticationToken goodToken = new UsernamePasswordToken(tenant.getApiKey(), tenant.getApiSecret());
        try {
            securityManager.login(subject, goodToken);
            Assert.assertTrue(true);
        } catch (AuthenticationException e) {
            Assert.fail();
        }

        // Bad login
        final AuthenticationToken badPasswordToken = new UsernamePasswordToken(tenant.getApiKey(), tenant.getApiSecret() + "T");
        try {
            securityManager.login(subject, badPasswordToken);
            Assert.fail();
        } catch (AuthenticationException e) {
            Assert.assertTrue(true);
        }

        // Bad password
        final AuthenticationToken badLoginToken = new UsernamePasswordToken(tenant.getApiKey() + "U", tenant.getApiSecret());
        try {
            securityManager.login(subject, badLoginToken);
            Assert.fail();
        } catch (AuthenticationException e) {
            Assert.assertTrue(true);
        }
    }
}
