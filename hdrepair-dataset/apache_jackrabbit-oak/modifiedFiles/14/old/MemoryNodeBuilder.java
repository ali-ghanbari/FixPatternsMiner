/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.plugins.memory;

import static com.google.common.base.Objects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyList;
import static org.apache.jackrabbit.oak.api.Type.BOOLEAN;
import static org.apache.jackrabbit.oak.api.Type.NAME;
import static org.apache.jackrabbit.oak.api.Type.NAMES;
import static org.apache.jackrabbit.oak.plugins.memory.EmptyNodeState.EMPTY_NODE;

import javax.annotation.Nonnull;

import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.apache.jackrabbit.oak.spi.state.NodeState;

/**
 * In-memory node state builder.
 * <p>
 * A {@code MemoryNodeBuilder} instance tracks uncommitted changes without
 * relying on weak references or requiring hard references on the entire
 * accessed subtree. It does this by relying on {@code MutableNodeState}
 * instances for tracking <em>uncommitted changes</em> and on {@code Head}
 * instances for tracking the connectedness of the builder. A builder keeps
 * a reference to the parent builder and knows its own name, which is used
 * to check for relevant changes in its parent builder and update its state
 * accordingly.
 * <p>
 * A builder is in one of three possible states, which is tracked within
 * its {@code Head} instance:
 * <dl>
 *   <dt><em>unconnected</em></dt>
 *   <dd>
 *     A child builder with no content changes starts in this state.
 *     Before each access the unconnected builder checks its parent for
 *     relevant changes.
 *   </dd>
 *   <dt><em>connected</em></dt>
 *   <dd>
 *     Once a builder is first modified, it switches to the connected state
 *     and records all modification in a shared {@code MutableNodeState}
 *     instance. Before each access the connected builder checks whether its
 *     parents base state has been reset and if so, resets its own base state
 *     accordingly.
 *   </dd>
 *   <dt><em>root</em></dt>
 *   <dd>
 *     Same as the connected state but only the root of the builder hierarchy
 *     can have this state.
 *   </dd>
 * </dl>
 */
public class MemoryNodeBuilder implements NodeBuilder {

    /**
     * Parent builder, or {@code null} for a root builder.
     */
    private final MemoryNodeBuilder parent;

    /**
     * Name of this child node within the parent builder,
     * or {@code null} for a root builder.
     */
    private final String name;

    /**
     * Root builder, or {@code this} for the root builder itself.
     */
    private final MemoryNodeBuilder rootBuilder;

    /**
     * Internal revision counter for the base state of this builder. The counter
     * is incremented in the root builder whenever its base state is reset.
     * Each builder instance has its own copy of this revision counter for
     * quickly checking whether its base state needs updating.
     * @see #reset(org.apache.jackrabbit.oak.spi.state.NodeState)
     * @see #base()
     */
    private long baseRevision;

    /**
     * The base state of this builder, possibly non-existent if this builder
     * represents a new node that didn't yet exist in the base content tree.
     */
    private NodeState base;

    /**
     * Head of this builder. Always use {@link #head()} for accessing to
     * ensure the connected state is correctly updated.
     */
    private Head head;

    /**
     * Creates a new in-memory child builder.
     * @param parent parent builder
     * @param name name of this node
     */
    private MemoryNodeBuilder(MemoryNodeBuilder parent, String name) {
        this.parent = parent;
        this.name = name;
        this.rootBuilder = parent.rootBuilder;
        this.head = new UnconnectedHead();
    }

    /**
     * Creates a new in-memory node state builder rooted at
     * and based on the passed {@code base} state.
     * @param base base state of the new builder
     */
    public MemoryNodeBuilder(@Nonnull NodeState base) {
        this.parent = null;
        this.name = null;
        this.rootBuilder = this;

        // ensure child builder's base is updated on first access
        this.baseRevision = 1;
        this.base = checkNotNull(base);

        this.head = new RootHead();
    }

    /**
     * Update the head of this builder to reflect the actual connected state.
     * @return  head of this builder
     */
    private Head head() {
        Head newHead = head.update();
        if (newHead != head) {
            head = newHead;
        }
        return newHead;
    }

    /**
     * @return  {@code true} iff this is the root builder
     */
    private boolean isRoot() {
        return this == rootBuilder;
    }

    /**
     * Update the base state of this builder by recursively retrieving it
     * from its parent builder.
     * @return  base state of this builder
     */
    @Nonnull
    private NodeState base() {
        if (rootBuilder.baseRevision != baseRevision) {
            base = parent.base().getChildNode(name);
            baseRevision = rootBuilder.baseRevision;
        }
        return base;
    }

