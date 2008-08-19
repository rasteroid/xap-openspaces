/*
 * Copyright 2002-2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openspaces.core;

import com.gigaspaces.async.AsyncFuture;
import com.gigaspaces.async.AsyncFutureListener;
import com.gigaspaces.async.AsyncResultFilter;
import com.gigaspaces.async.AsyncResultsReducer;
import com.gigaspaces.async.FutureFactory;
import com.j_spaces.core.IJSpace;
import com.j_spaces.core.LeaseContext;
import com.j_spaces.core.client.ISpaceProxy;
import com.j_spaces.core.client.Query;
import com.j_spaces.core.client.ReadModifiers;
import net.jini.core.lease.Lease;
import net.jini.core.transaction.Transaction;
import net.jini.space.JavaSpace;
import org.openspaces.core.exception.ExceptionTranslator;
import org.openspaces.core.executor.DistributedTask;
import org.openspaces.core.executor.Task;
import org.openspaces.core.executor.TaskRoutingProvider;
import org.openspaces.core.executor.internal.ExecutorMetaDataProvider;
import org.openspaces.core.executor.internal.InternalDistributedSpaceTaskWrapper;
import org.openspaces.core.executor.internal.InternalSpaceTaskWrapper;
import org.openspaces.core.internal.InternalGigaSpace;
import org.openspaces.core.transaction.TransactionProvider;
import org.openspaces.core.transaction.internal.InternalAsyncFuture;
import org.openspaces.core.transaction.internal.InternalAsyncFutureListener;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionDefinition;

import java.io.Serializable;
import java.rmi.RemoteException;

/**
 * Default implementation of {@link GigaSpace}. Constructed with {@link com.j_spaces.core.IJSpace},
 * {@link org.openspaces.core.transaction.TransactionProvider} and
 * {@link org.openspaces.core.exception.ExceptionTranslator}.
 *
 * <p>Operations are delegated to {@link com.j_spaces.core.IJSpace} with transactions acquired using
 * {@link org.openspaces.core.transaction.TransactionProvider}. Any exceptions thrown during the
 * operations are translated using {@link org.openspaces.core.exception.ExceptionTranslator}.
 *
 * <p>Allows to set default timeouts for read and take operations and default lease for write
 * operation.
 *
 * @author kimchy
 */
public class DefaultGigaSpace implements GigaSpace, InternalGigaSpace {

    final private ISpaceProxy space;

    final private TransactionProvider txProvider;

    final private ExceptionTranslator exTranslator;

    private long defaultReadTimeout = JavaSpace.NO_WAIT;

    private long defaultTakeTimeout = JavaSpace.NO_WAIT;

    private long defaultWriteLease = Lease.FOREVER;

    private int defaultIsolationLevel;

    final private ExecutorMetaDataProvider executorMetaDataProvider = new ExecutorMetaDataProvider();

    /**
     * Constructs a new DefaultGigaSpace implementation.
     *
     * @param space                 The space implementation to delegate operations to
     * @param txProvider            The transaction provider for declarative transaction ex.
     * @param exTranslator          Exception translator to translate low level exceptions into GigaSpaces runtime
     *                              exception
     * @param defaultIsolationLevel The default isolation level for read operations without modifiers. Maps to
     *                              {@link org.springframework.transaction.TransactionDefinition#getIsolationLevel()}
     *                              levels values.
     */
    public DefaultGigaSpace(IJSpace space, TransactionProvider txProvider, ExceptionTranslator exTranslator,
                            int defaultIsolationLevel) {
        this.space = (ISpaceProxy) space;
        this.txProvider = txProvider;
        this.exTranslator = exTranslator;
        // set the default read take modifiers according to the default isolation level
        switch (defaultIsolationLevel) {
            case TransactionDefinition.ISOLATION_DEFAULT:
                this.defaultIsolationLevel = space.getReadModifiers();
                break;
            case TransactionDefinition.ISOLATION_READ_UNCOMMITTED:
                this.defaultIsolationLevel = ReadModifiers.DIRTY_READ;
                break;
            case TransactionDefinition.ISOLATION_READ_COMMITTED:
                this.defaultIsolationLevel = ReadModifiers.READ_COMMITTED;
                break;
            case TransactionDefinition.ISOLATION_REPEATABLE_READ:
                this.defaultIsolationLevel = ReadModifiers.REPEATABLE_READ;
                break;
            case TransactionDefinition.ISOLATION_SERIALIZABLE:
                throw new IllegalArgumentException("GigaSpace does not support serializable isolation level");
        }
    }

