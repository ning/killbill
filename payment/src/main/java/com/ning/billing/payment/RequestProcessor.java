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

package com.ning.billing.payment;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.ning.billing.account.api.IAccount;
import com.ning.billing.account.api.IAccountUserApi;
import com.ning.billing.invoice.model.Invoice;
import com.ning.billing.payment.provider.PaymentProviderPlugin;
import com.ning.billing.payment.provider.PaymentProviderPluginRegistry;
import com.ning.billing.util.Either;
import com.ning.billing.util.eventbus.IEventBus;
import com.ning.billing.util.eventbus.IEventBus.EventBusException;

public class RequestProcessor {
    public static final String PAYMENT_PROVIDER_KEY = "paymentProvider";
    private final IAccountUserApi accountUserApi;
    private final PaymentProviderPluginRegistry pluginRegistry;
    private final IEventBus eventBus;

    @Inject
    public RequestProcessor(IAccountUserApi accountUserApi,
                            PaymentProviderPluginRegistry pluginRegistry,
                            IEventBus eventBus) {
        this.accountUserApi = accountUserApi;
        this.pluginRegistry = pluginRegistry;
        this.eventBus = eventBus;
    }

    @Subscribe
    public void receiveInvoice(Invoice invoice) throws EventBusException {
        final IAccount account = accountUserApi.getAccountFromId(invoice.getAccountId());
        final String paymentProviderName = account.getFieldValue(PAYMENT_PROVIDER_KEY);
        final PaymentProviderPlugin plugin = pluginRegistry.getPlugin(paymentProviderName);

        Either<PaymentError, PaymentInfo> result = plugin.processInvoice(account, invoice);

        eventBus.post(result.isLeft() ? result.getLeft() : result.getRight());
    }

    @Subscribe
    public void receiveRequest(PaymentInfoRequest request) throws EventBusException {
        final IAccount account = accountUserApi.getAccountFromId(request.getAccountId());
        final String paymentProviderName = account.getFieldValue(PAYMENT_PROVIDER_KEY);
        final PaymentProviderPlugin plugin = pluginRegistry.getPlugin(paymentProviderName);

        Either<PaymentError, PaymentInfo> result = plugin.getPaymentInfo(request.getPaymentId());

        eventBus.post(result.isLeft() ? result.getLeft() : result.getRight());
    }
}