    /**
     * Factory method for creating new child state builders. Subclasses may
     * override this method to control the behavior of child state builders.
     * @return new builder
     */
    protected MemoryNodeBuilder createChildBuilder(String name) {
        return new MemoryNodeBuilder(this, name);
    }

    /**
     * Called whenever <em>this</em> node is modified, i.e. a property is
     * added, changed or removed, or a child node is added or removed. Changes
     * inside child nodes or the subtrees below are not reported. The default
     * implementation does nothing, but subclasses may override this method
     * to better track changes.
     */
    protected void updated() {
        // do nothing
    }

    //--------------------------------------------------------< NodeBuilder >---

    @Override
    public NodeState getNodeState() {
        return head().getImmutableNodeState();
    }

    @Override
    public NodeState getBaseState() {
        return base();
    }

    @Override
    public boolean exists() {
        return head().getCurrentNodeState().exists();
    }

    @Override
    public boolean isNew() {
        return !isRoot() && !parent.base().hasChildNode(name) && parent.hasChildNode(name);
    }

    @Override
    public boolean isModified() {
        return head().isModified();
    }

    @Override
    public void reset(NodeState newBase) {
        base = checkNotNull(newBase);
        baseRevision++;
        head().reset();
    }

    @Override
    public long getChildNodeCount() {
        return head().getCurrentNodeState().getChildNodeCount();
    }

    @Override
    public Iterable<String> getChildNodeNames() {
        return head().getCurrentNodeState().getChildNodeNames();
    }

    @Override
    public boolean hasChildNode(String name) {
        return head().getCurrentNodeState().hasChildNode(checkNotNull(name));
    }

    @Override
    public NodeBuilder child(String name) {
        if (hasChildNode(name)) {
            return getChildNode(name);
        } else {
            return setChildNode(name);
        }
    }

    @Override
    public NodeBuilder getChildNode(String name) {
        return createChildBuilder(checkNotNull(name));
    }

    @Override
    public NodeBuilder setChildNode(String name) {
        return setChildNode(checkNotNull(name), EMPTY_NODE);
    }

    @Override
    public NodeBuilder setChildNode(String name, NodeState state) {
        checkState(exists(), "This builder does not exist: " + this.name);
        head().getMutableNodeState().setChildNode(checkNotNull(name), checkNotNull(state));
        MemoryNodeBuilder builder = createChildBuilder(name);
        updated();
        return builder;
    }