    /**
     * Sets the default read timeout when executing {@link #read(Object)} or
     * {@link #readIfExists(Object)} operations.
     */
    public void setDefaultReadTimeout(long defaultReadTimeout) {
        this.defaultReadTimeout = defaultReadTimeout;
    }

    /**
     * Sets the default take timeout when executing {@link #take(Object)} or
     * {@link #takeIfExists(Object)} operations.
     */
    public void setDefaultTakeTimeout(long defaultTakeTimeout) {
        this.defaultTakeTimeout = defaultTakeTimeout;
    }

    /**
     * Sets the default write lease when executing {@link #write(Object)}.
     */
    public void setDefaultWriteLease(long defaultWriteLease) {
        this.defaultWriteLease = defaultWriteLease;
    }

    // GigaSpace interface Methods

    public IJSpace getSpace() {
        return this.space;
    }

    public TransactionProvider getTxProvider() {
        return this.txProvider;
    }

    public ExceptionTranslator getExceptionTranslator() {
        return this.exTranslator;
    }

    public void clean() throws DataAccessException {
        try {
            space.clean();
        } catch (Exception e) {
            throw exTranslator.translate(e);
        }
    }

    public void clear(Object template) throws DataAccessException {
        try {
            space.clear(template, getCurrentTransaction());
        } catch (Exception e) {
            throw exTranslator.translate(e);
        }
    }

    public int count(Object template) throws DataAccessException {
        return count(template, getModifiersForIsolationLevel());
    }

    public int count(Object template, int modifiers) throws DataAccessException {
        try {
            return space.count(template, getCurrentTransaction(), modifiers);
        } catch (Exception e) {
            throw exTranslator.translate(e);
        }
    }

    public Object snapshot(Object entry) throws DataAccessException {
        try {
            return space.snapshot(entry);
        } catch (Exception e) {
            throw exTranslator.translate(e);
        }
    }

    public <T> T read(T template) throws DataAccessException {
        return read(template, defaultReadTimeout);
    }

    public <T> T read(T template, long timeout) throws DataAccessException {
        return read(template, timeout, getModifiersForIsolationLevel());
    }

    @SuppressWarnings("unchecked")
    public <T> T read(T template, long timeout, int modifiers) throws DataAccessException {
        try {
            return (T) space.read(template, getCurrentTransaction(), timeout, modifiers);
        } catch (Exception e) {
            throw exTranslator.translate(e);
        }
    }

    public <T> T read(Query<T> template) throws DataAccessException {
        return read(template, defaultReadTimeout);
    }

    public <T> T read(Query<T> template, long timeout) throws DataAccessException {
        return read(template, timeout, getModifiersForIsolationLevel());
    }

    @SuppressWarnings("unchecked")
    public <T> T read(Query<T> template, long timeout, int modifiers) throws DataAccessException {
        try {
            return (T) space.read(template, getCurrentTransaction(), timeout, modifiers);
        } catch (Exception e) {
            throw exTranslator.translate(e);
        }
    }

    public <T> AsyncFuture<T> asyncRead(T template) throws DataAccessException {
        return asyncRead(template, defaultReadTimeout);
    }

    public <T> AsyncFuture<T> asyncRead(T template, AsyncFutureListener<T> listener) throws DataAccessException {
        return asyncRead(template, defaultReadTimeout, listener);
    }

    public <T> AsyncFuture<T> asyncRead(T template, long timeout) throws DataAccessException {
        return asyncRead(template, timeout, null);
    }

    public <T> AsyncFuture<T> asyncRead(T template, long timeout, AsyncFutureListener<T> listener) throws DataAccessException {
        return asyncRead(template, timeout, getModifiersForIsolationLevel(), listener);
    }

    public <T> AsyncFuture<T> asyncRead(T template, long timeout, int modifiers) throws DataAccessException {
        return asyncRead(template, timeout, modifiers, null);
    }

