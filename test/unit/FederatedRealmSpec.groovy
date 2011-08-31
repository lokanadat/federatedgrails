
import spock.lang.*
import grails.plugin.spock.*

import org.apache.shiro.subject.Subject
import org.apache.shiro.authc.AuthenticationException
import org.apache.shiro.authc.UnknownAccountException
import org.apache.shiro.authc.DisabledAccountException
import org.apache.shiro.authc.IncorrectCredentialsException
import org.apache.shiro.authc.SimpleAccount

import org.codehaus.groovy.grails.commons.ConfigurationHolder

import grails.plugins.federatedgrails.SubjectBase
import grails.plugins.federatedgrails.SessionRecord
import grails.plugins.federatedgrails.FederatedToken

class FederatedRealmSpec extends UnitSpec {
  
  @Shared def shiroEnvironment = new ShiroEnvironment()
  
  def cleanupSpec() { 
    shiroEnvironment.tearDownShiro() 
  }
  
  def 'UnknownAccountException when federated authentication is not enabled'() {
    setup:
    mockLogging(FederatedRealm, true)
    def realm = new FederatedRealm()
    
    mockConfig '''
      federation {
        enabled = false
      }
    ''' 
    realm.grailsApplication = [config: ConfigurationHolder.config]
    
    def token = new FederatedToken()
    
    when:
    realm.authenticate(token)
    
    then:
    UnknownAccountException e = thrown()
    e.message == 'Authentication attempt for federated provider, denying attempt as federation disabled'
  }
  
  def 'UnknownAccountException when principal is not supplied'() {
    setup:
    mockLogging(FederatedRealm, true)
    def realm = new FederatedRealm()
    
    mockConfig '''
      federation {
        enabled = true
      }
    ''' 
    realm.grailsApplication = [config: ConfigurationHolder.config]
    
    def token = new FederatedToken()
    
    when:
    realm.authenticate(token)
    
    then:
    UnknownAccountException e = thrown()
    e.message == 'Authentication attempt for federated provider, denying attempt as no persistent identifier was provided'
  }
  
  def 'UnknownAccountException when credential is not supplied'() {
    setup:
    mockLogging(FederatedRealm, true)
    def realm = new FederatedRealm()
    
    mockConfig '''
      federation {
        enabled = true
      }
    ''' 
    realm.grailsApplication = [config: ConfigurationHolder.config]
    
    def token = new FederatedToken(principal:'http://test.com!http://sp.test.com!1234')
    
    when:
    realm.authenticate(token)
    
    then:
    UnknownAccountException e = thrown()
    e.message == 'Authentication attempt for federated provider, denying attempt as no credential was provided'
  }
  
  def 'DisabledAccountException when user doesnt already exist and autoprovision disabled'() {
    setup:
    mockDomain(SubjectBase)
    mockDomain(SessionRecord)
    mockLogging(FederatedRealm, true)
    def realm = new FederatedRealm()
    
    mockConfig '''
      federation {
        enabled = true
        autoprovision = false
      }
    ''' 
    realm.grailsApplication = [config: ConfigurationHolder.config]
    
    def token = new FederatedToken(principal:'http://test.com!http://sp.test.com!1234', credential:'1234-mockid-5678')
    SubjectBase.metaClass.'static'.withTransaction = { Closure c -> c() }

    when:
    realm.authenticate(token)
    
    then:
    DisabledAccountException e = thrown()
    e.message == 'Authentication attempt for federated provider, denying attempt as federation integration is denying automated account provisioning'
  }
  
  def 'Basic account created when autoprovision is enabled'() {
    setup:
    mockDomain(SubjectBase)
    mockDomain(SessionRecord)
    mockLogging(FederatedRealm, true)
    def realm = new FederatedRealm()
    
    mockConfig '''
      federation {
        enabled = true
        autoprovision = true
        app.subject = 'grails.plugins.federatedgrails.SubjectBase'
      }
    ''' 
    realm.grailsApplication = [config: ConfigurationHolder.config]
    
    def token = new FederatedToken(principal:'http://test.com!http://sp.test.com!1234', credential:'1234-mockid-5678', userAgent:'Google Chrome X.Y')
    SubjectBase.metaClass.'static'.withTransaction = { Closure c -> c() }

    when:
    def subjects = SubjectBase.count()
    def account = realm.authenticate(token)
    
    then:
    subjects == 0
    SubjectBase.count() == 1  // Note we can't do a count on SessionRecord as cascade save doesn't work in unit tests
    account instanceof SimpleAccount
    account.credentials == '1234-mockid-5678'
    def subject = SubjectBase.get(account.principals.primaryPrincipal)
    subject.principal == 'http://test.com!http://sp.test.com!1234'
    subject.enabled
    def sessionRecord = subject.sessionRecords.toList().get(0)
    sessionRecord.credential == '1234-mockid-5678'
    sessionRecord.userAgent == 'Google Chrome X.Y'
  }
  
