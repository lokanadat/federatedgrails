package grails.plugins.federatedgrails

import spock.lang.*
import grails.plugin.spock.*

import grails.plugins.federatedgrails.SessionRecord

class SessionRecordSpec extends UnitSpec {
  
  def 'Ensure SessionRecord wont validate with null credential'() {
    setup:
    mockDomain(SessionRecord)
    
    when:
    def sr = new SessionRecord(remoteHost:'test', userAgent:'test').validate()
    
    then:
    !sr
  }
  
  def 'Ensure SessionRecord wont validate with blank credential'() {
    setup:
    mockDomain(SessionRecord)
    
    when:
    def sr = new SessionRecord(credential:'', remoteHost:'test', userAgent:'test').validate()
    
    then:
    !sr
  }
  
  def 'Ensure SessionRecord wont validate null remoteHost credential'() {
    setup:
    mockDomain(SessionRecord)
    
    when:
    def sr = new SessionRecord(credential:'test', userAgent:'test').validate()
    
    then:
    !sr
  }
  
  def 'Ensure SessionRecord wont validate with blank remoteHost'() {
    setup:
    mockDomain(SessionRecord)
    
    when:
    def sr = new SessionRecord(credential:'test', remoteHost:'', userAgent:'test').validate()
    
    then:
    !sr
  }
  
  def 'Ensure SessionRecord wont validate null userAgent credential'() {
    setup:
    mockDomain(SessionRecord)
    
    when:
    def sr = new SessionRecord(credential:'test', remoteHost:'test').validate()
    
    then:
    !sr
  }
  
  def 'Ensure SessionRecord wont validate with blank userAgent'() {
    setup:
    mockDomain(SessionRecord)
    
    when:
    def sr = new SessionRecord(credential:'test', remoteHost:'test', userAgent:'').validate()
    
    then:
    !sr
  }

}