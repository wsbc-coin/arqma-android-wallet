/*
 * Copyright (c) 2017-2018 m2049r et al.
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

package com.m2049r.xmrwallet.service.exchange.coinmarketcap;

import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeApi;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeCallback;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeException;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeRate;

import net.jodah.concurrentunit.Waiter;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeoutException;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;


public class ExchangeRateTest {

    private MockWebServer mockWebServer;

    private ExchangeApi exchangeApi;

    private final OkHttpClient okHttpClient = new OkHttpClient();
    private Waiter waiter;

    @Mock
    ExchangeCallback mockExchangeCallback;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        waiter = new Waiter();

        MockitoAnnotations.initMocks(this);

        exchangeApi = new ExchangeApiImpl(okHttpClient, mockWebServer.url("/"));
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    public void queryExchangeRate_shouldBeGetMethod()
            throws InterruptedException, TimeoutException {

        exchangeApi.queryExchangeRate(Wallet.ARQ_SYMBOL, "EUR", mockExchangeCallback);

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("GET", request.getMethod());
    }

    @Test
    public void queryExchangeRate_shouldHavePairInUrl()
            throws InterruptedException, TimeoutException {

        exchangeApi.queryExchangeRate(Wallet.ARQ_SYMBOL, "EUR", mockExchangeCallback);

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("/?convert=EUR", request.getPath());
    }

    @Test
    public void queryExchangeRate_wasSuccessfulShouldRespondWithRate()
            throws InterruptedException, JSONException, TimeoutException {
        final String base = Wallet.ARQ_SYMBOL;
        final String quote = "EUR";
        final double rate = 1.56;
        MockResponse jsonMockResponse = new MockResponse().setBody(
                createMockExchangeRateResponse(base, quote, rate));
        mockWebServer.enqueue(jsonMockResponse);

        exchangeApi.queryExchangeRate(base, quote, new ExchangeCallback() {
            @Override
            public void onSuccess(final ExchangeRate exchangeRate) {
                waiter.assertEquals(exchangeRate.getBaseCurrency(), base);
                waiter.assertEquals(exchangeRate.getQuoteCurrency(), quote);
                waiter.assertEquals(exchangeRate.getRate(), rate);
                waiter.resume();
            }

            @Override
            public void onError(final Exception e) {
                waiter.fail(e);
                waiter.resume();
            }
        });
        waiter.await();
    }

    @Test
    public void queryExchangeRate_wasSuccessfulShouldRespondWithRateUSD()
            throws InterruptedException, JSONException, TimeoutException {
        final String base = Wallet.ARQ_SYMBOL;
        final String quote = "USD";
        final double rate = 1.56;
        MockResponse jsonMockResponse = new MockResponse().setBody(
                createMockExchangeRateResponse(base, quote, rate));
        mockWebServer.enqueue(jsonMockResponse);

        exchangeApi.queryExchangeRate(base, quote, new ExchangeCallback() {
            @Override
            public void onSuccess(final ExchangeRate exchangeRate) {
                waiter.assertEquals(exchangeRate.getBaseCurrency(), base);
                waiter.assertEquals(exchangeRate.getQuoteCurrency(), quote);
                waiter.assertEquals(exchangeRate.getRate(), rate);
                waiter.resume();
            }

            @Override
            public void onError(final Exception e) {
                waiter.fail(e);
                waiter.resume();
            }
        });
        waiter.await();
    }

    @Test
    public void queryExchangeRate_wasNotSuccessfulShouldCallOnError()
            throws InterruptedException, JSONException, TimeoutException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        exchangeApi.queryExchangeRate(Wallet.ARQ_SYMBOL, "USD", new ExchangeCallback() {
            @Override
            public void onSuccess(final ExchangeRate exchangeRate) {
                waiter.fail();
                waiter.resume();
            }

            @Override
            public void onError(final Exception e) {
                waiter.assertTrue(e instanceof ExchangeException);
                waiter.assertTrue(((ExchangeException) e).getCode() == 500);
                waiter.resume();
            }

        });
        waiter.await();
    }

    @Test
    public void queryExchangeRate_unknownAssetShouldCallOnError()
            throws InterruptedException, JSONException, TimeoutException {
        MockResponse jsonMockResponse = new MockResponse().setBody(
                createMockExchangeRateErrorResponse());
        mockWebServer.enqueue(jsonMockResponse);

        exchangeApi.queryExchangeRate(Wallet.ARQ_SYMBOL, "ABC", new ExchangeCallback() {
            @Override
            public void onSuccess(final ExchangeRate exchangeRate) {
                waiter.fail();
                waiter.resume();
            }

            @Override
            public void onError(final Exception e) {
                waiter.assertTrue(e instanceof ExchangeException);
                ExchangeException ex = (ExchangeException) e;
                waiter.assertTrue(ex.getCode() == 200);
                waiter.assertEquals(ex.getErrorMsg(), "id not found");
                waiter.resume();
            }

        });
        waiter.await();
    }

    private String createMockExchangeRateResponse(final String base, final String quote, final double rate) {
        return "{\n" +
                "    \"data\": {\n" +
                "        \"id\": 2748, \n" +
                "        \"name\": \"Arqma\", \n" +
                "        \"symbol\": \"" + base + "\", \n" +
                "        \"website_slug\": \"loki\", \n" +
                "        \"rank\": 484, \n" +
                "        \"circulating_supply\": 18679078.0, \n" +
                "        \"total_supply\": 25585140.0, \n" +
                "        \"max_supply\": null, \n" +
                "        \"quotes\": {\n" +
                "            \"USD\": {\n" +
                "                \"price\": " + rate + ", \n" +
                "                \"volume_24h\": 57090.0, \n" +
                "                \"market_cap\": 9182747.0, \n" +
                "                \"percent_change_1h\": -2.34, \n" +
                "                \"percent_change_24h\": 2.08, \n" +
                "                \"percent_change_7d\": -31.08\n" +
                "            }, \n" +
                (!"USD".equals(quote) ? (
                        "            \"" + quote + "\": {\n" +
                                "                \"price\": " + rate + ", \n" +
                                "                \"volume_24h\": 30377728.701265607, \n" +
                                "                \"market_cap\": 2174289586.0, \n" +
                                "                \"percent_change_1h\": -0.16, \n" +
                                "                \"percent_change_24h\": -3.46, \n" +
                                "                \"percent_change_7d\": 1.49\n" +
                                "            }\n") : "") +
                "        }, \n" +
                "        \"last_updated\": 1528795188\n" +
                "    }, \n" +
                "    \"metadata\": {\n" +
                "        \"timestamp\": 1528794926, \n" +
                "        \"error\": null\n" +
                "    }\n" +
                "}";
    }

    private String createMockExchangeRateErrorResponse() {
        return "{\n" +
                "    \"data\": null, \n" +
                "    \"metadata\": {\n" +
                "        \"timestamp\": 1525137187, \n" +
                "        \"error\": \"id not found\"\n" +
                "    }\n" +
                "}";
    }
}
