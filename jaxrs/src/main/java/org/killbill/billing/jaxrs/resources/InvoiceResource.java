/*
 * Copyright 2010-2013 Ning, Inc.
 * Copyright 2014 Groupon, Inc
 * Copyright 2014 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
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

package org.killbill.billing.jaxrs.resources;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.joda.time.LocalDate;
import org.killbill.billing.ErrorCode;
import org.killbill.billing.ObjectType;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.account.api.AccountUserApi;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.entitlement.api.SubscriptionApiException;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceApiException;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.invoice.api.InvoiceNotifier;
import org.killbill.billing.invoice.api.InvoiceUserApi;
import org.killbill.billing.jaxrs.json.CustomFieldJson;
import org.killbill.billing.jaxrs.json.InvoiceItemJson;
import org.killbill.billing.jaxrs.json.InvoiceJson;
import org.killbill.billing.jaxrs.json.PaymentJson;
import org.killbill.billing.jaxrs.util.Context;
import org.killbill.billing.jaxrs.util.JaxrsUriBuilder;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentApi;
import org.killbill.billing.payment.api.PaymentApiException;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.util.api.AuditUserApi;
import org.killbill.billing.util.api.CustomFieldApiException;
import org.killbill.billing.util.api.CustomFieldUserApi;
import org.killbill.billing.util.api.TagApiException;
import org.killbill.billing.util.api.TagDefinitionApiException;
import org.killbill.billing.util.api.TagUserApi;
import org.killbill.billing.util.audit.AccountAuditLogs;
import org.killbill.billing.util.audit.AccountAuditLogsForObjectType;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.billing.util.entity.Pagination;
import org.killbill.clock.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_HTML;

@Path(JaxrsResource.INVOICES_PATH)
public class InvoiceResource extends JaxRsResourceBase {

    private static final Logger log = LoggerFactory.getLogger(InvoiceResource.class);
    private static final String ID_PARAM_NAME = "invoiceId";

    private final InvoiceUserApi invoiceApi;
    private final PaymentApi paymentApi;
    private final InvoiceNotifier invoiceNotifier;

    @Inject
    public InvoiceResource(final AccountUserApi accountUserApi,
                           final InvoiceUserApi invoiceApi,
                           final PaymentApi paymentApi,
                           final InvoiceNotifier invoiceNotifier,
                           final Clock clock,
                           final JaxrsUriBuilder uriBuilder,
                           final TagUserApi tagUserApi,
                           final CustomFieldUserApi customFieldUserApi,
                           final AuditUserApi auditUserApi,
                           final Context context) {
        super(uriBuilder, tagUserApi, customFieldUserApi, auditUserApi, accountUserApi, clock, context);
        this.invoiceApi = invoiceApi;
        this.paymentApi = paymentApi;
        this.invoiceNotifier = invoiceNotifier;
    }

    @GET
    @Path("/{invoiceId:" + UUID_PATTERN + "}/")
    @Produces(APPLICATION_JSON)
    public Response getInvoice(@PathParam("invoiceId") final String invoiceId,
                               @QueryParam(QUERY_INVOICE_WITH_ITEMS) @DefaultValue("false") final boolean withItems,
                               @QueryParam(QUERY_AUDIT) @DefaultValue("NONE") final AuditMode auditMode,
                               @javax.ws.rs.core.Context final HttpServletRequest request) throws InvoiceApiException {
        final TenantContext tenantContext = context.createContext(request);
        final Invoice invoice = invoiceApi.getInvoice(UUID.fromString(invoiceId), tenantContext);
        final AccountAuditLogs accountAuditLogs = auditUserApi.getAccountAuditLogs(invoice.getAccountId(), auditMode.getLevel(), tenantContext);

        if (invoice == null) {
            throw new InvoiceApiException(ErrorCode.INVOICE_NOT_FOUND, invoiceId);
        } else {
            final InvoiceJson json = new InvoiceJson(invoice, withItems, accountAuditLogs);
            return Response.status(Status.OK).entity(json).build();
        }
    }

    @GET
    @Path("/{invoiceNumber:" + NUMBER_PATTERN + "}/")
    @Produces(APPLICATION_JSON)
    public Response getInvoiceByNumber(@PathParam("invoiceNumber") final Integer invoiceNumber,
                                       @QueryParam(QUERY_INVOICE_WITH_ITEMS) @DefaultValue("false") final boolean withItems,
                                       @QueryParam(QUERY_AUDIT) @DefaultValue("NONE") final AuditMode auditMode,
                                       @javax.ws.rs.core.Context final HttpServletRequest request) throws InvoiceApiException {
        final TenantContext tenantContext = context.createContext(request);
        final Invoice invoice = invoiceApi.getInvoiceByNumber(invoiceNumber, tenantContext);
        final AccountAuditLogs accountAuditLogs = auditUserApi.getAccountAuditLogs(invoice.getAccountId(), auditMode.getLevel(), tenantContext);

        if (invoice == null) {
            throw new InvoiceApiException(ErrorCode.INVOICE_NOT_FOUND, invoiceNumber);
        } else {
            final InvoiceJson json = new InvoiceJson(invoice, withItems, accountAuditLogs);
            return Response.status(Status.OK).entity(json).build();
        }
    }

    @GET
    @Path("/{invoiceId:" + UUID_PATTERN + "}/html")
    @Produces(TEXT_HTML)
    public Response getInvoiceAsHTML(@PathParam("invoiceId") final String invoiceId,
                                     @javax.ws.rs.core.Context final HttpServletRequest request) throws InvoiceApiException, IOException, AccountApiException {
        return Response.status(Status.OK).entity(invoiceApi.getInvoiceAsHTML(UUID.fromString(invoiceId), context.createContext(request))).build();
    }

    @GET
    @Path("/" + PAGINATION)
    @Produces(APPLICATION_JSON)
    public Response getInvoices(@QueryParam(QUERY_SEARCH_OFFSET) @DefaultValue("0") final Long offset,
                                @QueryParam(QUERY_SEARCH_LIMIT) @DefaultValue("100") final Long limit,
                                @QueryParam(QUERY_INVOICE_WITH_ITEMS) @DefaultValue("false") final Boolean withItems,
                                @QueryParam(QUERY_AUDIT) @DefaultValue("NONE") final AuditMode auditMode,
                                @javax.ws.rs.core.Context final HttpServletRequest request) throws InvoiceApiException {
        final TenantContext tenantContext = context.createContext(request);
        final Pagination<Invoice> invoices = invoiceApi.getInvoices(offset, limit, tenantContext);
        final URI nextPageUri = uriBuilder.nextPage(InvoiceResource.class, "getInvoices", invoices.getNextOffset(), limit, ImmutableMap.<String, String>of(QUERY_INVOICE_WITH_ITEMS, withItems.toString(),
                                                                                                                                                           QUERY_AUDIT, auditMode.getLevel().toString()));

        final AtomicReference<Map<UUID, AccountAuditLogs>> accountsAuditLogs = new AtomicReference<Map<UUID, AccountAuditLogs>>(new HashMap<UUID, AccountAuditLogs>());
        return buildStreamingPaginationResponse(invoices,
                                                new Function<Invoice, InvoiceJson>() {
                                                    @Override
                                                    public InvoiceJson apply(final Invoice invoice) {
                                                        // Cache audit logs per account
                                                        if (accountsAuditLogs.get().get(invoice.getAccountId()) == null) {
                                                            accountsAuditLogs.get().put(invoice.getAccountId(), auditUserApi.getAccountAuditLogs(invoice.getAccountId(), auditMode.getLevel(), tenantContext));
                                                        }
                                                        return new InvoiceJson(invoice, withItems, accountsAuditLogs.get().get(invoice.getAccountId()));
                                                    }
                                                },
                                                nextPageUri
                                               );
    }

    @GET
    @Path("/" + SEARCH + "/{searchKey:" + ANYTHING_PATTERN + "}")
    @Produces(APPLICATION_JSON)
    public Response searchInvoices(@PathParam("searchKey") final String searchKey,
                                   @QueryParam(QUERY_SEARCH_OFFSET) @DefaultValue("0") final Long offset,
                                   @QueryParam(QUERY_SEARCH_LIMIT) @DefaultValue("100") final Long limit,
                                   @QueryParam(QUERY_INVOICE_WITH_ITEMS) @DefaultValue("false") final Boolean withItems,
                                   @QueryParam(QUERY_AUDIT) @DefaultValue("NONE") final AuditMode auditMode,
                                   @javax.ws.rs.core.Context final HttpServletRequest request) throws SubscriptionApiException {
        final TenantContext tenantContext = context.createContext(request);
        final Pagination<Invoice> invoices = invoiceApi.searchInvoices(searchKey, offset, limit, tenantContext);
        final URI nextPageUri = uriBuilder.nextPage(InvoiceResource.class, "searchInvoices", invoices.getNextOffset(), limit, ImmutableMap.<String, String>of("searchKey", searchKey,
                                                                                                                                                              QUERY_INVOICE_WITH_ITEMS, withItems.toString(),
                                                                                                                                                              QUERY_AUDIT, auditMode.getLevel().toString()));
        final AtomicReference<Map<UUID, AccountAuditLogs>> accountsAuditLogs = new AtomicReference<Map<UUID, AccountAuditLogs>>(new HashMap<UUID, AccountAuditLogs>());
        return buildStreamingPaginationResponse(invoices,
                                                new Function<Invoice, InvoiceJson>() {
                                                    @Override
                                                    public InvoiceJson apply(final Invoice invoice) {
                                                        // Cache audit logs per account
                                                        if (accountsAuditLogs.get().get(invoice.getAccountId()) == null) {
                                                            accountsAuditLogs.get().put(invoice.getAccountId(), auditUserApi.getAccountAuditLogs(invoice.getAccountId(), auditMode.getLevel(), tenantContext));
                                                        }
                                                        return new InvoiceJson(invoice, withItems, accountsAuditLogs.get().get(invoice.getAccountId()));
                                                    }
                                                },
                                                nextPageUri
                                               );
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response createFutureInvoice(@QueryParam(QUERY_ACCOUNT_ID) final String accountId,
                                        @QueryParam(QUERY_TARGET_DATE) final String targetDateTime,
                                        @QueryParam(QUERY_DRY_RUN) @DefaultValue("false") final Boolean dryRun,
                                        @HeaderParam(HDR_CREATED_BY) final String createdBy,
                                        @HeaderParam(HDR_REASON) final String reason,
                                        @HeaderParam(HDR_COMMENT) final String comment,
                                        @javax.ws.rs.core.Context final HttpServletRequest request,
                                        @javax.ws.rs.core.Context final UriInfo uriInfo) throws AccountApiException, InvoiceApiException {
        final CallContext callContext = context.createContext(createdBy, reason, comment, request);
        final LocalDate inputDate = toLocalDate(UUID.fromString(accountId), targetDateTime, callContext);

        final Invoice generatedInvoice = invoiceApi.triggerInvoiceGeneration(UUID.fromString(accountId), inputDate, dryRun,
                                                                             callContext);
        if (dryRun) {
            return Response.status(Status.OK).entity(new InvoiceJson(generatedInvoice)).build();
        } else {
            return uriBuilder.buildResponse(uriInfo, InvoiceResource.class, "getInvoice", generatedInvoice.getId());
        }
    }

    @DELETE
    @Path("/{invoiceId:" + UUID_PATTERN + "}" + "/{invoiceItemId:" + UUID_PATTERN + "}/cba")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response deleteCBA(@PathParam("invoiceId") final String invoiceId,
                              @PathParam("invoiceItemId") final String invoiceItemId,
                              @QueryParam(QUERY_ACCOUNT_ID) final String accountId,
                              @HeaderParam(HDR_CREATED_BY) final String createdBy,
                              @HeaderParam(HDR_REASON) final String reason,
                              @HeaderParam(HDR_COMMENT) final String comment,
                              @javax.ws.rs.core.Context final HttpServletRequest request) throws AccountApiException, InvoiceApiException {
        final CallContext callContext = context.createContext(createdBy, reason, comment, request);

        final Account account = accountUserApi.getAccountById(UUID.fromString(accountId), callContext);

        invoiceApi.deleteCBA(account.getId(), UUID.fromString(invoiceId), UUID.fromString(invoiceItemId), callContext);

        return Response.status(Status.OK).build();
    }

    @POST
    @Path("/{invoiceId:" + UUID_PATTERN + "}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response adjustInvoiceItem(final InvoiceItemJson json,
                                      @PathParam("invoiceId") final String invoiceId,
                                      @QueryParam(QUERY_REQUESTED_DT) final String requestedDateTimeString,
                                      @HeaderParam(HDR_CREATED_BY) final String createdBy,
                                      @HeaderParam(HDR_REASON) final String reason,
                                      @HeaderParam(HDR_COMMENT) final String comment,
                                      @javax.ws.rs.core.Context final HttpServletRequest request,
                                      @javax.ws.rs.core.Context final UriInfo uriInfo) throws AccountApiException, InvoiceApiException {
        final CallContext callContext = context.createContext(createdBy, reason, comment, request);

        final UUID accountId = UUID.fromString(json.getAccountId());
        final LocalDate requestedDate = toLocalDate(accountId, requestedDateTimeString, callContext);
        final InvoiceItem adjustmentItem;
        if (json.getAmount() == null) {
            adjustmentItem = invoiceApi.insertInvoiceItemAdjustment(accountId,
                                                                    UUID.fromString(invoiceId),
                                                                    UUID.fromString(json.getInvoiceItemId()),
                                                                    requestedDate,
                                                                    callContext);
        } else {
            adjustmentItem = invoiceApi.insertInvoiceItemAdjustment(accountId,
                                                                    UUID.fromString(invoiceId),
                                                                    UUID.fromString(json.getInvoiceItemId()),
                                                                    requestedDate,
                                                                    json.getAmount(),
                                                                    json.getCurrency(),
                                                                    callContext);
        }

        return uriBuilder.buildResponse(uriInfo, InvoiceResource.class, "getInvoice", adjustmentItem.getInvoiceId());
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @Path("/" + CHARGES + "/{accountId:" + UUID_PATTERN + "}")
    public Response createExternalCharges(final Iterable<InvoiceItemJson> externalChargesJson,
                                          @PathParam("accountId") final String accountId,
                                          @QueryParam(QUERY_REQUESTED_DT) final String requestedDateTimeString,
                                          @QueryParam(QUERY_PAY_INVOICE) @DefaultValue("false") final Boolean payInvoice,
                                          @QueryParam(QUERY_PLUGIN_PROPERTY) final List<String> pluginPropertiesString,
                                          @HeaderParam(HDR_CREATED_BY) final String createdBy,
                                          @HeaderParam(HDR_REASON) final String reason,
                                          @HeaderParam(HDR_COMMENT) final String comment,
                                          @javax.ws.rs.core.Context final UriInfo uriInfo,
                                          @javax.ws.rs.core.Context final HttpServletRequest request) throws AccountApiException, InvoiceApiException, PaymentApiException {
        final Iterable<PluginProperty> pluginProperties = extractPluginProperties(pluginPropertiesString);
        final CallContext callContext = context.createContext(createdBy, reason, comment, request);

        final Account account = accountUserApi.getAccountById(UUID.fromString(accountId), callContext);

        // TODO Get rid of that check once we truly support multiple currencies per account
        // See discussion https://github.com/killbill/killbill/commit/942e214d49e9c7ed89da76d972ee017d2d3ade58#commitcomment-6045547
        final Set<Currency> currencies = new HashSet<Currency>(Lists.<InvoiceItemJson, Currency>transform(ImmutableList.<InvoiceItemJson>copyOf(externalChargesJson),
                                                                                                          new Function<InvoiceItemJson, Currency>() {
                                                                                                              @Override
                                                                                                              public Currency apply(final InvoiceItemJson input) {
                                                                                                                  return input.getCurrency();
                                                                                                              }
                                                                                                          }
                                                                                                         ));
        if (currencies.size() != 1 || !currencies.iterator().next().equals(account.getCurrency())) {
            throw new InvoiceApiException(ErrorCode.EXTERNAL_CHARGE_CURRENCY_INVALID, account.getCurrency());
        }

        // Get the effective date of the external charge, in the account timezone
        final LocalDate requestedDate = toLocalDate(account, requestedDateTimeString, callContext);

        final Iterable<InvoiceItem> externalCharges = Iterables.<InvoiceItemJson, InvoiceItem>transform(externalChargesJson,
                                                                                                        new Function<InvoiceItemJson, InvoiceItem>() {
                                                                                                            @Override
                                                                                                            public InvoiceItem apply(final InvoiceItemJson invoiceItemJson) {
                                                                                                                return invoiceItemJson.toInvoiceItem();
                                                                                                            }
                                                                                                        }
                                                                                                       );
        final List<InvoiceItem> createdExternalCharges = invoiceApi.insertExternalCharges(account.getId(), requestedDate, externalCharges, callContext);

        if (payInvoice) {
            final Collection<UUID> paidInvoices = new HashSet<UUID>();
            for (final InvoiceItem externalCharge : createdExternalCharges) {
                if (!paidInvoices.contains(externalCharge.getInvoiceId())) {
                    paidInvoices.add(externalCharge.getInvoiceId());

                    final Invoice invoice = invoiceApi.getInvoice(externalCharge.getInvoiceId(), callContext);
                    paymentApi.createPayment(account, invoice.getId(), invoice.getBalance(), pluginProperties, callContext);
                }
            }
        }

        final List<InvoiceItemJson> createdExternalChargesJson = Lists.<InvoiceItem, InvoiceItemJson>transform(createdExternalCharges,
                                                                                                               new Function<InvoiceItem, InvoiceItemJson>() {
                                                                                                                   @Override
                                                                                                                   public InvoiceItemJson apply(final InvoiceItem input) {
                                                                                                                       return new InvoiceItemJson(input);
                                                                                                                   }
                                                                                                               }
                                                                                                              );
        return Response.status(Status.OK).entity(createdExternalChargesJson).build();
    }

    @GET
    @Path("/{invoiceId:" + UUID_PATTERN + "}/" + PAYMENTS)
    @Produces(APPLICATION_JSON)
    public Response getPayments(@PathParam("invoiceId") final String invoiceId,
                                @QueryParam(QUERY_AUDIT) @DefaultValue("NONE") final AuditMode auditMode,
                                @javax.ws.rs.core.Context final HttpServletRequest request) throws PaymentApiException {
        final TenantContext tenantContext = context.createContext(request);
        final List<Payment> payments = paymentApi.getInvoicePayments(UUID.fromString(invoiceId), tenantContext);
        final List<PaymentJson> result = new ArrayList<PaymentJson>(payments.size());
        if (payments.size() == 0) {
            return Response.status(Status.OK).entity(result).build();
        }

        final AccountAuditLogsForObjectType auditLogsForPayments = auditUserApi.getAccountAuditLogs(payments.get(0).getAccountId(),
                                                                                                    ObjectType.PAYMENT,
                                                                                                    auditMode.getLevel(),
                                                                                                    tenantContext);

        for (final Payment cur : payments) {
            result.add(new PaymentJson(cur, auditLogsForPayments.getAuditLogs(cur.getId())));
        }

        return Response.status(Status.OK).entity(result).build();
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @Path("/{invoiceId:" + UUID_PATTERN + "}/" + PAYMENTS)
    public Response createInstantPayment(final PaymentJson payment,
                                         @QueryParam(QUERY_PAYMENT_EXTERNAL) @DefaultValue("false") final Boolean externalPayment,
                                         @QueryParam(QUERY_PLUGIN_PROPERTY) final List<String> pluginPropertiesString,
                                         @HeaderParam(HDR_CREATED_BY) final String createdBy,
                                         @HeaderParam(HDR_REASON) final String reason,
                                         @HeaderParam(HDR_COMMENT) final String comment,
                                         @javax.ws.rs.core.Context final HttpServletRequest request,
                                         @javax.ws.rs.core.Context final UriInfo uriInfo) throws AccountApiException, PaymentApiException {
        final Iterable<PluginProperty> pluginProperties = extractPluginProperties(pluginPropertiesString);
        final CallContext callContext = context.createContext(createdBy, reason, comment, request);

        final Account account = accountUserApi.getAccountById(UUID.fromString(payment.getAccountId()), callContext);

        final UUID invoiceId = UUID.fromString(payment.getInvoiceId());
        if (externalPayment) {
            paymentApi.createExternalPayment(account, invoiceId, payment.getAmount(), callContext);
        } else {
            paymentApi.createPayment(account, invoiceId, payment.getAmount(), pluginProperties, callContext);
        }

        return uriBuilder.buildResponse(uriInfo, InvoiceResource.class, "getPayments", payment.getInvoiceId());
    }

    @POST
    @Path("/{invoiceId:" + UUID_PATTERN + "}/" + EMAIL_NOTIFICATIONS)
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response triggerEmailNotificationForInvoice(@PathParam("invoiceId") final String invoiceId,
                                                       @HeaderParam(HDR_CREATED_BY) final String createdBy,
                                                       @HeaderParam(HDR_REASON) final String reason,
                                                       @HeaderParam(HDR_COMMENT) final String comment,
                                                       @javax.ws.rs.core.Context final HttpServletRequest request) throws InvoiceApiException, AccountApiException {
        final CallContext callContext = context.createContext(createdBy, reason, comment, request);

        final Invoice invoice = invoiceApi.getInvoice(UUID.fromString(invoiceId), callContext);
        if (invoice == null) {
            throw new InvoiceApiException(ErrorCode.INVOICE_NOT_FOUND, invoiceId);
        }

        final Account account = accountUserApi.getAccountById(invoice.getAccountId(), callContext);

        // Send the email (synchronous send)
        invoiceNotifier.notify(account, invoice, callContext);

        return Response.status(Status.OK).build();
    }

    @GET
    @Path("/{invoiceId:" + UUID_PATTERN + "}/" + CUSTOM_FIELDS)
    @Produces(APPLICATION_JSON)
    public Response getCustomFields(@PathParam(ID_PARAM_NAME) final String id,
                                    @QueryParam(QUERY_AUDIT) @DefaultValue("NONE") final AuditMode auditMode,
                                    @javax.ws.rs.core.Context final HttpServletRequest request) {
        return super.getCustomFields(UUID.fromString(id), auditMode, context.createContext(request));
    }

    @POST
    @Path("/{invoiceId:" + UUID_PATTERN + "}/" + CUSTOM_FIELDS)
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response createCustomFields(@PathParam(ID_PARAM_NAME) final String id,
                                       final List<CustomFieldJson> customFields,
                                       @HeaderParam(HDR_CREATED_BY) final String createdBy,
                                       @HeaderParam(HDR_REASON) final String reason,
                                       @HeaderParam(HDR_COMMENT) final String comment,
                                       @javax.ws.rs.core.Context final HttpServletRequest request,
                                       @javax.ws.rs.core.Context final UriInfo uriInfo) throws CustomFieldApiException {
        return super.createCustomFields(UUID.fromString(id), customFields,
                                        context.createContext(createdBy, reason, comment, request), uriInfo);
    }

    @DELETE
    @Path("/{invoiceId:" + UUID_PATTERN + "}/" + CUSTOM_FIELDS)
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response deleteCustomFields(@PathParam(ID_PARAM_NAME) final String id,
                                       @QueryParam(QUERY_CUSTOM_FIELDS) final String customFieldList,
                                       @HeaderParam(HDR_CREATED_BY) final String createdBy,
                                       @HeaderParam(HDR_REASON) final String reason,
                                       @HeaderParam(HDR_COMMENT) final String comment,
                                       @javax.ws.rs.core.Context final HttpServletRequest request) throws CustomFieldApiException {
        return super.deleteCustomFields(UUID.fromString(id), customFieldList,
                                        context.createContext(createdBy, reason, comment, request));
    }

    @GET
    @Path("/{invoiceId:" + UUID_PATTERN + "}/" + TAGS)
    @Produces(APPLICATION_JSON)
    public Response getTags(@PathParam(ID_PARAM_NAME) final String invoiceIdString,
                            @QueryParam(QUERY_AUDIT) @DefaultValue("NONE") final AuditMode auditMode,
                            @QueryParam(QUERY_TAGS_INCLUDED_DELETED) @DefaultValue("false") final Boolean includedDeleted,
                            @javax.ws.rs.core.Context final HttpServletRequest request) throws TagDefinitionApiException, InvoiceApiException {
        final UUID invoiceId = UUID.fromString(invoiceIdString);
        final TenantContext tenantContext = context.createContext(request);
        final Invoice invoice = invoiceApi.getInvoice(invoiceId, tenantContext);
        return super.getTags(invoice.getAccountId(), invoiceId, auditMode, includedDeleted, tenantContext);
    }

    @POST
    @Path("/{invoiceId:" + UUID_PATTERN + "}/" + TAGS)
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response createTags(@PathParam(ID_PARAM_NAME) final String id,
                               @QueryParam(QUERY_TAGS) final String tagList,
                               @HeaderParam(HDR_CREATED_BY) final String createdBy,
                               @HeaderParam(HDR_REASON) final String reason,
                               @HeaderParam(HDR_COMMENT) final String comment,
                               @javax.ws.rs.core.Context final UriInfo uriInfo,
                               @javax.ws.rs.core.Context final HttpServletRequest request) throws TagApiException {
        return super.createTags(UUID.fromString(id), tagList, uriInfo,
                                context.createContext(createdBy, reason, comment, request));
    }

    @DELETE
    @Path("/{invoiceId:" + UUID_PATTERN + "}/" + TAGS)
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response deleteTags(@PathParam(ID_PARAM_NAME) final String id,
                               @QueryParam(QUERY_TAGS) final String tagList,
                               @HeaderParam(HDR_CREATED_BY) final String createdBy,
                               @HeaderParam(HDR_REASON) final String reason,
                               @HeaderParam(HDR_COMMENT) final String comment,
                               @javax.ws.rs.core.Context final HttpServletRequest request) throws TagApiException {
        return super.deleteTags(UUID.fromString(id), tagList,
                                context.createContext(createdBy, reason, comment, request));
    }

    @Override
    protected ObjectType getObjectType() {
        return ObjectType.INVOICE;
    }
}
