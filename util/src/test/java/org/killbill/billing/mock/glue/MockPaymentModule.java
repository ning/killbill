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

import org.mockito.Mockito;

import org.killbill.billing.payment.api.PaymentApi;
import org.killbill.billing.payment.api.PaymentInternalApi;

import com.google.inject.AbstractModule;

public class MockPaymentModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(PaymentApi.class).toInstance(Mockito.mock(PaymentApi.class));
        bind(PaymentInternalApi.class).toInstance(Mockito.mock(PaymentInternalApi.class));
    }
}