    @Override
    public boolean remove() {
        if (exists()) {
            head().getMutableNodeState();  // Make sure the removed node is connected
            parent.head().getMutableNodeState().removeChildNode(name);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public long getPropertyCount() {
        return head().getCurrentNodeState().getPropertyCount();
    }

    @Override
    public Iterable<? extends PropertyState> getProperties() {
        return head().getCurrentNodeState().getProperties();
    }

    @Override
    public boolean hasProperty(String name) {
        return head().getCurrentNodeState().hasProperty(checkNotNull(name));
    }

    @Override
    public PropertyState getProperty(String name) {
        return head().getCurrentNodeState().getProperty(checkNotNull(name));
    }

    @Override
    public boolean getBoolean(String name) {
        PropertyState property = getProperty(name);
        return property != null
                && property.getType() == BOOLEAN
                && property.getValue(BOOLEAN);
    }

    @Override
    public String getName(@Nonnull String name) {
        PropertyState property = getProperty(name);
        if (property != null && property.getType() == NAME) {
            return property.getValue(NAME);
        } else {
            return null;
        }
    }

    @Override
    public Iterable<String> getNames(@Nonnull String name) {
        PropertyState property = getProperty(name);
        if (property != null && property.getType() == NAMES) {
            return property.getValue(NAMES);
        } else {
            return emptyList();
        }
    }

    @Override
    public NodeBuilder setProperty(PropertyState property) {
        checkState(exists(), "This builder does not exist: " + name);
        head().getMutableNodeState().setProperty(checkNotNull(property));
        updated();
        return this;
    }

    @Override
    public <T> NodeBuilder setProperty(String name, T value) {
        setProperty(PropertyStates.createProperty(name, value));
        return this;
    }

    @Override
    public <T> NodeBuilder setProperty(String name, T value, Type<T> type) {
        setProperty(PropertyStates.createProperty(name, value, type));
        return this;
    }

    @Override
    public NodeBuilder removeProperty(String name) {
        checkState(exists(), "This builder does not exist: " + name);
        if (head().getMutableNodeState().removeProperty(checkNotNull(name))) {
            updated();
        }
        return this;
    }

    /**
     * @return path of this builder. For debugging purposes only
     */
    private String getPath() {
        return parent == null ? "/" : getPath(new StringBuilder()).toString();
    }

    private RootHead rootHead() {
        return (RootHead) rootBuilder.head;
    }

    private StringBuilder getPath(StringBuilder parentPath) {
        return parent == null ? parentPath : parent.getPath(parentPath).append('/').append(name);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("path", getPath()).toString();
    }

    //------------------------------------------------------------< Head >---

    /**
     * Subclasses of this base class represent the different states associated
     * builders can have: <em>unconnected</em>, <em>connected</em>, and <em>root</em>.
     * Its methods provide access to the node state being built by this builder.
     */
    private abstract static class Head {

        /**
         * Returns the up-to-date head of the associated builder. In most
         * cases the returned value will be the current head instance, but
         * a different head can be returned if a state transition is needed.
         * The returned value is then used as the new current head of the
         * builder.
         *
         * @return up-to-date head of the associated builder
         */
        public abstract Head update();

        /**
         * Returns the current node state associated with this head. This state
         * is only stable across one method call and must not be passed outside
         * the {@code NodeBuilder} API boundary.
         * @return  current head state.
         */
        public abstract NodeState getCurrentNodeState();

        /**
         * Connects the builder to which this head belongs and all its parents
         * and return the mutable node state associated with this head. This state
         * is only stable across one method call and must not be passed outside
         * the {@code NodeBuilder} API boundary.
         * @return  current head state.
         */
        public abstract MutableNodeState getMutableNodeState();

        /**
         * Returns the current nodes state associated with this head.
         * @return  current head state.
         */
        public abstract NodeState getImmutableNodeState();

        /**
         * Check whether the associated builder represents a modified node, which has
         * either modified properties or removed or added child nodes.
         * @return  {@code true} for a modified node
         */
        public abstract boolean isModified();

        public void reset() {
            throw new IllegalStateException("Cannot reset a non-root builder");
        }
    }

    private class UnconnectedHead extends Head {
        private long revision;
        private NodeState state;

        @Override
        public Head update() {
            long rootRevision = rootHead().revision;
            if (revision != rootRevision) {
                // root revision changed: recursively re-get state from parent
                NodeState parentState = parent.head().getCurrentNodeState();
                NodeState newState = parentState.getChildNode(name);
                if (newState instanceof MutableNodeState) {
                    // transition state to ConnectedHead
                    return new ConnectedHead((MutableNodeState) newState);
                } else {
                    // update to match the latest revision
                    state = newState;
                    revision = rootRevision;
                }
            }
            return this;
        }

        @Override
        public NodeState getCurrentNodeState() {
            return state;
        }

        @Override
        public MutableNodeState getMutableNodeState() {
            // switch to connected state recursively up to the parent
            MutableNodeState parentState = parent.head().getMutableNodeState();
            MutableNodeState state = parentState.getMutableChildNode(name);
            // triggers a head state transition at next access
            return new ConnectedHead(state).getMutableNodeState();
        }

        @Override
        public NodeState getImmutableNodeState() {
            assert !(state instanceof MutableNodeState);
            return state;
        }

        @Override
        public boolean isModified() {
            return false;
        }

        @Override
        public String toString() {
            return toStringHelper(this).add("path", getPath()).toString();
        }
    }

    private class ConnectedHead extends Head {
        protected long revision = rootBuilder.baseRevision;
        protected MutableNodeState state;

        public ConnectedHead(MutableNodeState state) {
            this.state = state;
        }

        @Override
        public Head update() {
            if (revision != rootBuilder.baseRevision) {
                // the root builder's base state has been reset: transition back
                // to unconnected and connect again if necessary.
                return new UnconnectedHead().update();
            } else {
                return this;
            }
        }

        @Override
        public NodeState getCurrentNodeState() {
            return state;
        }

        @Override
        public MutableNodeState getMutableNodeState() {
            // incrementing the root revision triggers unconnected
            // child state to re-get their state on next access
            rootHead().revision++;
            return state;
        }

        @Override
        public NodeState getImmutableNodeState() {
            return state.snapshot();
        }

        @Override
        public boolean isModified() {
            return state.isModified(base());
        }

        @Override
        public String toString() {
            return toStringHelper(this).add("path", getPath()).toString();
        }
    }

    private class RootHead extends ConnectedHead {
        public RootHead() {
            super(new MutableNodeState(base));
            // ensure updating of child builders on first access
            revision = 1;
        }

        @Override
        public Head update() {
            return this;
        }

        @Override
        public final void reset() {
            state = new MutableNodeState(base);
            revision++;
        }
    }

}
