/*
 * Copyright 2018-2018 adorsys GmbH & Co KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.adorsys.xs2a.gateway.service.provider;

import de.adorsys.xs2a.gateway.service.PaymentInitiationService;
import de.adorsys.xs2a.gateway.service.impl.DeutscheBankPaymentInitiationService;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DeutscheBankPaymentInitiationServiceProvider implements PaymentInitiationServiceProvider {

    private Set<String> bankCodes = Collections.unmodifiableSet(new HashSet<>(Collections.singletonList("50010517")));
    private DeutscheBankPaymentInitiationService paymentInitiationService;

    @Override
    public Set<String> getBankCodes() {
        return bankCodes;
    }

    @Override
    public PaymentInitiationService getPaymentInitiationService() {
        if (paymentInitiationService == null) {
            paymentInitiationService = new DeutscheBankPaymentInitiationService();
        }
        return paymentInitiationService;
    }
}