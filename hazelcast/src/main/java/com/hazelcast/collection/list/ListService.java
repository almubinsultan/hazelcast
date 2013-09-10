/*
 * Copyright (c) 2008-2012, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.collection.list;

import com.hazelcast.collection.CollectionContainer;
import com.hazelcast.collection.CollectionService;
import com.hazelcast.collection.txn.TransactionalListProxy;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.spi.NodeEngine;
import com.hazelcast.spi.Operation;
import com.hazelcast.spi.PartitionReplicationEvent;
import com.hazelcast.transaction.impl.TransactionSupport;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @ali 8/29/13
 */
public class ListService extends CollectionService {

    public static final String SERVICE_NAME = "hz:impl:listService";

    private final ConcurrentMap<String, ListContainer> containerMap = new ConcurrentHashMap<String, ListContainer>();

    public ListService(NodeEngine nodeEngine) {
        super(nodeEngine);
    }

    public ListContainer getOrCreateContainer(String name, boolean backup) {
        ListContainer container = containerMap.get(name);
        if (container == null){
            container = new ListContainer(name, nodeEngine, this);
            final ListContainer current = containerMap.putIfAbsent(name, container);
            if (current != null){
                container = current;
            }
        }
        return container;
    }

    public Map<String, ? extends CollectionContainer> getContainerMap() {
        return containerMap;
    }

    public String getServiceName() {
        return SERVICE_NAME;
    }

    public DistributedObject createDistributedObject(Object objectId) {
        return new ListProxyImpl(String.valueOf(objectId), nodeEngine, this);
    }

    public TransactionalListProxy createTransactionalObject(Object id, TransactionSupport transaction) {
        return new TransactionalListProxy(String.valueOf(id), transaction, nodeEngine, this);
    }

    public Operation prepareReplicationOperation(PartitionReplicationEvent event) {
        final int totalBackupCount = 1; //TODO through config
        final Map<String, CollectionContainer> migrationData = getMigrationData(event, totalBackupCount);
        return migrationData.isEmpty() ? null : new ListReplicationOperation(migrationData, event.getPartitionId(), event.getReplicaIndex());
    }
}
