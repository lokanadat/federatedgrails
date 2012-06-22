import groovy.text.SimpleTemplateEngine

includeTargets << grailsScript("_GrailsArgParsing")

USAGE = """
AAF/SAML integration for Grails and Shiro Applications.
  
Usage:
    integrate-federation PKG SUBJECT

Where:
    PKG  = The package name to use for your Subject class.
    SUBJECT = The name of your Subject domain class (which will be created).

e.g:
    grails integrate-federation au.edu.myuni.appname Subject
"""

target ( default : 'Sets up a new project with a common federated base environment ready for customization' ) {
  
  def subject, pack, packdir
  (pack, subject) = parseArgs()
  packdir = pack.replace('.', '/')
 
  def subjectbinding = [ 'pack':pack, 'subject':subject ]
  def configbinding = [ 'pack':pack, 'subject':subject ]
  def securitybinding = ['pack':pack, 'subject':subject]

  def engine = new SimpleTemplateEngine()
  def subjecttemplate = engine.createTemplate(new FileReader("${federatedGrailsPluginDir}/src/templates/domain/Base.groovy")).make(subjectbinding)
  def configtemplate = engine.createTemplate(new FileReader("${federatedGrailsPluginDir}/src/templates/conf/FederatedConfig.groovy")).make(configbinding)
  def securitytemplate = engine.createTemplate(new FileReader("${federatedGrailsPluginDir}/src/templates/conf/SecurityFilters.groovy")).make(securitybinding)

  echo("Customizing your application for federation integration...." )

  // Config
  new File("${basedir}/grails-app/conf/FederationConfig.groovy").write(configtemplate.toString())
  new File("${basedir}/grails-app/conf/SecurityFilters.groovy").write(securitytemplate.toString())

  // Realms
  copy( todir: "${basedir}/grails-app/realms" , overwrite: false ) { fileset ( dir : "${federatedGrailsPluginDir}/injected-src/realms" ) }

  // Controllers
  copy( todir: "${basedir}/grails-app/controllers" , overwrite: false ) { fileset ( dir : "${federatedGrailsPluginDir}/injected-src/controllers" ) }

  // Domain Objects
  mkdir(dir:"${basedir}/grails-app/domain/${packdir}")
  new File("${basedir}/grails-app/domain/${packdir}/${subject}.groovy").write(subjecttemplate.toString())

  // Views
  copy( todir: "${basedir}/grails-app/views/auth" , overwrite: false ) { fileset ( dir : "${federatedGrailsPluginDir}/injected-src/views/auth" ) }
}

def parseArgs() {
  args = args ? args.split('\n') : []
    switch (args.size()) {
            case 2:
                println "Using package ${args[0]} to create custom classes"
                    println "Setting up federated integration with custom Subject domain class: ${args[1]}"
                    return [args[0], args[1]]
                    break
            default:
                  usage()
                  break
    }
}

private void usage() {
  println USAGE
  System.exit(1)
}
