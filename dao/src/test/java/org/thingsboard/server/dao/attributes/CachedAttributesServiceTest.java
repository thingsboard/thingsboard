/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.dao.attributes;

import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Test;
import org.thingsboard.server.dao.cache.CacheExecutorService;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.mock;

public class CachedAttributesServiceTest {

    public static final String REDIS = "redis";

    @Test
    public void givenLocalCacheTypeName_whenEquals_thenOK() {
        assertThat(CachedAttributesService.LOCAL_CACHE_TYPE, is("caffeine"));
    }

    @Test
    public void givenCacheType_whenGetExecutor_thenDirectExecutor() {
        CachedAttributesService cachedAttributesService = mock(CachedAttributesService.class);
        CacheExecutorService cacheExecutorService = mock(CacheExecutorService.class);
        willCallRealMethod().given(cachedAttributesService).getExecutor(any(), any());

        assertThat(cachedAttributesService.getExecutor(null, cacheExecutorService), is(MoreExecutors.directExecutor()));

        assertThat(cachedAttributesService.getExecutor("", cacheExecutorService), is(MoreExecutors.directExecutor()));

        assertThat(cachedAttributesService.getExecutor(CachedAttributesService.LOCAL_CACHE_TYPE, cacheExecutorService), is(MoreExecutors.directExecutor()));

    }

    @Test
    public void givenCacheType_whenGetExecutor_thenReturnCacheExecutorService() {
        CachedAttributesService cachedAttributesService = mock(CachedAttributesService.class);
        CacheExecutorService cacheExecutorService = mock(CacheExecutorService.class);
        willCallRealMethod().given(cachedAttributesService).getExecutor(any(String.class), any(CacheExecutorService.class));

        assertThat(cachedAttributesService.getExecutor(REDIS, cacheExecutorService), is(cacheExecutorService));

        assertThat(cachedAttributesService.getExecutor("unknownCacheType", cacheExecutorService), is(cacheExecutorService));

    }

}