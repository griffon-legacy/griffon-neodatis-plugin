/*
 * Copyright 2011-2013 the original author or authors.
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

package griffon.plugins.neodatis

import griffon.core.GriffonApplication
import griffon.util.Environment
import griffon.util.Metadata
import griffon.util.ConfigUtils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.neodatis.odb.*

/**
 * @author Andres Almiray
 */
@Singleton
final class NeodatisConnector {
    private static final String DEFAULT = 'default'
    private static final Logger LOG = LoggerFactory.getLogger(NeodatisConnector)
    private bootstrap

    ConfigObject createConfig(GriffonApplication app) {
        if (!app.config.pluginConfig.neodatis) {
            app.config.pluginConfig.neodatis = ConfigUtils.loadConfigWithI18n('NeodatisConfig')
        }
        app.config.pluginConfig.neodatis
    }

    private ConfigObject narrowConfig(ConfigObject config, String databaseName) {
        if (config.containsKey('database') && databaseName == DEFAULT) {
            return config.database
        } else if (config.containsKey('databases')) {
            return config.databases[databaseName]
        }
        return config
    }

    ODB connect(GriffonApplication app, ConfigObject config, String databaseName = DEFAULT) {
        if (OdbHolder.instance.isDatabaseConnected(databaseName)) {
            return OdbHolder.instance.getDatabase(databaseName)
        }

        config = narrowConfig(config, databaseName)
        app.event('NeodatisConnectStart', [config, databaseName])
        ODB database = startNeodatis(config)
        OdbHolder.instance.setDatabase(databaseName, database)
        bootstrap = app.class.classLoader.loadClass('BootstrapNeodatis').newInstance()
        bootstrap.metaClass.app = app
        resolveNeodatisProvider(app).withOdb { dn, odb -> bootstrap.init(dn, odb) }
        app.event('NeodatisConnectEnd', [databaseName, database])
        database
    }

    void disconnect(GriffonApplication app, ConfigObject config, String databaseName = DEFAULT) {
        if (OdbHolder.instance.isDatabaseConnected(databaseName)) {
            config = narrowConfig(config, databaseName)
            ODB database = OdbHolder.instance.getDatabase(databaseName)
            app.event('NeodatisDisconnectStart', [config, databaseName, database])
            resolveNeodatisProvider(app).withOdb { dn, odb -> bootstrap.destroy(dn, odb) }
            stopNeodatis(config, database)
            app.event('NeodatisDisconnectEnd', [config, databaseName])
            OdbHolder.instance.disconnectDatabase(databaseName)
        }
    }

    NeodatisProvider resolveNeodatisProvider(GriffonApplication app) {
        def neodatisProvider = app.config.neodatisProvider
        if (neodatisProvider instanceof Class) {
            neodatisProvider = neodatisProvider.newInstance()
            app.config.neodatisProvider = neodatisProvider
        } else if (!neodatisProvider) {
            neodatisProvider = DefaultNeodatisProvider.instance
            app.config.neodatisProvider = neodatisProvider
        }
        neodatisProvider
    }

    private ODB startNeodatis(ConfigObject config) {
        boolean isClient = config.client ?: false
        String alias = config.alias ?: 'neodatis.odb'

        NeoDatisConfig neodatisConfig = NeoDatis.getConfig()
        config.config.each { key, value ->
            if (key in ['class', 'metaClass']) return
            try {
                neodatisConfig[key] = value
            } catch(MissingPropertyException mpe) {
                // ignore
            }
        }

        if (isClient) {
            return NeoDatis.openClient(alias, neodatisConfig)
        } else {
            File aliasFile = new File(alias)
            if (!aliasFile.absolute) aliasFile = new File(Metadata.current.getGriffonWorkingDir(), alias)
            aliasFile.parentFile?.mkdirs()
            return NeoDatis.open(aliasFile.absolutePath, neodatisConfig)
        }
    }

    private void stopNeodatis(ConfigObject config, ODB db) {
        boolean isClient = config.client ?: false
        String alias = config.alias ?: 'neodatis/db.odb'

        File aliasFile = new File(alias)
        if (!aliasFile.absolute) aliasFile = new File(Metadata.current.getGriffonWorkingDir(), alias)

        switch(Environment.current) {
            case Environment.DEVELOPMENT:
            case Environment.TEST:
                if (isClient) return
                // Runtime.getRuntime().addShutdownHook {
                    aliasFile.parentFile?.eachFileRecurse { f -> 
                        try { if (f?.exists()) f.delete() }
                        catch(IOException ioe) { /* ignore */ }
                    }
                    try { if (aliasFile?.exists()) aliasFile.delete() }
                    catch(IOException ioe) { /* ignore */ }
                // }
            default:
                db.close()
        }
    }
}