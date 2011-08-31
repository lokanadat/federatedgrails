package grails.plugins.federatedgrails

import spock.lang.*
import grails.plugin.spock.*

import grails.plugins.federatedgrails.SubjectBase

class SubjectBaseSpec extends UnitSpec {
  
  def 'Ensure subject wont validate with null principal'() {
    setup:
    mockDomain(SubjectBase)
    
    when:
    def s = new SubjectBase().validate()
    
    then:
    !s
  }
  
  def 'Ensure subject wont validate with blank principal'() {
    setup:
    mockDomain(SubjectBase)
    
    when:
    def s = new SubjectBase(principal:'').validate()
    
    then:
    !s
  }
  
  def 'Ensure subject wont validate with non-unique principal'() {
    setup:
    mockDomain(SubjectBase)
    
    when:
    new SubjectBase(principal:'http://test.com!http://sp.test.com!1234').save()
    def s = new SubjectBase(principal:'http://test.com!http://sp.test.com!1234').validate()
    
    then:
    !s
  }
  
  def 'Ensure subject will validate'() {
    setup:
    mockDomain(SubjectBase)
    
    when:
    def s = new SubjectBase(principal:'http://test.com!http://sp.test.com!1234').validate()
    
    then:
    s
  }

}