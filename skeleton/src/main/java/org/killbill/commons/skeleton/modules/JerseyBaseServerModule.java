/*
 * Copyright 2010-2014 Ning, Inc.
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

package org.killbill.commons.skeleton.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.Filter;
import javax.servlet.http.HttpServlet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

public class JerseyBaseServerModule extends BaseServerModule {

    private static final Joiner joiner = Joiner.on(";");

    @VisibleForTesting
    static final String JERSEY_CONTAINER_REQUEST_FILTERS = "com.sun.jersey.spi.container.ContainerRequestFilters";
    @VisibleForTesting
    static final String JERSEY_CONTAINER_RESPONSE_FILTERS = "com.sun.jersey.spi.container.ContainerResponseFilters";
    @VisibleForTesting
    static final String JERSEY_DISABLE_ENTITYLOGGING = "com.sun.jersey.config.feature.logging.DisableEntitylogging";

    // See com.sun.jersey.api.core.ResourceConfig
    private final ImmutableMap.Builder<String, String> jerseyParams;

    public JerseyBaseServerModule(final Map<String, ArrayList<Entry<Class<? extends Filter>, Map<String, String>>>> filters,
                                  final Map<String, ArrayList<Entry<Class<? extends Filter>, Map<String, String>>>> filtersRegex,
                                  final Map<String, Class<? extends HttpServlet>> servlets,
                                  final Map<String, Class<? extends HttpServlet>> servletsRegex,
                                  final Map<String, Class<? extends HttpServlet>> jaxrsServlets,
                                  final Map<String, Class<? extends HttpServlet>> jaxrsServletsRegex,
                                  final String jaxrsUriPattern,
                                  final Collection<String> jaxrsResources,
                                  final List<String> jerseyFilters,
                                  final Map<String, String> jerseyParams) {
        super(filters, filtersRegex, servlets, servletsRegex, jaxrsServlets, jaxrsServletsRegex, jaxrsUriPattern, jaxrsResources);

        String manuallySpecifiedRequestFilters = Strings.nullToEmpty(jerseyParams.remove(JERSEY_CONTAINER_REQUEST_FILTERS));
        String manuallySpecifiedResponseFilters = Strings.nullToEmpty(jerseyParams.remove(JERSEY_CONTAINER_RESPONSE_FILTERS));
        if (!jerseyFilters.isEmpty()) {
            if (!manuallySpecifiedRequestFilters.isEmpty()) {
                manuallySpecifiedRequestFilters += ";";
            }
            if (!manuallySpecifiedResponseFilters.isEmpty()) {
                manuallySpecifiedResponseFilters += ";";
            }
        }
        final String containerRequestFilters = manuallySpecifiedRequestFilters + joiner.join(jerseyFilters);
        final String containerResponseFilters = manuallySpecifiedResponseFilters + joiner.join(Lists.reverse(jerseyFilters));

        this.jerseyParams = new ImmutableMap.Builder<String, String>();
        if (!containerRequestFilters.isEmpty()) {
            this.jerseyParams.put(JERSEY_CONTAINER_REQUEST_FILTERS, containerRequestFilters);
        }
        if (!containerResponseFilters.isEmpty()) {
            this.jerseyParams.put(JERSEY_CONTAINER_RESPONSE_FILTERS, containerResponseFilters);
        }

        // The LoggingFilter will log the body by default, which breaks StreamingOutput
        final String disableEntityLogging = Objects.firstNonNull(Strings.emptyToNull(jerseyParams.remove(JERSEY_DISABLE_ENTITYLOGGING)), "true");
        this.jerseyParams.put(JERSEY_DISABLE_ENTITYLOGGING, disableEntityLogging)
                         .putAll(jerseyParams);
    }

    @Override
    protected void configureResources() {
        for (final String urlPattern : jaxrsServlets.keySet()) {
            serve(urlPattern).with(jaxrsServlets.get(urlPattern), jerseyParams.build());
        }

        for (final String urlPattern : jaxrsServletsRegex.keySet()) {
            serveRegex(urlPattern).with(jaxrsServletsRegex.get(urlPattern), jerseyParams.build());
        }

        // Catch-all resources
        if (jaxrsResources.size() != 0) {
            jerseyParams.put("com.sun.jersey.config.property.packages", joiner.join(jaxrsResources));
            serveRegex(jaxrsUriPattern).with(GuiceContainer.class, jerseyParams.build());
        }
    }

    @VisibleForTesting
    ImmutableMap.Builder<String, String> getJerseyParams() {
        return jerseyParams;
    }
}
