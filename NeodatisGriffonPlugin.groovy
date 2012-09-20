/*
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Andres Almiray
 */
class NeodatisGriffonPlugin {
    // the plugin version
    String version = '0.6'
    // the version or versions of Griffon the plugin is designed for
    String griffonVersion = '1.1.0 > *'
    // the other plugins this plugin depends on
    Map dependsOn = [:]
    // resources that are included in plugin packaging
    List pluginIncludes = []
    // the plugin license
    String license = 'Apache Software License 2.0'
    // Toolkit compatibility. No value means compatible with all
    // Valid values are: swing, javafx, swt, pivot, gtk
    List toolkits = []
    // Platform compatibility. No value means compatible with all
    // Valid values are:
    // linux, linux64, windows, windows64, macosx, macosx64, solaris
    List platforms = []
    // URL where documentation can be found
    String documentation = ''
    // URL where source can be found
    String source = 'https://github.com/griffon/griffon-neodatis-plugin'

    List authors = [
        [
            name: 'Andres Almiray',
            email: 'aalmiray@yahoo.com'
        ]
    ]
    String title = 'Neodatis support'
    String description = '''
The Neodatis plugin enables lightweight access to [Neodatis][1] databases.
This plugin does NOT provide domain classes nor dynamic finders like GORM does.

Usage
-----
Upon installation the plugin will generate the following artifacts in `$appdir/griffon-app/conf`:

 * NeodatisConfig.groovy - contains the database definitions.
 * BootstrapNeodatis.groovy - defines init/destroy hooks for data to be manipulated during app startup/shutdown.

A new dynamic method named `withOdb` will be injected into all controllers,
giving you access to a `org.neodatis.odb.ODB` object, with which you'll be able
to make calls to the database. Remember to make all database calls off the EDT
otherwise your application may appear unresponsive when doing long computations
inside the EDT.

This method is aware of multiple databases. If no databaseName is specified when calling
it then the default database will be selected. Here are two example usages, the first
queries against the default database while the second queries a database whose name has
been configured as 'internal'

    package sample
    class SampleController {
        def queryAllDatabases = {
            withOdb { databaseName, odb -> ... }
            withOdb('internal') { databaseName, odb -> ... }
        }
    }

This method is also accessible to any component through the singleton `griffon.plugins.neodatis.NeodatisConnector`.
You can inject these methods to non-artifacts via metaclasses. Simply grab hold of a particular metaclass and call
`NeodatisEnhancer.enhance(metaClassInstance, neodatisProviderInstance)`.

Configuration
-------------
### Dynamic method injection

The `withOdb()` dynamic method will be added to controllers by default. You can
change this setting by adding a configuration flag in `griffon-app/conf/Config.groovy`

    griffon.neodatis.injectInto = ['controller', 'service']

### Events

The following events will be triggered by this addon

 * NeodatisConnectStart[config, databaseName] - triggered before connecting to the database
 * NeodatisConnectEnd[databaseName, odb] - triggered after connecting to the database
 * NeodatisDisconnectStart[config, databaseName, odb] - triggered before disconnecting from the database
 * NeodatisDisconnectEnd[config, databaseName] - triggered after disconnecting from the database

### Multiple Stores

The config file `NeodatisConfig.groovy` defines a default database block. As the name
implies this is the database used by default, however you can configure named databases
by adding a new config block. For example connecting to a database whose name is 'internal'
can be done in this way

    databases {
        internal {
            client = false
            config { }
            alias = 'neodatis/internal'
        }
    }

This block can be used inside the `environments()` block in the same way as the
default database block is used.

### Example

A trivial sample application can be found at [https://github.com/aalmiray/griffon_sample_apps/tree/master/persistence/neodatis][2]

Testing
-------
The `withOdb()` dynamic method will not be automatically injected during unit testing, because addons are simply not initialized
for this kind of tests. However you can use `NeodatisEnhancer.enhance(metaClassInstance, neodatisProviderInstance)` where 
`neodatisProviderInstance` is of type `griffon.plugins.neodatis.NeodatisProvider`. The contract for this interface looks like this

    public interface NeodatisProvider {
        Object withOdb(Closure closure);
        Object withOdb(String serverName, Closure closure);
        <T> T withOdb(CallableWithArgs<T> callable);
        <T> T withOdb(String serverName, CallableWithArgs<T> callable);
    }

It's up to you define how these methods need to be implemented for your tests. For example, here's an implementation that never
fails regardless of the arguments it receives

    class MyNeodatisProvider implements NeodatisProvider {
        Object withOdb(String serverName = 'default', Closure closure) { null }
        public <T> T withOdb(String serverName = 'default', CallableWithArgs<T> callable) { null }
    }

This implementation may be used in the following way

    class MyServiceTests extends GriffonUnitTestCase {
        void testSmokeAndMirrors() {
            MyService service = new MyService()
            NeodatisEnhancer.enhance(service.metaClass, new MyNeodatisProvider())
            // exercise service methods
        }
    }


[1]: http://neodatis.org/
[2]: https://github.com/aalmiray/griffon_sample_apps/tree/master/persistence/neodatis
'''
}