  def 'Existing, disabled account throws DisabledAccountException'() {
    setup:
    mockDomain(SubjectBase)
    mockDomain(SessionRecord)
    mockLogging(FederatedRealm, true)
    def realm = new FederatedRealm()
    
    mockConfig '''
      federation {
        enabled = true
        autoprovision = true
        app.subject = 'grails.plugins.federatedgrails.SubjectBase'
      }
    ''' 
    realm.grailsApplication = [config: ConfigurationHolder.config]
    
    def token = new FederatedToken(principal:'http://test.com!http://sp.test.com!1234', credential:'1234-mockid-5678', userAgent:'Google Chrome X.Y', attributes:[displayName:'Joe Bloggs', email:'joe@test.com', entitlement:'test:domain:value;test:domain:value2'])
    SubjectBase.metaClass.'static'.withTransaction = { Closure c -> c() }
    
    def subject = new SubjectBase(principal:'http://test.com!http://sp.test.com!1234', enabled:false).save()

    when:
    def account = realm.authenticate(token)
    
    then:
    SubjectBase.count() == 1
    DisabledAccountException e = thrown()
    e.message == "Attempt to authenticate using using federated principal mapped to a locally disabled account [${subject.id}]${subject.principal}"
  }
  
  def 'Failing subject save for new account throws RuntimeException'() {
    setup:
    mockDomain(SubjectBase)
    mockDomain(SessionRecord)
    mockLogging(FederatedRealm, true)
    def realm = new FederatedRealm()
    
    mockConfig '''
      federation {
        enabled = true
        autoprovision = true
        app.subject = 'grails.plugins.federatedgrails.SubjectBase'
      }
    ''' 
    realm.grailsApplication = [config: ConfigurationHolder.config]
    
    def token = new FederatedToken(principal:'http://test.com!http://sp.test.com!1234', credential:'1234-mockid-5678', userAgent:'Google Chrome X.Y', attributes:[displayName:'Joe Bloggs', email:'joe@test.com', entitlement:'test:domain:value;test:domain:value2'])
    SubjectBase.metaClass.'static'.withTransaction = { Closure c -> c() }
    SubjectBase.metaClass.save = { false }
    
    when:
    def account = realm.authenticate(token)
    
    then:
    RuntimeException e = thrown()
    e.message == "Account creation exception for new federated account for http://test.com!http://sp.test.com!1234"
  }
  
  def 'Failing subject save for session record throws RuntimeException'() {
    setup:
    mockDomain(SubjectBase)
    mockDomain(SessionRecord)
    mockLogging(FederatedRealm, true)
    def realm = new FederatedRealm()
    
    mockConfig '''
      federation {
        enabled = true
        autoprovision = true
        app.subject = 'grails.plugins.federatedgrails.SubjectBase'
      }
    ''' 
    realm.grailsApplication = [config: ConfigurationHolder.config]
    
    def token = new FederatedToken(principal:'http://test.com!http://sp.test.com!1234', credential:'1234-mockid-5678', userAgent:'Google Chrome X.Y', attributes:[displayName:'Joe Bloggs', email:'joe@test.com', entitlement:'test:domain:value;test:domain:value2'])
    SubjectBase.metaClass.'static'.withTransaction = { Closure c -> c() }
    
    
    def subject = new SubjectBase(principal:'http://test.com!http://sp.test.com!1234', enabled:true).save()
    SubjectBase.metaClass.save = { false }
    
    when:
    def account = realm.authenticate(token)
    
    then:
    RuntimeException e = thrown()
    e.message == "Account modification for ${token.principal} when adding new session record"
  }
  
}