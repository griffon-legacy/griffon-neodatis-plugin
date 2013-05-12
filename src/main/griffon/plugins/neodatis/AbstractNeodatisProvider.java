/*
 * Copyright 2012-2013 the original author or authors.
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

package griffon.plugins.neodatis;

import griffon.util.CallableWithArgs;
import griffon.exceptions.GriffonException;
import groovy.lang.Closure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.neodatis.odb.ODB;

import static griffon.util.GriffonNameUtils.isBlank;

/**
 * @author Andres Almiray
 */
public abstract class AbstractNeodatisProvider implements NeodatisProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractNeodatisProvider.class);
    private static final String DEFAULT = "default";

    public <R> R withOdb(Closure<R> closure) {
        return withOdb(DEFAULT, closure);
    }

    public <R> R withOdb(String databaseName, Closure<R> closure) {
        R result = null;
        if (isBlank(databaseName)) databaseName = DEFAULT;
        if (closure != null) {
            ODB db = getDatabase(databaseName);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Executing statement on database '" + databaseName + "'");
            }
            try {
                result = closure.call(databaseName, db);
                db.commit();
            } catch (Exception e) {
                db.rollback();
                throw new GriffonException(e);
            }
        }
        return result;
    }

    public <R> R withOdb(CallableWithArgs<R> callable) {
        return withOdb(DEFAULT, callable);
    }

    public <R> R withOdb(String databaseName, CallableWithArgs<R> callable) {
        R result = null;
        if (isBlank(databaseName)) databaseName = DEFAULT;
        if (callable != null) {
            ODB db = getDatabase(databaseName);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Executing statement on database '" + databaseName + "'");
            }
            try {
                callable.setArgs(new Object[]{databaseName, db});
                result = callable.call();
                db.commit();
            } catch (Exception e) {
                db.rollback();
                throw new GriffonException(e);
            }
        }
        return result;
    }

    protected abstract ODB getDatabase(String databaseName);
}