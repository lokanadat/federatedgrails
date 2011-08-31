import groovy.text.SimpleTemplateEngine

includeTargets << grailsScript("_GrailsArgParsing")

target ( default : 'Enhances the amount of customization a project has over Federated components by creating local copies of Realms and Views' ) {

	// Controllers
	copy( todir: "${basedir}/grails-app/controllers" , overwrite: false ) { fileset ( dir : "${federatedGrailsPluginDir}/grails-app/controllers" ) }

  	// Realms
  	copy( todir: "${basedir}/grails-app/realms" , overwrite: false ) { fileset ( dir : "${federatedGrailsPluginDir}/grails-app/realms" ) }

}