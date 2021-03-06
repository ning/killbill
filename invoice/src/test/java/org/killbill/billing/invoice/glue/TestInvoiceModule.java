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

package org.killbill.billing.invoice.glue;

import org.killbill.billing.catalog.glue.CatalogModule;
import org.killbill.billing.invoice.TestInvoiceHelper;
import org.killbill.billing.junction.BillingInternalApi;
import org.killbill.billing.subscription.api.SubscriptionBaseInternalApi;
import org.killbill.billing.usage.glue.UsageModule;
import org.killbill.billing.util.email.EmailModule;
import org.killbill.billing.util.email.templates.TemplateModule;
import org.killbill.billing.util.glue.CacheModule;
import org.killbill.billing.util.glue.CallContextModule;
import org.killbill.billing.util.glue.CustomFieldModule;
import org.killbill.billing.util.glue.MemoryGlobalLockerModule;
import org.killbill.billing.util.glue.NotificationQueueModule;
import org.killbill.billing.util.glue.TagStoreModule;
import org.mockito.Mockito;
import org.skife.config.ConfigSource;

public class TestInvoiceModule extends DefaultInvoiceModule {

    public TestInvoiceModule(final ConfigSource configSource) {
        super(configSource);
    }

    private void installExternalApis() {
        bind(SubscriptionBaseInternalApi.class).toInstance(Mockito.mock(SubscriptionBaseInternalApi.class));
        bind(BillingInternalApi.class).toInstance(Mockito.mock(BillingInternalApi.class));
    }

    @Override
    protected void configure() {
        super.configure();
        install(new CallContextModule());
        install(new MemoryGlobalLockerModule());

        install(new CatalogModule(configSource));
        install(new CacheModule(configSource));
        install(new TemplateModule());
        install(new EmailModule(configSource));

        install(new NotificationQueueModule(configSource));
        install(new TagStoreModule());
        install(new CustomFieldModule());
        install(new UsageModule(configSource));
        installExternalApis();

        bind(TestInvoiceHelper.class).asEagerSingleton();
    }
}
