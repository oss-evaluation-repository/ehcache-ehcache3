/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehcache.xml.service;

import org.ehcache.config.CacheConfiguration;
import org.ehcache.event.EventType;
import org.ehcache.impl.config.event.DefaultCacheEventListenerConfiguration;
import org.ehcache.xml.exceptions.XmlConfigurationException;
import org.ehcache.xml.model.CacheType;
import org.ehcache.xml.model.EventFiringType;
import org.ehcache.xml.model.EventOrderingType;
import org.ehcache.xml.model.ListenersType;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.pany.ehcache.integration.TestCacheEventListener;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.ehcache.core.spi.service.ServiceUtils.findSingletonAmongst;
import static org.ehcache.event.EventFiring.SYNCHRONOUS;
import static org.ehcache.event.EventOrdering.UNORDERED;
import static org.ehcache.event.EventType.CREATED;
import static org.ehcache.event.EventType.REMOVED;

public class DefaultCacheEventListenerConfigurationParserTest extends ServiceConfigurationParserTestBase {

  public DefaultCacheEventListenerConfigurationParserTest() {
    super(new DefaultCacheEventListenerConfigurationParser());
  }

  @Test
  public void parseServiceConfiguration() throws Exception {
    CacheConfiguration<?, ?> cacheConfiguration = getCacheDefinitionFrom("/configs/ehcache-cacheEventListener.xml", "bar");

    DefaultCacheEventListenerConfiguration listenerConfig =
      findSingletonAmongst(DefaultCacheEventListenerConfiguration.class, cacheConfiguration.getServiceConfigurations());

    assertThat(listenerConfig).isNotNull();
    assertThat(listenerConfig.getClazz()).isEqualTo(TestCacheEventListener.class);
    assertThat(listenerConfig.firingMode()).isEqualTo(SYNCHRONOUS);
    assertThat(listenerConfig.orderingMode()).isEqualTo(UNORDERED);
    assertThat(listenerConfig.fireOn()).containsExactlyInAnyOrder(EventType.values());
  }


  @Test
  public void unparseServiceConfiguration() {
    DefaultCacheEventListenerConfiguration listenerConfig =
      new DefaultCacheEventListenerConfiguration(EnumSet.of(CREATED, REMOVED), TestCacheEventListener.class);
    listenerConfig.setEventFiringMode(SYNCHRONOUS);
    listenerConfig.setEventOrderingMode(UNORDERED);

    CacheConfiguration<?, ?> cacheConfig = buildCacheConfigWithServiceConfig(listenerConfig);
    CacheType cacheType = new CacheType();
    cacheType = parser.unparseServiceConfiguration(cacheConfig, cacheType);

    List<ListenersType.Listener> listeners = cacheType.getListeners().getListener();
    assertThat(listeners).hasSize(1);
    ListenersType.Listener listener = listeners.get(0);
    assertThat(listener.getEventFiringMode()).isEqualTo(EventFiringType.SYNCHRONOUS);
    assertThat(listener.getEventOrderingMode()).isEqualTo(EventOrderingType.UNORDERED);
    assertThat(listener.getEventsToFireOn()).contains(org.ehcache.xml.model.EventType.CREATED, org.ehcache.xml.model.EventType.REMOVED);
  }

  @Test
  public void unparseServiceConfigurationWithInstance() {
    TestCacheEventListener testCacheEventListener = new TestCacheEventListener();
    DefaultCacheEventListenerConfiguration listenerConfig =
      new DefaultCacheEventListenerConfiguration(EnumSet.of(CREATED, REMOVED), testCacheEventListener);
    listenerConfig.setEventFiringMode(SYNCHRONOUS);
    listenerConfig.setEventOrderingMode(UNORDERED);

    CacheConfiguration<?, ?> cacheConfig = buildCacheConfigWithServiceConfig(listenerConfig);
    CacheType cacheType = new CacheType();
    assertThatExceptionOfType(XmlConfigurationException.class).isThrownBy(() ->
      parser.unparseServiceConfiguration(cacheConfig, cacheType))
      .withMessage("%s", "XML translation for instance based intialization for " +
                         "DefaultCacheEventListenerConfiguration is not supported");
  }
}
