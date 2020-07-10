/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.sql.lock.LockRepository;

import javax.annotation.PostConstruct;

@Service
@Slf4j
public class LockServiceImpl implements LockService {

    @Autowired(required = false)
    private LockRepository lockRepository;

    @PostConstruct
    public void init(){
        log.warn("Locking with DB is not enabled.");
    }

    @Override
    public void transactionLock(LockKey key) {
        if (lockRepository == null) return;
        log.trace("Locking transaction key [{}]", key);
        lockRepository.transactionLock(key.getId());
    }
}
