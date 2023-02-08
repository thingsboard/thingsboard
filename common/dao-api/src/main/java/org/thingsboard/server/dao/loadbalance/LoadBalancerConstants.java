/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.dao.loadbalance;

public interface LoadBalancerConstants {
    String masterPrefix = "/*MASTER*/ "; // Will be added to regular queries to route the query to the master (findById, etc.).
    String replicaPrefix = "/*REPLICA*/ "; // A query intended for LoadBalancer is expected to startsWith this replicaPrefix. This prefix will be removed from queries to route to a replica
}
