/*
 * Copyright (C) 2013, 2014 Brett Wooldridge
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

package com.zaxxer.hikari;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import com.zaxxer.hikari.pool.PoolElf;
import com.zaxxer.hikari.util.PropertyElf;

public class HikariConfig implements HikariConfigMXBean
{
    public void test(Object metricRegistry)
    {
       if (metricsTrackerFactory != null) {
          throw new IllegalStateException("cannot use setMetricRegistry() and setMetricsTrackerFactory() together");
       }
 
       if (metricRegistry != null) {
          if (metricRegistry instanceof String) {
             try {
                InitialContext initCtx = new InitialContext();
                metricRegistry = (MetricRegistry) initCtx.lookup((String) metricRegistry);
             }
             catch (NamingException e) {
                throw new IllegalArgumentException(e);
             }
          }
 
          if (!(metricRegistry instanceof MetricRegistry)) {
             throw new IllegalArgumentException("Class must be an instance of com.codahale.metrics.MetricRegistry");
          }
       }
 
       this.metricRegistry = metricRegistry;
    }
}