    public <T> AsyncFuture<T> asyncRead(T template, long timeout, int modifiers, AsyncFutureListener<T> listener) throws DataAccessException {
        Transaction tx = getCurrentTransaction();
        try {
            return wrapFuture(space.asyncRead(template, tx, timeout, modifiers, listener), tx);
        } catch (RemoteException e) {
            throw exTranslator.translate(e);
        }
    }

    public <T> AsyncFuture<T> asyncRead(Query<T> template) throws DataAccessException {
        return asyncRead(template, defaultReadTimeout);
    }

    public <T> AsyncFuture<T> asyncRead(Query<T> template, AsyncFutureListener<T> listener) throws DataAccessException {
        return asyncRead(template, defaultReadTimeout, listener);
    }

    public <T> AsyncFuture<T> asyncRead(Query<T> template, long timeout) throws DataAccessException {
        return asyncRead(template, timeout, (AsyncFutureListener<T>) null);
    }

    public <T> AsyncFuture<T> asyncRead(Query<T> template, long timeout, AsyncFutureListener<T> listener) throws DataAccessException {
        return asyncRead(template, timeout, getModifiersForIsolationLevel(), listener);
    }

    public <T> AsyncFuture<T> asyncRead(Query<T> template, long timeout, int modifiers) throws DataAccessException {
        return asyncRead(template, timeout, modifiers, (AsyncFutureListener<T>) null);
    }

    public <T> AsyncFuture<T> asyncRead(Query<T> template, long timeout, int modifiers, AsyncFutureListener<T> listener) throws DataAccessException {
        Transaction tx = getCurrentTransaction();
        try {
            return wrapFuture(space.asyncRead(template, tx, timeout, modifiers, listener), tx);
        } catch (RemoteException e) {
            throw exTranslator.translate(e);
        }
    }

    public <T> T readIfExists(T template) throws DataAccessException {
        return readIfExists(template, defaultReadTimeout);
    }

    public <T> T readIfExists(T template, long timeout) throws DataAccessException {
        return readIfExists(template, timeout, getModifiersForIsolationLevel());
    }

    @SuppressWarnings("unchecked")
    public <T> T readIfExists(T template, long timeout, int modifiers) throws DataAccessException {
        try {
            return (T) space.readIfExists(template, getCurrentTransaction(), timeout, modifiers);
        } catch (Exception e) {
            throw exTranslator.translate(e);
        }
    }

    public <T> T readIfExists(Query<T> template) throws DataAccessException {
        return readIfExists(template, defaultReadTimeout);
    }

    public <T> T readIfExists(Query<T> template, long timeout) throws DataAccessException {
        return readIfExists(template, timeout, getModifiersForIsolationLevel());
    }

    @SuppressWarnings("unchecked")
    public <T> T readIfExists(Query<T> template, long timeout, int modifiers) throws DataAccessException {
        try {
            return (T) space.readIfExists(template, getCurrentTransaction(), timeout, modifiers);
        } catch (Exception e) {
            throw exTranslator.translate(e);
        }
    }

    public <T> T[] readMultiple(T template, int maxEntries) throws DataAccessException {
        return readMultiple(template, maxEntries, getModifiersForIsolationLevel());
    }

    @SuppressWarnings("unchecked")
    public <T> T[] readMultiple(T template, int maxEntries, int modifiers) throws DataAccessException {
        try {
            return (T[]) space.readMultiple(template, getCurrentTransaction(), maxEntries, modifiers);
        } catch (Exception e) {
            throw exTranslator.translate(e);
        }
    }

    public <T> T[] readMultiple(Query<T> template, int maxEntries) throws DataAccessException {
        return readMultiple(template, maxEntries, getModifiersForIsolationLevel());
    }

    @SuppressWarnings("unchecked")
    public <T> T[] readMultiple(Query<T> template, int maxEntries, int modifiers) throws DataAccessException {
        try {
            return (T[]) space.readMultiple(template, getCurrentTransaction(), maxEntries, modifiers);
        } catch (Exception e) {
            throw exTranslator.translate(e);
        }
    }

    public <T> T take(T template) throws DataAccessException {
        return take(template, defaultTakeTimeout);
    }

    public <T> T take(T template, long timeout) throws DataAccessException {
        return take(template, timeout, 0);
    }

