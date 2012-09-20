/*
 * Copyright 2010-2012 the original author or authors.
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

import org.neodatis.odb.ODB
import org.neodatis.odb.Query
import org.neodatis.odb.Objects
import org.neodatis.OrderByConstants
import org.neodatis.odb.core.query.criteria.W

import griffon.core.GriffonClass
import griffon.core.GriffonApplication
import griffon.plugins.neodatis.NeodatisConnector
import griffon.plugins.neodatis.NeodatisEnhancer

/**
 * @author Andres Almiray
 */
class NeodatisGriffonAddon {
    void addonInit(GriffonApplication app) {
        // ODB.metaClass.query = { Closure cls ->
        //    delegate.query(new ClosureQuery(cls))
        // }
        ODB.metaClass.query = { Map params, Class clazz ->
            if(!params) return delegate.query(clazz)
            def criteria = W.and()
            params.each { p, v -> criteria.add(W.equal(p, v)) }
            delegate.query(clazz, criteria)
        }
        Query.metaClass.iterator = {-> 
            delegate.objects().iterator(OrderByConstants.ORDER_BY_ASC)
            /*
            def objects = delegate.objects()
            [next: {-> objects.next() },
             hasNext: {-> objects.hasNext()},
             remove: {-> throw new UnsupportedOperationException("${Objects.class.name} is immutable!")}] as Iterator
            */
        }
        ConfigObject config = NeodatisConnector.instance.createConfig(app)
        NeodatisConnector.instance.connect(app, config)
    }

    void addonPostInit(GriffonApplication app) {
        def types = app.config.griffon?.neodatis?.injectInto ?: ['controller']
        for(String type : types) {
            for(GriffonClass gc : app.artifactManager.getClassesOfType(type)) {
                NeodatisEnhancer.enhance(gc.metaClass)
            }
        }
    }

    Map events = [
        ShutdownStart: { app ->
            ConfigObject config = NeodatisConnector.instance.createConfig(app)
            NeodatisConnector.instance.disconnect(app, config)
        }
    ]
}