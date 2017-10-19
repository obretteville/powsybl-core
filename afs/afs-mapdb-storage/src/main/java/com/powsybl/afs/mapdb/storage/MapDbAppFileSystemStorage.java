/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.afs.mapdb.storage;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.powsybl.afs.storage.AfsStorageException;
import com.powsybl.afs.storage.AppFileSystemStorage;
import com.powsybl.afs.storage.NodeId;
import com.powsybl.afs.storage.PseudoClass;
import com.powsybl.afs.storage.timeseries.*;
import com.powsybl.commons.datasource.DataSource;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class MapDbAppFileSystemStorage implements AppFileSystemStorage {

    public static MapDbAppFileSystemStorage createHeap(String fileSystemName) {
        DBMaker.Maker maker = DBMaker.heapDB();
        return new MapDbAppFileSystemStorage(fileSystemName, maker, maker::make);
    }

    public static MapDbAppFileSystemStorage createMmapFile(String fileSystemName, File dbFile) {
        DBMaker.Maker maker = DBMaker.fileDB(dbFile);
        return new MapDbAppFileSystemStorage(fileSystemName, maker, () -> maker
                .fileMmapEnable()
                .fileMmapEnableIfSupported()
                .fileMmapPreclearDisable()
                .transactionEnable()
                .make());
    }

    private final DBMaker.Maker maker;

    private final DB db;

    private static final class NamedLink implements Serializable {

        private static final long serialVersionUID = 5645222029377034394L;

        private final NodeId nodeId;

        private final String name;

        private NamedLink(NodeId nodeId, String name) {
            this.nodeId = Objects.requireNonNull(nodeId);
            this.name = Objects.requireNonNull(name);
        }

        @Override
        public int hashCode() {
            return nodeId.hashCode() + name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof NamedLink) {
                NamedLink other = (NamedLink) obj;
                return nodeId.equals(other.nodeId) && name.equals(other.name);
            }
            return false;
        }
    }

    private static final class UnorderedNodeIdPair implements Serializable {

        private static final long serialVersionUID = 5740826508016859275L;

        private final NodeId nodeId1;

        private final NodeId nodeId2;

        private UnorderedNodeIdPair(NodeId nodeId1, NodeId nodeId2) {
            this.nodeId1 = Objects.requireNonNull(nodeId1);
            this.nodeId2 = Objects.requireNonNull(nodeId2);
        }

        @Override
        public int hashCode() {
            return nodeId1.hashCode() + nodeId2.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof UnorderedNodeIdPair) {
                UnorderedNodeIdPair other = (UnorderedNodeIdPair) obj;
                return (nodeId1.equals(other.nodeId1) && nodeId2.equals(other.nodeId2)) ||
                        (nodeId1.equals(other.nodeId2) && nodeId2.equals(other.nodeId1));
            }
            return false;
        }
    }

    private static final class TimeSeriesKey implements Serializable {

        private static final long serialVersionUID = -7403590270598848073L;

        private final NodeId nodeId;

        private final int version;

        private final String timeSeriesName;

        private TimeSeriesKey(NodeId nodeId, int version, String timeSeriesName) {
            this.nodeId = Objects.requireNonNull(nodeId);
            this.version = version;
            this.timeSeriesName = Objects.requireNonNull(timeSeriesName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nodeId, version, timeSeriesName);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TimeSeriesKey) {
                TimeSeriesKey other = (TimeSeriesKey) obj;
                return nodeId.equals(other.nodeId) &&
                        version == other.version &&
                        timeSeriesName.equals(other.timeSeriesName);
            }
            return false;
        }

        @Override
        public String toString() {
            return "TimeSeriesKey(nodeId=" + nodeId + ", " + version + ", " + timeSeriesName + ")";
        }
    }

    private static final class TimeSeriesChunkKey implements Serializable {

        private static final long serialVersionUID = -6118770840872733294L;

        private final TimeSeriesKey timeSeriesKey;

        private final int chunk;

        private TimeSeriesChunkKey(TimeSeriesKey timeSeriesKey, int chunk) {
            this.timeSeriesKey = Objects.requireNonNull(timeSeriesKey);
            this.chunk = chunk;
        }

        @Override
        public int hashCode() {
            return Objects.hash(timeSeriesKey, chunk);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TimeSeriesChunkKey) {
                TimeSeriesChunkKey other = (TimeSeriesChunkKey) obj;
                return timeSeriesKey.equals(other.timeSeriesKey) &&
                        chunk == other.chunk;
            }
            return false;
        }

        @Override
        public String toString() {
            return "TimeSeriesChunkKey(key=" + timeSeriesKey + ", chunk=" + chunk + ")";
        }
    }

    private final ConcurrentMap<String, NodeId> rootNodeMap;

    private final ConcurrentMap<NodeId, List<NodeId>> childNodesMap;

    private final ConcurrentMap<NamedLink, NodeId> childNodeMap;

    private final ConcurrentMap<NodeId, NodeId> parentNodeMap;

    private final ConcurrentMap<NodeId, String> nodeNameMap;

    private final ConcurrentMap<NodeId, String> nodePseudoClassMap;

    private final ConcurrentMap<NamedLink, String> stringAttributeMap;

    private final ConcurrentMap<NamedLink, Integer> integerAttributeMap;

    private final ConcurrentMap<NamedLink, Float> floatAttributeMap;

    private final ConcurrentMap<NamedLink, Double> doubleAttributeMap;

    private final ConcurrentMap<NamedLink, Boolean> booleanAttributeMap;

    private final ConcurrentMap<NodeId, Set<String>> attributesMap;

    private final ConcurrentMap<NodeId, NodeId> projectRootNodeMap;

    private final ConcurrentMap<MapDbDataSource.Key, byte[]> dataSourceAttributeDataMap;

    private final ConcurrentMap<String, byte[]> dataSourceAttributeData2Map;

    private final ConcurrentMap<NodeId, Set<String>> timeSeriesNamesMap;

    private final ConcurrentMap<NamedLink, TimeSeriesMetadata> timeSeriesMetadataMap;

    private final ConcurrentMap<TimeSeriesKey, Integer> timeSeriesLastChunkMap;

    private final ConcurrentMap<TimeSeriesChunkKey, DoubleArrayChunk> doubleTimeSeriesChunksMap;

    private final ConcurrentMap<TimeSeriesChunkKey, StringArrayChunk> stringTimeSeriesChunksMap;

    private final ConcurrentMap<NodeId, List<NodeId>> dependencyNodesMap;

    private final ConcurrentMap<NamedLink, NodeId> dependencyNodeMap;

    private final ConcurrentMap<UnorderedNodeIdPair, String> dependencyNameMap;

    private final ConcurrentMap<NodeId, List<NodeId>> backwardDependencyNodesMap;

    private final ConcurrentMap<NamedLink, byte[]> cacheMap;

    protected MapDbAppFileSystemStorage(String fileSystemName, DBMaker.Maker maker, Supplier<DB> db) {
        this.maker = Objects.requireNonNull(maker);
        this.db = db.get();

        rootNodeMap = this.db
                .hashMap("rootNode", Serializer.STRING, Serializer.JAVA)
                .createOrOpen();

        childNodesMap = this.db
                .hashMap("childNodes", Serializer.JAVA, Serializer.JAVA)
                .createOrOpen();

        childNodeMap = this.db
                .hashMap("childNode", Serializer.JAVA, Serializer.JAVA)
                .createOrOpen();

        parentNodeMap = this.db
                .hashMap("parentNode", Serializer.JAVA, Serializer.JAVA)
                .createOrOpen();

        nodeNameMap = this.db
                .hashMap("nodeName", Serializer.JAVA, Serializer.JAVA)
                .createOrOpen();

        nodePseudoClassMap = this.db
                .hashMap("nodePseudoClass", Serializer.JAVA, Serializer.STRING)
                .createOrOpen();

        stringAttributeMap = this.db
                .hashMap("stringAttribute", Serializer.JAVA, Serializer.STRING)
                .createOrOpen();

        integerAttributeMap = this.db
                .hashMap("integerAttribute", Serializer.JAVA, Serializer.INTEGER)
                .createOrOpen();

        floatAttributeMap = this.db
                .hashMap("floatAttribute", Serializer.JAVA, Serializer.FLOAT)
                .createOrOpen();

        doubleAttributeMap = this.db
                .hashMap("doubleAttribute", Serializer.JAVA, Serializer.DOUBLE)
                .createOrOpen();

        booleanAttributeMap = this.db
                .hashMap("booleanAttribute", Serializer.JAVA, Serializer.BOOLEAN)
                .createOrOpen();

        attributesMap = this.db
                .hashMap("attributes", Serializer.JAVA, Serializer.JAVA)
                .createOrOpen();

        projectRootNodeMap = this.db
                .hashMap("projectRootNode", Serializer.JAVA, Serializer.JAVA)
                .createOrOpen();

        dataSourceAttributeDataMap = this.db
                .hashMap("dataSourceAttributeData", Serializer.JAVA, Serializer.BYTE_ARRAY)
                .createOrOpen();

        dataSourceAttributeData2Map = this.db
                .hashMap("dataSourceAttributeData2", Serializer.STRING, Serializer.BYTE_ARRAY)
                .createOrOpen();

        timeSeriesNamesMap = this.db
                .hashMap("timeSeriesNamesMap", Serializer.JAVA, Serializer.JAVA)
                .createOrOpen();

        timeSeriesMetadataMap = this.db
                .hashMap("timeSeriesMetadataMap", Serializer.JAVA, Serializer.JAVA)
                .createOrOpen();

        timeSeriesLastChunkMap = this.db
                .hashMap("timeSeriesLastChunkMap", Serializer.JAVA, Serializer.INTEGER)
                .createOrOpen();

        doubleTimeSeriesChunksMap = this.db
                .hashMap("doubleTimeSeriesChunksMap", Serializer.JAVA, Serializer.JAVA)
                .createOrOpen();

        stringTimeSeriesChunksMap = this.db
                .hashMap("stringTimeSeriesChunksMap", Serializer.JAVA, Serializer.JAVA)
                .createOrOpen();

        dependencyNodesMap = this.db
                .hashMap("dependencyNodes", Serializer.JAVA, Serializer.JAVA)
                .createOrOpen();

        dependencyNodeMap = this.db
                .hashMap("dependencyNode", Serializer.JAVA, Serializer.JAVA)
                .createOrOpen();

        dependencyNameMap = this.db
                .hashMap("dependencyName", Serializer.JAVA, Serializer.STRING)
                .createOrOpen();

        backwardDependencyNodesMap = this.db
                .hashMap("backwardDependencyNodes", Serializer.JAVA, Serializer.JAVA)
                .createOrOpen();

        cacheMap = this.db
                .hashMap("cache", Serializer.JAVA, Serializer.BYTE_ARRAY)
                .createOrOpen();

        // create root node
        if (rootNodeMap.isEmpty()) {
            NodeId rootNodeId = createNode(null, fileSystemName, PseudoClass.FOLDER_PSEUDO_CLASS);
            rootNodeMap.put("rootNode", rootNodeId);
        }
    }

    private static List<NodeId> remove(List<NodeId> nodeIds, NodeId nodeId) {
        List<NodeId> newNodeIds = new ArrayList<>(nodeIds);
        newNodeIds.remove(nodeId);
        return newNodeIds;
    }

    private static Set<String> remove(Set<String> strings, String string) {
        Set<String> newStrings = new HashSet<>(strings);
        newStrings.remove(string);
        return newStrings;
    }

    private static List<NodeId> add(List<NodeId> nodeIds, NodeId nodeId) {
        return ImmutableList.<NodeId>builder()
                .addAll(nodeIds)
                .add(nodeId)
                .build();
    }

    private static Set<String> add(Set<String> strings, String string) {
        return ImmutableSet.<String>builder()
                .addAll(strings)
                .add(string)
                .build();
    }

    @Override
    public NodeId fromString(String str) {
        return new UuidNodeId(UUID.fromString(str));
    }

    @Override
    public NodeId getRootNode() {
        return rootNodeMap.get("rootNode");
    }

    private AfsStorageException createNodeNotFoundException(NodeId nodeId) {
        return new AfsStorageException("Node " + nodeId + " not found");
    }

    @Override
    public String getNodeName(NodeId nodeId) {
        Objects.requireNonNull(nodeId);
        String name = nodeNameMap.get(nodeId);
        if (name == null) {
            throw createNodeNotFoundException(nodeId);
        }
        return name;
    }

    @Override
    public List<NodeId> getChildNodes(NodeId nodeId) {
        Objects.requireNonNull(nodeId);
        List<NodeId> childNodes = childNodesMap.get(nodeId);
        if (childNodes == null) {
            throw createNodeNotFoundException(nodeId);
        }
        return childNodes;
    }

    @Override
    public NodeId getChildNode(NodeId parentNodeId, String name) {
        Objects.requireNonNull(parentNodeId);
        Objects.requireNonNull(name);
        if (!nodeNameMap.containsKey(parentNodeId)) {
            throw new AfsStorageException("Parent node " + parentNodeId + " not found");
        }
        return childNodeMap.get(new NamedLink(parentNodeId, name));
    }

    @Override
    public NodeId getParentNode(NodeId nodeId) {
        Objects.requireNonNull(nodeId);
        if (!nodeNameMap.containsKey(nodeId)) {
            throw createNodeNotFoundException(nodeId);
        }
        return parentNodeMap.get(nodeId);
    }

    @Override
    public void setParentNode(NodeId nodeId, NodeId newParentNodeId) {
        Objects.requireNonNull(nodeId);
        Objects.requireNonNull(newParentNodeId);
        if (!nodeNameMap.containsKey(nodeId)) {
            throw createNodeNotFoundException(nodeId);
        }
        if (!nodeNameMap.containsKey(newParentNodeId)) {
            throw new AfsStorageException("New parent node " + newParentNodeId + " not found");
        }
        NodeId oldParentNodeId = parentNodeMap.get(nodeId);
        if (oldParentNodeId == null) {
            throw new AfsStorageException("Cannot change parent of root folder");
        }

        parentNodeMap.put(nodeId, newParentNodeId);

        // remove from old parent
        String name = nodeNameMap.get(nodeId);
        childNodeMap.remove(new NamedLink(oldParentNodeId, name));
        childNodesMap.put(oldParentNodeId, remove(childNodesMap.get(oldParentNodeId), nodeId));

        // add to new parent
        childNodesMap.put(newParentNodeId, add(childNodesMap.get(newParentNodeId), nodeId));
        childNodeMap.put(new NamedLink(newParentNodeId, name), nodeId);
    }

    @Override
    public boolean isWritable(NodeId nodeId) {
        return true;
    }

    @Override
    public NodeId createNode(NodeId parentNodeId, String name, String nodePseudoClass) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(nodePseudoClass);
        if (parentNodeId != null && !nodeNameMap.containsKey(parentNodeId)) {
            throw new AfsStorageException("Parent node " + parentNodeId + " not found");
        }
        NodeId nodeId = UuidNodeId.generate();
        nodeNameMap.put(nodeId, name);
        nodePseudoClassMap.put(nodeId, nodePseudoClass);
        attributesMap.put(nodeId, Collections.emptySet());
        childNodesMap.put(nodeId, Collections.emptyList());
        if (parentNodeId != null) {
            parentNodeMap.put(nodeId, parentNodeId);
            childNodesMap.put(parentNodeId, add(childNodesMap.get(parentNodeId), nodeId));
            childNodeMap.put(new NamedLink(parentNodeId, name), nodeId);
        }
        if (nodePseudoClass.equals(PseudoClass.PROJECT_PSEUDO_CLASS)) {
            // create root project folder
            NodeId projectRootNodeId = createNode(null, "root", PseudoClass.PROJECT_FOLDER_PSEUDO_CLASS);
            projectRootNodeMap.put(nodeId, projectRootNodeId);
        }
        dependencyNodesMap.put(nodeId, Collections.emptyList());
        backwardDependencyNodesMap.put(nodeId, Collections.emptyList());
        return nodeId;
    }

    @Override
    public String getNodePseudoClass(NodeId nodeId) {
        Objects.requireNonNull(nodeId);
        String nodePseudoClass = nodePseudoClassMap.get(nodeId);
        if (nodePseudoClass == null) {
            throw createNodeNotFoundException(nodeId);
        }
        return nodePseudoClass;
    }

    @Override
    public void deleteNode(NodeId nodeId) {
        Objects.requireNonNull(nodeId);
        if (!childNodesMap.get(nodeId).isEmpty()) {
            throw new AfsStorageException("Cannot delete a node with children, remove children before");
        }
        if (!getBackwardDependencies(nodeId).isEmpty()) {
            throw new AfsStorageException("Cannot delete a node that is a dependency of another node");
        }
        String name = nodeNameMap.remove(nodeId);
        String nodePseudoClass = nodePseudoClassMap.remove(nodeId);
        for (String attributeName : attributesMap.get(nodeId)) {
            NamedLink namedLink = new NamedLink(nodeId, attributeName);
            stringAttributeMap.remove(namedLink);
            integerAttributeMap.remove(namedLink);
            floatAttributeMap.remove(namedLink);
            doubleAttributeMap.remove(namedLink);
            booleanAttributeMap.remove(namedLink);
        }
        attributesMap.remove(nodeId);
        childNodesMap.remove(nodeId);
        NodeId parentNodeId = parentNodeMap.remove(nodeId);
        if (parentNodeId != null) {
            childNodesMap.put(parentNodeId, remove(childNodesMap.get(parentNodeId), nodeId));
            childNodeMap.remove(new NamedLink(parentNodeId, name));
        }
        if (nodePseudoClass.equals(PseudoClass.PROJECT_PSEUDO_CLASS)) {
            // also remove everything inside the project
            throw new UnsupportedOperationException("TODO"); // TODO
        }
        for (NodeId toNodeId : getDependencies(nodeId)) {
            String dependencyName = dependencyNameMap.remove(new UnorderedNodeIdPair(nodeId, toNodeId));
            dependencyNodeMap.remove(new NamedLink(nodeId, dependencyName));
            backwardDependencyNodesMap.put(toNodeId, remove(backwardDependencyNodesMap.get(toNodeId), nodeId));
        }
        dependencyNodesMap.remove(nodeId);
    }

    private <T> T getAttribute(ConcurrentMap<NamedLink, T> map, NodeId nodeId, String name) {
        Objects.requireNonNull(nodeId);
        Objects.requireNonNull(name);
        if (!nodeNameMap.containsKey(nodeId)) {
            throw createNodeNotFoundException(nodeId);
        }
        return map.get(new NamedLink(nodeId, name));
    }

    private <T> void setAttribute(ConcurrentMap<NamedLink, T> map, NodeId nodeId, String name, T value) {
        Objects.requireNonNull(nodeId);
        Objects.requireNonNull(name);
        if (!nodeNameMap.containsKey(nodeId)) {
            throw createNodeNotFoundException(nodeId);
        }
        NamedLink namedLink = new NamedLink(nodeId, name);
        if (value == null) {
            map.remove(namedLink);
            attributesMap.put(nodeId, remove(attributesMap.get(nodeId), name));
        } else {
            map.put(namedLink, value);
            attributesMap.put(nodeId, add(attributesMap.get(nodeId), name));
        }
    }

    @Override
    public String getStringAttribute(NodeId nodeId, String name) {
        return getAttribute(stringAttributeMap, nodeId, name);
    }

    @Override
    public void setStringAttribute(NodeId nodeId, String name, String value) {
        setAttribute(stringAttributeMap, nodeId, name, value);
    }

    @Override
    public OptionalInt getIntAttribute(NodeId nodeId, String name) {
        Integer i = getAttribute(integerAttributeMap, nodeId, name);
        return i == null ? OptionalInt.empty() : OptionalInt.of(i);
    }

    @Override
    public void setIntAttribute(NodeId nodeId, String name, int value) {
        setAttribute(integerAttributeMap, nodeId, name, value);
    }

    @Override
    public OptionalDouble getDoubleAttribute(NodeId nodeId, String name) {
        Double d = getAttribute(doubleAttributeMap, nodeId, name);
        return d == null ? OptionalDouble.empty() : OptionalDouble.of(d);
    }

    @Override
    public void setDoubleAttribute(NodeId nodeId, String name, double value) {
        setAttribute(doubleAttributeMap, nodeId, name, value);
    }

    @Override
    public Optional<Boolean> getBooleanAttribute(NodeId nodeId, String name) {
        Boolean b = getAttribute(booleanAttributeMap, nodeId, name);
        return b == null ? Optional.empty() : Optional.of(b);
    }

    @Override
    public void setBooleanAttribute(NodeId nodeId, String name, boolean value) {
        setAttribute(booleanAttributeMap, nodeId, name, value);
    }

    @Override
    public Reader readStringAttribute(NodeId nodeId, String name) {
        String value = getStringAttribute(nodeId, name);
        return value != null ? new StringReader(value) : null;
    }

    @Override
    public Writer writeStringAttribute(NodeId nodeId, String name) {
        Objects.requireNonNull(nodeId);
        Objects.requireNonNull(name);
        return new StringWriter() {
            @Override
            public void close() throws IOException {
                super.close();
                setStringAttribute(nodeId, name, toString());
            }
        };
    }

    @Override
    public DataSource getDataSourceAttribute(NodeId nodeId, String name) {
        return new MapDbDataSource(nodeId, name, dataSourceAttributeDataMap, dataSourceAttributeData2Map);
    }

    @Override
    public void createTimeSeries(NodeId nodeId, TimeSeriesMetadata metadata) {
        Objects.requireNonNull(nodeId);
        Objects.requireNonNull(metadata);
        if (!nodeNameMap.containsKey(nodeId)) {
            throw createNodeNotFoundException(nodeId);
        }
        Set<String> names = timeSeriesNamesMap.get(nodeId);
        if (names == null) {
            names = new HashSet<>();
        }
        if (names.contains(metadata.getName())) {
            throw new AfsStorageException("Time series " + metadata.getName() + " already exists at node " + nodeId);
        }
        timeSeriesNamesMap.put(nodeId, add(names, metadata.getName()));
        timeSeriesMetadataMap.put(new NamedLink(nodeId, metadata.getName()), metadata);
    }

    @Override
    public Set<String> getTimeSeriesNames(NodeId nodeId) {
        Objects.requireNonNull(nodeId);
        Set<String> names = timeSeriesNamesMap.get(nodeId);
        if (names == null) {
            throw createNodeNotFoundException(nodeId);
        }
        return names;
    }

    private static AfsStorageException createTimeSeriesNotFoundAtNode(String timeSeriesName, NodeId nodeId) {
        return new AfsStorageException("Time series " + timeSeriesName + " not found at node " + nodeId);
    }

    @Override
    public List<TimeSeriesMetadata> getTimeSeriesMetadata(NodeId nodeId, Set<String> timeSeriesNames) {
        Objects.requireNonNull(nodeId);
        Objects.requireNonNull(timeSeriesNames);
        List<TimeSeriesMetadata> metadataList = new ArrayList<>();
        for (String timeSeriesName : timeSeriesNames) {
            TimeSeriesMetadata metadata = timeSeriesMetadataMap.get(new NamedLink(nodeId, timeSeriesName));
            if (metadata == null) {
                throw createTimeSeriesNotFoundAtNode(timeSeriesName, nodeId);
            }
            metadataList.add(metadata);
        }
        return metadataList;
    }

    private static void checkVersion(int version) {
        if (version < 0) {
            throw new IllegalArgumentException("Bad version " + version);
        }
    }

    private <P extends AbstractPoint, C extends ArrayChunk<P>, T extends TimeSeries<P>> List<T> getTimeSeries(NodeId nodeId,
                                                                                                              Set<String> timeSeriesNames,
                                                                                                              int version,
                                                                                                              ConcurrentMap<TimeSeriesChunkKey, C> map,
                                                                                                              BiFunction<TimeSeriesMetadata, List<C>, T> constr) {
        Objects.requireNonNull(nodeId);
        Objects.requireNonNull(timeSeriesNames);
        checkVersion(version);
        Objects.requireNonNull(map);
        Objects.requireNonNull(constr);
        List<T> timeSeriesList = new ArrayList<>();
        for (String timeSeriesName : timeSeriesNames) {
            TimeSeriesMetadata metadata = timeSeriesMetadataMap.get(new NamedLink(nodeId, timeSeriesName));
            if (metadata == null) {
                throw createTimeSeriesNotFoundAtNode(timeSeriesName, nodeId);
            }
            TimeSeriesKey key = new TimeSeriesKey(nodeId, version, timeSeriesName);
            Integer lastChunkNum = timeSeriesLastChunkMap.get(key);
            if (lastChunkNum != null) {
                List<C> chunks = new ArrayList<>(lastChunkNum + 1);
                for (int chunkNum = 0; chunkNum <= lastChunkNum; chunkNum++) {
                    C chunk = map.get(new TimeSeriesChunkKey(key, chunkNum));
                    if (chunk == null) {
                        throw new AssertionError("chunk is null");
                    }
                    chunks.add(chunk);
                }
                timeSeriesList.add(constr.apply(metadata, chunks));
            }
        }
        return timeSeriesList;
    }

    private <P extends AbstractPoint, C extends ArrayChunk<P>> void addTimeSeriesData(NodeId nodeId,
                                                                                      int version,
                                                                                      String timeSeriesName,
                                                                                      List<C> chunks,
                                                                                      ConcurrentMap<TimeSeriesChunkKey, C> map) {
        Objects.requireNonNull(nodeId);
        checkVersion(version);
        Objects.requireNonNull(timeSeriesName);
        Objects.requireNonNull(chunks);
        Objects.requireNonNull(map);
        for (C chunk : chunks) {
            TimeSeriesKey key = new TimeSeriesKey(nodeId, version, timeSeriesName);
            Integer lastNum = timeSeriesLastChunkMap.get(key);
            int num;
            if (lastNum == null) {
                num = 0;
            } else {
                num = lastNum + 1;
            }
            timeSeriesLastChunkMap.put(key, num);
            map.put(new TimeSeriesChunkKey(key, num), chunk);
        }
    }

    @Override
    public List<DoubleTimeSeries> getDoubleTimeSeries(NodeId nodeId, Set<String> timeSeriesNames, int version) {
        return getTimeSeries(nodeId, timeSeriesNames, version, doubleTimeSeriesChunksMap, StoredDoubleTimeSeries::new);
    }

    @Override
    public void addDoubleTimeSeriesData(NodeId nodeId, int version, String timeSeriesName, List<DoubleArrayChunk> chunks) {
        addTimeSeriesData(nodeId, version, timeSeriesName, chunks, doubleTimeSeriesChunksMap);
    }

    @Override
    public List<StringTimeSeries> getStringTimeSeries(NodeId nodeId, Set<String> timeSeriesNames, int version) {
        return getTimeSeries(nodeId, timeSeriesNames, version, stringTimeSeriesChunksMap, StringTimeSeries::new);
    }

    @Override
    public void addStringTimeSeriesData(NodeId nodeId, int version, String timeSeriesName, List<StringArrayChunk> chunks) {
        addTimeSeriesData(nodeId, version, timeSeriesName, chunks, stringTimeSeriesChunksMap);
    }

    @Override
    public void removeAllTimeSeries(NodeId nodeId) {
        Objects.requireNonNull(nodeId);
        Set<String> names = timeSeriesNamesMap.get(nodeId);
        if (names != null) {
            for (String name : names) {
                timeSeriesMetadataMap.remove(new NamedLink(nodeId, name));
            }
            timeSeriesNamesMap.remove(nodeId);
        }
    }

    @Override
    public NodeId getProjectRootNode(NodeId projectNodeId) {
        Objects.requireNonNull(projectNodeId);
        NodeId projectRootNodeId = projectRootNodeMap.get(projectNodeId);
        if (projectRootNodeId == null) {
            throw createNodeNotFoundException(projectNodeId);
        }
        return projectRootNodeId;
    }

    @Override
    public NodeId getDependency(NodeId nodeId, String name) {
        Objects.requireNonNull(nodeId);
        Objects.requireNonNull(name);
        if (!nodeNameMap.containsKey(nodeId)) {
            throw createNodeNotFoundException(nodeId);
        }
        return dependencyNodeMap.get(new NamedLink(nodeId, name));
    }

    @Override
    public void addDependency(NodeId nodeId, String name, NodeId toNodeId) {
        Objects.requireNonNull(nodeId);
        Objects.requireNonNull(name);
        Objects.requireNonNull(toNodeId);
        if (!nodeNameMap.containsKey(nodeId)) {
            throw createNodeNotFoundException(nodeId);
        }
        if (!nodeNameMap.containsKey(toNodeId)) {
            throw createNodeNotFoundException(toNodeId);
        }
        dependencyNodesMap.put(nodeId, add(dependencyNodesMap.get(nodeId), toNodeId));
        dependencyNodeMap.put(new NamedLink(nodeId, name), toNodeId);
        dependencyNameMap.put(new UnorderedNodeIdPair(nodeId, toNodeId), name);
        backwardDependencyNodesMap.put(toNodeId, ImmutableList.<NodeId>builder()
                .addAll(backwardDependencyNodesMap.get(toNodeId))
                .add(nodeId)
                .build());
    }

    @Override
    public List<NodeId> getDependencies(NodeId nodeId) {
        Objects.requireNonNull(nodeId);
        List<NodeId> dependencyNodeIds = dependencyNodesMap.get(nodeId);
        if (dependencyNodeIds == null) {
            throw createNodeNotFoundException(nodeId);
        }
        return dependencyNodeIds;
    }

    @Override
    public List<NodeId> getBackwardDependencies(NodeId nodeId) {
        Objects.requireNonNull(nodeId);
        List<NodeId> backwardDependencyNodeIds = backwardDependencyNodesMap.get(nodeId);
        if (backwardDependencyNodeIds == null) {
            throw createNodeNotFoundException(nodeId);
        }
        return backwardDependencyNodeIds;
    }

    @Override
    public InputStream readFromCache(NodeId nodeId, String key) {
        byte[] value = cacheMap.get(new NamedLink(nodeId, key));
        return value != null ? new ByteArrayInputStream(value) : null;
    }

    @Override
    public OutputStream writeToCache(NodeId nodeId, String key) {
        return new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                super.close();
                cacheMap.put(new NamedLink(nodeId, key), toByteArray());
            }
        };
    }

    @Override
    public void invalidateCache(NodeId nodeId, String key) {
        cacheMap.remove(new NamedLink(nodeId, key));
    }

    @Override
    public void invalidateCache() {
        cacheMap.clear();
    }

    @Override
    public void flush() {
        db.commit();
    }

    @Override
    public void close() {
        db.commit();
        db.close();
    }
}