    @SuppressWarnings("unchecked")
    public <T> T take(T template, long timeout, int modifiers) throws DataAccessException {
        try {
            return (T) space.take(template, getCurrentTransaction(), timeout, modifiers);
        } catch (Exception e) {
            throw exTranslator.translate(e);
        }
    }

    public <T> T take(Query<T> template) throws DataAccessException {
        return take(template, defaultTakeTimeout);
    }

    public <T> T take(Query<T> template, long timeout) throws DataAccessException {
        return take(template, timeout, 0);
    }

    @SuppressWarnings("unchecked")
    public <T> T take(Query<T> template, long timeout, int modifiers) throws DataAccessException {
        try {
            return (T) space.take(template, getCurrentTransaction(), timeout, modifiers);
        } catch (Exception e) {
            throw exTranslator.translate(e);
        }
    }

    public <T> AsyncFuture<T> asyncTake(T template) throws DataAccessException {
        return asyncTake(template, defaultTakeTimeout);
    }

    public <T> AsyncFuture<T> asyncTake(T template, AsyncFutureListener<T> listener) throws DataAccessException {
        return asyncTake(template, defaultTakeTimeout, listener);
    }

    public <T> AsyncFuture<T> asyncTake(T template, long timeout) throws DataAccessException {
        return asyncTake(template, timeout, 0);
    }

    public <T> AsyncFuture<T> asyncTake(T template, long timeout, AsyncFutureListener<T> listener) throws DataAccessException {
        return asyncTake(template, timeout, 0, listener);
    }

    public <T> AsyncFuture<T> asyncTake(T template, long timeout, int modifiers) throws DataAccessException {
        return asyncTake(template, timeout, modifiers, null);
    }

    public <T> AsyncFuture<T> asyncTake(T template, long timeout, int modifiers, AsyncFutureListener<T> listener) throws DataAccessException {
        Transaction tx = getCurrentTransaction();
        try {
            return wrapFuture(space.asyncTake(template, tx, timeout, modifiers, listener), tx);
        } catch (RemoteException e) {
            throw exTranslator.translate(e);
        }
    }

    public <T> AsyncFuture<T> asyncTake(Query<T> template) throws DataAccessException {
        return asyncTake(template, defaultTakeTimeout);
    }

    public <T> AsyncFuture<T> asyncTake(Query<T> template, AsyncFutureListener<T> listener) throws DataAccessException {
        return asyncTake(template, defaultTakeTimeout, listener);
    }

    public <T> AsyncFuture<T> asyncTake(Query<T> template, long timeout) throws DataAccessException {
        return asyncTake(template, timeout, 0);
    }

    public <T> AsyncFuture<T> asyncTake(Query<T> template, long timeout, AsyncFutureListener<T> listener) throws DataAccessException {
        return asyncTake(template, timeout, 0, listener);
    }

    public <T> AsyncFuture<T> asyncTake(Query<T> template, long timeout, int modifiers) throws DataAccessException {
        return asyncTake(template, timeout, modifiers, (AsyncFutureListener<T>) null);
    }

    public <T> AsyncFuture<T> asyncTake(Query<T> template, long timeout, int modifiers, AsyncFutureListener<T> listener) throws DataAccessException {
        Transaction tx = getCurrentTransaction();
        try {
            return wrapFuture(space.asyncTake(template, tx, timeout, modifiers, listener), tx);
        } catch (RemoteException e) {
            throw exTranslator.translate(e);
        }
    }

    public <T> T takeIfExists(T template) throws DataAccessException {
        return takeIfExists(template, defaultTakeTimeout);
    }

    @SuppressWarnings("unchecked")
    public <T> T takeIfExists(T template, long timeout) throws DataAccessException {
        try {
            return (T) space.takeIfExists(template, getCurrentTransaction(), timeout);
        } catch (Exception e) {
            throw exTranslator.translate(e);
        }
    }

    public <T> T takeIfExists(Query<T> template) throws DataAccessException {
        return takeIfExists(template, defaultTakeTimeout);
    }

