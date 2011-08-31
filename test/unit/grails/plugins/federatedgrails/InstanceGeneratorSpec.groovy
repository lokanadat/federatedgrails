package grails.plugins.federatedgrails

import spock.lang.*
import grails.plugin.spock.*

import org.codehaus.groovy.grails.commons.ConfigurationHolder

import grails.plugins.federatedgrails.SubjectBase
import grails.plugins.federatedgrails.TestSubject

class InstanceGeneratorSpec extends UnitSpec {
  
  def 'InstanceGenerator returns SubjectBase when no configuration supplied'() {
    setup:    
    mockConfig '''
    '''
    
    when:
    def subject = InstanceGenerator.subject()
    
    then:
    !(subject instanceof TestSubject)
    subject instanceof SubjectBase
  }
  
  def 'InstanceGenerator returns TestSubject when configured as such'() {
    setup:    
    mockConfig '''
      federation.app.subject = 'grails.plugins.federatedgrails.TestSubject'
    '''
    
    when:
    def subject = InstanceGenerator.subject()
    
    then:
    subject instanceof TestSubject
    subject instanceof SubjectBase
  }
  
  def 'InstanceGenerator throws RuntimeException when invalid class set'() {
    setup:    
    mockConfig '''
      federation.app.subject = 'grails.plugins.federatedgrails.NullSubject'
    '''
    
    when:
    def subject = InstanceGenerator.subject()
    
    then:
    RuntimeException e = thrown()
  }
}