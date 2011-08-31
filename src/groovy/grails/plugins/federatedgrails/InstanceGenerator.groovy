
package grails.plugins.federatedgrails

import org.codehaus.groovy.grails.commons.ConfigurationHolder

import grails.plugins.federatedgrails.SubjectBase

class InstanceGenerator {

    static subject = { 
      try { 
        if(ConfigurationHolder.config?.federation?.app?.subject) {
          InstanceGenerator.class.classLoader.loadClass(ConfigurationHolder.config.federation.app.subject).newInstance()
        } else {
          SubjectBase.newInstance()
      }
      } catch(ClassNotFoundException e){ throw new RuntimeException(e)} 
    }

}