    @SuppressWarnings("unchecked")
    public <T> T takeIfExists(Query<T> template, long timeout) throws DataAccessException {
        try {
            return (T) space.takeIfExists(template, getCurrentTransaction(), timeout);
        } catch (Exception e) {
            throw exTranslator.translate(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T[] takeMultiple(T template, int maxEntries) throws DataAccessException {
        try {
            return (T[]) space.takeMultiple(template, getCurrentTransaction(), maxEntries);
        } catch (Exception e) {
            throw exTranslator.translate(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T[] takeMultiple(Query<T> template, int maxEntries) throws DataAccessException {
        try {
            return (T[]) space.takeMultiple(template, getCurrentTransaction(), maxEntries);
        } catch (Exception e) {
            throw exTranslator.translate(e);
        }
    }

    public <T> LeaseContext<T> write(T entry) throws DataAccessException {
        return write(entry, defaultWriteLease);
    }

    @SuppressWarnings("unchecked")
    public <T> LeaseContext<T> write(T entry, long lease) throws DataAccessException {
        try {
            return space.write(entry, getCurrentTransaction(), lease);
        } catch (Exception e) {
            throw exTranslator.translate(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> LeaseContext<T> write(T entry, long lease, long timeout, int modifiers) throws DataAccessException {
        try {
            return space.write(entry, getCurrentTransaction(), lease, timeout, modifiers);
        } catch (Exception e) {
            throw exTranslator.translate(e);
        }
    }

    public <T> LeaseContext<T>[] writeMultiple(T[] entries) throws DataAccessException {
        return writeMultiple(entries, defaultWriteLease);
    }

    @SuppressWarnings("unchecked")
    public <T> LeaseContext<T>[] writeMultiple(T[] entries, long lease) throws DataAccessException {
        try {
            return (LeaseContext<T>[]) space.writeMultiple(entries, getCurrentTransaction(), lease);
        } catch (Exception e) {
            throw exTranslator.translate(e);
        }
    }

    public <T> Object[] updateMultiple(T[] entries, long[] leases) throws DataAccessException {
        try {
            Object[] retVals = space.updateMultiple(entries, getCurrentTransaction(), leases);
            for (int i = 0; i < retVals.length; i++) {
                if (retVals[i] instanceof Exception) {
                    retVals[i] = exTranslator.translate((Exception) retVals[i]);
                }
            }
            return retVals;
        } catch (Exception e) {
            throw exTranslator.translate(e);
        }
    }

    public <T> Object[] updateMultiple(T[] entries, long[] leases, int updateModifiers) throws DataAccessException {
        try {
            Object[] retVals = space.updateMultiple(entries, getCurrentTransaction(), leases, updateModifiers);
            for (int i = 0; i < retVals.length; i++) {
                if (retVals[i] instanceof Exception) {
                    retVals[i] = exTranslator.translate((Exception) retVals[i]);
                }
            }
            return retVals;
        } catch (Exception e) {
            throw exTranslator.translate(e);
        }
    }

    public IteratorBuilder iterator() {
        return new IteratorBuilder(this);
    }

    public <T extends Serializable> AsyncFuture<T> execute(Task<T> task) {
        return execute(task, (AsyncFutureListener<T>) null);
    }

    public <T extends Serializable> AsyncFuture<T> execute(Task<T> task, AsyncFutureListener<T> listener) {
        Object routing = null;
        if (task instanceof TaskRoutingProvider) {
            Object optionalRouting = executorMetaDataProvider.findRouting(((TaskRoutingProvider) task).getRouting());
            if (optionalRouting != null) {
                routing = optionalRouting;
            }
        }
        if (routing == null) {
            routing = executorMetaDataProvider.findRouting(task);
        }
        return execute(task, routing, listener);
    }

    public <T extends Serializable> AsyncFuture<T> execute(Task<T> task, Object routing) {
        return execute(task, routing, (AsyncFutureListener<T>) null);
    }

    public <T extends Serializable> AsyncFuture<T> execute(Task<T> task, Object routing, AsyncFutureListener<T> listener) {
        if (routing == null) {
            throw new IllegalArgumentException("Task [" + task + "] can not be executed without routing information");
        }
        Object optionalRouting = executorMetaDataProvider.findRouting(routing);
        if (optionalRouting != null) {
            routing = optionalRouting;
        }
        try {
            Transaction tx = getCurrentTransaction();
            return wrapFuture(space.execute(new InternalSpaceTaskWrapper<T>(task, routing), tx, wrapListener(listener, tx)), tx);
        } catch (Exception e) {
            throw exTranslator.translate(e);
        }
    }

    public <T extends Serializable, R> AsyncFuture<R> execute(DistributedTask<T, R> task, Object... routings) {
        AsyncFutureListener<T> listener = null;
        if (routings.length > 0 && routings[routings.length - 1] instanceof AsyncFutureListener) {
            listener = (AsyncFutureListener<T>) routings[routings.length - 1];
        }
        AsyncFuture<T>[] futures = new AsyncFuture[routings.length];
        Transaction tx = getCurrentTransaction();
        for (int i = 0; i < routings.length; i++) {
            try {
                Object routing = routings[i];
                Object optionalRouting = executorMetaDataProvider.findRouting(routing);
                if (optionalRouting != null) {
                    routing = optionalRouting;
                }
                futures[i] = space.execute(new InternalSpaceTaskWrapper<T>(task, routing), tx, wrapListener(listener, tx));
            } catch (Exception e) {
                throw exTranslator.translate(e);
            }
        }
        AsyncFuture<R> result;
        if (task instanceof AsyncResultFilter) {
            result = FutureFactory.create(futures, task, (AsyncResultFilter<T>) task);
        } else {
            result = FutureFactory.create(futures, task);
        }
        return wrapFuture(result, tx);
    }

    public <T extends Serializable, R> AsyncFuture<R> execute(DistributedTask<T, R> task) {
        return execute(task, (AsyncFutureListener<R>) null);
    }

    public <T extends Serializable, R> AsyncFuture<R> execute(DistributedTask<T, R> task, AsyncFutureListener<R> listener) {
        try {
            Transaction tx = getCurrentTransaction();
            return wrapFuture(space.execute(new InternalDistributedSpaceTaskWrapper<T, R>(task), tx, wrapListener(listener, tx)), tx);
        } catch (Exception e) {
            throw exTranslator.translate(e);
        }
    }

    public <T extends Serializable, R> ExecutorBuilder<T, R> executorBuilder(AsyncResultsReducer<T, R> reducer) {
        return new ExecutorBuilder<T, R>(this, reducer);
    }

    // Support methods

    public Transaction getCurrentTransaction() {
        Transaction.Created txCreated = txProvider.getCurrentTransaction(this, space);
        if (txCreated != null) {
            return txCreated.transaction;
        }
        return null;
    }

    /**
     * Gets the isolation level from the current running transaction (enabling the usage of Spring
     * declarative isolation level settings). If there is no transaction in progress or the
     * transaction isolation is
     * {@link org.springframework.transaction.TransactionDefinition#ISOLATION_DEFAULT} will use the
     * default isolation level associated with this class.
     */
    public int getModifiersForIsolationLevel() {
        int isolationLevel = txProvider.getCurrentTransactionIsolationLevel(this);
        if (isolationLevel == TransactionDefinition.ISOLATION_DEFAULT) {
            return defaultIsolationLevel;
        } else if (isolationLevel == TransactionDefinition.ISOLATION_READ_UNCOMMITTED) {
            return ReadModifiers.DIRTY_READ;
        } else if (isolationLevel == TransactionDefinition.ISOLATION_READ_COMMITTED) {
            return ReadModifiers.READ_COMMITTED;
        } else if (isolationLevel == TransactionDefinition.ISOLATION_REPEATABLE_READ) {
            return ReadModifiers.REPEATABLE_READ;
        } else {
            throw new IllegalArgumentException("GigaSpaces does not support isolation level [" + isolationLevel + "]");
        }
    }

    @Override
    public String toString() {
        return space.toString();
    }

    public <T> AsyncFutureListener<T> wrapListener(AsyncFutureListener<T> listener, Transaction tx) {
        if (listener == null) {
            return null;
        }
        if (tx == null) {
            return new InternalAsyncFutureListener<T>(this, listener);
        }
        return InternalAsyncFutureListener.wrapIfNeeded(listener, this);
    }

    public <T> AsyncFuture<T> wrapFuture(AsyncFuture<T> future, Transaction tx) {
        return new InternalAsyncFuture<T>(future, this, tx);
    }
}
