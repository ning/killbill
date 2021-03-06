/*
 * Copyright 2010-2011 Ning, Inc.
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

package org.killbill.billing.mock.glue;

import org.skife.config.ConfigSource;
import org.skife.config.ConfigurationObjectFactory;

import org.killbill.notificationq.MockNotificationQueueService;
import org.killbill.notificationq.api.NotificationQueueConfig;
import org.killbill.notificationq.api.NotificationQueueService;
import org.killbill.billing.util.glue.NotificationQueueModule;

import com.google.common.collect.ImmutableMap;

public class MockNotificationQueueModule extends NotificationQueueModule {

    public MockNotificationQueueModule(final ConfigSource configSource) {
        super(configSource);
    }

    @Override
    protected void configureNotificationQueueConfig() {
        final NotificationQueueConfig config = new ConfigurationObjectFactory(configSource).buildWithReplacements(NotificationQueueConfig.class,
                                                                                                                  ImmutableMap.<String, String>of("instanceName", "main"));
        bind(NotificationQueueConfig.class).toInstance(config);
    }

    @Override
    protected void configure() {
        bind(NotificationQueueService.class).to(MockNotificationQueueService.class).asEagerSingleton();
        configureNotificationQueueConfig();
    }

}
