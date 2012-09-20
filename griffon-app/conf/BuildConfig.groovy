griffon.project.dependency.resolution = {
    inherits "global"
    log "warn"
    repositories {
        flatDir name: 'neodatisPluginLib', dirs: 'lib'
    }
    dependencies {
        compile 'org.neodatis.odb:neodatis-odb:2.2.beta1.252'
    }
}

griffon {
    doc {
        logo = '<a href="http://griffon.codehaus.org" target="_blank"><img alt="The Griffon Framework" src="../img/griffon.png" border="0"/></a>'
        sponsorLogo = "<br/>"
        footer = "<br/><br/>Made with Griffon (@griffon.version@)"
    }
}

griffon.jars.destDir='target/addon'
