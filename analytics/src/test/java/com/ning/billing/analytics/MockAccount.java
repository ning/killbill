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

package com.ning.billing.analytics;

import java.util.UUID;
import com.ning.billing.account.api.AccountData;
import com.ning.billing.catalog.api.Currency;

public class MockAccount implements AccountData
{
    private final UUID id;
    private final String accountKey;
    private final Currency currency;

    public MockAccount(final UUID id, final String accountKey, final Currency currency)
    {
        this.id = id;
        this.accountKey = accountKey;
        this.currency = currency;
    }

    @Override
    public int getFirstNameLength() {
        return 0;
    }

    @Override
    public String getEmail()
    {
        return "test@test.com";
    }

    @Override
    public String getPhone()
    {
        return "408-555-6665";
    }

    @Override
    public String getExternalKey()
    {
        return accountKey;
    }

    @Override
    public String getName() {
        return "firstName lastName";
    }

    @Override
    public int getBillCycleDay()
    {
        return 12;
    }

    @Override
    public Currency getCurrency()
    {
        return currency;
    }

    @Override
    public String getPaymentProviderName() {
        return "PayPal";
    }

    @Override
    public UUID getId()
    {
        return id;
    }
}