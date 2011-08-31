
import spock.lang.*
import grails.plugin.spock.*

import org.apache.shiro.subject.Subject
import org.apache.shiro.authc.AuthenticationException
import org.apache.shiro.authc.UnknownAccountException
import org.apache.shiro.authc.DisabledAccountException
import org.apache.shiro.authc.IncorrectCredentialsException

import org.codehaus.groovy.grails.commons.ConfigurationHolder

import grails.plugins.federatedgrails.FederatedToken

class AuthControllerSpec extends ControllerSpec {
  
  @Shared def shiroEnvironment = new ShiroEnvironment()
  
  def cleanupSpec() { 
    shiroEnvironment.tearDownShiro() 
  }
  
  def 'that login view will be rendered when automate login is disabled'() {
    setup:
    def linkAction
    def linkAbsolute

    mockConfig '''
      federation {
        automatelogin = false
        ssoendpoint = "/Shibboleth.sso/Login"
      }
    ''' 
    controller.metaClass.getGrailsApplication = { -> [config: ConfigurationHolder.config]}

    controller.metaClass.createLink = { attrs ->
      linkAction = attrs.action
      linkAbsolute = attrs.absolute
      "http://test.com/federatedlogin"
    }
    
    when:
    def result = controller.login()
    
    then:
    mockResponse.status == 200
    linkAction == 'federatedlogin'
    linkAbsolute
    result.spsession_url == "/Shibboleth.sso/Login?target=http://test.com/federatedlogin"
  }
  
  def 'that SP redirect will be invoked when autologin active'() {
    setup:
    def linkAction
    def linkAbsolute
    
    mockConfig '''
      federation {
        automatelogin = true
        ssoendpoint = "/Shibboleth.sso/Login"
      }
    ''' 
    controller.metaClass.getGrailsApplication = { -> [config: ConfigurationHolder.config]}
    
    controller.metaClass.createLink = { attrs ->
      linkAction = attrs.action
      linkAbsolute = attrs.absolute
      "http://test.com/federatedlogin"
    }
    
    controller.params.targetUri = '/some/test/content'
    
    when:
    controller.login()
    
    then:
    linkAction == 'federatedlogin'
    linkAbsolute
    redirectArgs.url == '/Shibboleth.sso/Login?target=http://test.com/federatedlogin'
    mockSession['grails.controllers.AuthController.shiro:TARGET'] == '/some/test/content'
  }
  
  def 'that logout will redirect to application root'() {   
    setup:
    Subject subject = Mock(Subject)
    subject.isAuthenticated() >> true
    shiroEnvironment.setSubject(subject)
    
    controller.metaClass.getSubject = { [id:1, principal:'http://test.com!http://sp.test.com!1234'] }
    
    when:
    controller.logout()
    
    then:
    redirectArgs.uri == '/'
  }
  
  def 'Echo returns set attributes'() {   
    setup:
    mockConfig '''
      federation {
        request.attributes = true
      }
    '''
    controller.metaClass.getGrailsApplication = { -> [config: ConfigurationHolder.config]}
    
    mockRequest.setAttribute('Shib-Entity-ID', 'http://test.com/idpshibboleth') 
    mockRequest.setAttribute('displayName', 'Joe Bloggs')
    
    when:
    def model = controller.echo()
    
    then:
    model.attr.'Shib-Entity-ID' == "http://test.com/idpshibboleth"
    model.attr.displayName == "Joe Bloggs"
  }
  
  def 'Echo returns set headers'() {    
    setup:
    mockConfig '''
      federation {
        request.attributes = false
      }
    '''
    controller.metaClass.getGrailsApplication = { -> [config: ConfigurationHolder.config]}
    
    mockRequest.addHeader('Shib-Entity-ID', 'http://test.com/idpshibboleth') 
    mockRequest.addHeader('displayName', 'Joe Bloggs')
    
    when:
    def model = controller.echo()
    
    then:
    model.attr == ['Shib-Entity-ID':"http://test.com/idpshibboleth", displayName:"Joe Bloggs"]
  }
  
  def '403 when sp is disabled for federated login'() {
    setup:
    mockConfig '''
      federation {
        enabled = true
        federationactive = false
        developmentactive = false
      }
    ''' 
    controller.metaClass.getGrailsApplication = { -> [config: ConfigurationHolder.config]}
    
    when:
    controller.federatedlogin()
    
    then:
    mockResponse.status == 403
    mockResponse.committed
  }
  
  def 'incomplete and redirect to federatedincomplete when principal not provided'() {
    setup:
    mockConfig '''
      federation {
        enabled = true
        federationactive = true
        developmentactive = false
        
        request.attributes = true
        
        mapping {
          principal = 'persistent-id'   
          credential = 'Shib-Session-ID'
        }
      }
    ''' 
    controller.metaClass.getGrailsApplication = { -> [config: ConfigurationHolder.config]}
    
    when:
    controller.federatedlogin()
    
    then:
    renderArgs.view == "federatedincomplete"
    renderArgs.model.errors.contains("Unique subject identifier (principal) was not presented")
  }
  
  def 'incomplete and redirect to federatedincomplete when credential not provided'() {
    setup:
    mockConfig '''
      federation {
        enabled = true
        federationactive = true
        developmentactive = false
        
        request.attributes = true
        
        mapping {
          principal = 'persistent-id'   
          credential = 'Shib-Session-ID'
        }
      }
    ''' 
    controller.metaClass.getGrailsApplication = { -> [config: ConfigurationHolder.config]}
    mockRequest.setAttribute('persistent-id', 'http://test.com!http://sp.test.com!1234')
    
    when:
    controller.federatedlogin()
    
    then:
    renderArgs.view == "federatedincomplete"
    !renderArgs.model.errors.contains("Unique subject identifier (principal) was not presented")
    renderArgs.model.errors.contains("Internal SAML session identifier (credential) was not presented")
  }
  
  def 'redirect to root URI when all is valid and no target supplied'() {
    setup:
    mockConfig '''
      federation {
        enabled = true
        federationactive = true
        developmentactive = false
        
        request.attributes = true
        
        mapping {
          principal = 'persistent-id'   
          credential = 'Shib-Session-ID'
        }
      }
    ''' 
    controller.metaClass.getGrailsApplication = { -> [config: ConfigurationHolder.config]}
    mockRequest.setAttribute('persistent-id', 'http://test.com!http://sp.test.com!1234')
    mockRequest.setAttribute('Shib-Session-ID', '1234-mockid-5678')
    mockRequest.addHeader("User-Agent", "Google Chrome X.Y")
    
    def token
    Subject subject = Mock(Subject)
    shiroEnvironment.setSubject(subject)
    
    when:
    controller.federatedlogin()
    
    then:
    1 * subject.login( { t -> token = t; t instanceof FederatedToken } )
    token.principal == 'http://test.com!http://sp.test.com!1234'
    token.credential == '1234-mockid-5678'
    token.userAgent == "Google Chrome X.Y"
    redirectArgs.uri == '/'
  }
  
  def 'redirect to target URI when all is valid and target supplied'() {
    setup:
    mockConfig '''
      federation {
        enabled = true
        federationactive = true
        developmentactive = false
        
        request.attributes = true
        
        mapping {
          principal = 'persistent-id'   
          credential = 'Shib-Session-ID'
        }
      }
    ''' 
    controller.metaClass.getGrailsApplication = { -> [config: ConfigurationHolder.config]}
    mockRequest.setAttribute('persistent-id', 'http://test.com!http://sp.test.com!1234')
    mockRequest.setAttribute('Shib-Session-ID', '1234-mockid-5678')
    mockRequest.addHeader("User-Agent", "Google Chrome X.Y")
    
    def token
    Subject subject = Mock(Subject)
    shiroEnvironment.setSubject(subject)
    
    mockSession['grails.controllers.AuthController.shiro:TARGET'] = '/some/test/content'
    
    when:
    controller.federatedlogin()
    
    then:
    1 * subject.login( { t -> token = t; t instanceof FederatedToken } )
    token.principal == 'http://test.com!http://sp.test.com!1234'
    token.credential == '1234-mockid-5678'
    token.userAgent == "Google Chrome X.Y"
    redirectArgs.uri == '/some/test/content'
  }
  
  def 'redirect to federatederror when IncorrectCredentialsException thrown'() {
    setup:
    mockConfig '''
      federation {
        enabled = true
        federationactive = true
        developmentactive = false
        
        request.attributes = true
        
        mapping {
          principal = 'persistent-id'   
          credential = 'Shib-Session-ID'
        }
      }
    ''' 
    controller.metaClass.getGrailsApplication = { -> [config: ConfigurationHolder.config]}
    mockRequest.setAttribute('persistent-id', 'http://test.com!http://sp.test.com!1234')
    mockRequest.setAttribute('Shib-Session-ID', '1234-mockid-5678')
    mockRequest.addHeader("User-Agent", "Google Chrome X.Y")
    
    def token
    Subject subject = Mock(Subject)
    shiroEnvironment.setSubject(subject)
    
    mockSession['grails.controllers.AuthController.shiro:TARGET'] = '/some/test/content'
    
    when:
    controller.federatedlogin()
    
    then:
    1 * subject.login( _ as FederatedToken ) >> { throw new IncorrectCredentialsException('test') }
    redirectArgs.action == 'federatederror'
  }
  
  def 'redirect to federatederror when DisabledAccountException thrown'() {
    setup:
    mockConfig '''
      federation {
        enabled = true
        federationactive = true
        developmentactive = false
        
        request.attributes = true
        
        mapping {
          principal = 'persistent-id'   
          credential = 'Shib-Session-ID'
        }
      }
    ''' 
    controller.metaClass.getGrailsApplication = { -> [config: ConfigurationHolder.config]}
    mockRequest.setAttribute('persistent-id', 'http://test.com!http://sp.test.com!1234')
    mockRequest.setAttribute('Shib-Session-ID', '1234-mockid-5678')
    mockRequest.addHeader("User-Agent", "Google Chrome X.Y")
    
    def token
    Subject subject = Mock(Subject)
    shiroEnvironment.setSubject(subject)
    
    mockSession['grails.controllers.AuthController.shiro:TARGET'] = '/some/test/content'
    
    when:
    controller.federatedlogin()
    
    then:
    1 * subject.login( _ as FederatedToken ) >> { throw new DisabledAccountException('test') }
    redirectArgs.action == 'federatederror'
  }
  
  def 'redirect to federatederror when AuthenticationException thrown'() {
    setup:
    mockConfig '''
      federation {
        enabled = true
        federationactive = true
        developmentactive = false
        
        request.attributes = true
        
        mapping {
          principal = 'persistent-id'   
          credential = 'Shib-Session-ID'
        }
      }
    ''' 
    controller.metaClass.getGrailsApplication = { -> [config: ConfigurationHolder.config]}
    mockRequest.setAttribute('persistent-id', 'http://test.com!http://sp.test.com!1234')
    mockRequest.setAttribute('Shib-Session-ID', '1234-mockid-5678')
    mockRequest.addHeader("User-Agent", "Google Chrome X.Y")
    
    def token
    Subject subject = Mock(Subject)
    shiroEnvironment.setSubject(subject)
    
    mockSession['grails.controllers.AuthController.shiro:TARGET'] = '/some/test/content'
    
    when:
    controller.federatedlogin()
    
    then:
    1 * subject.login( _ as FederatedToken ) >> { throw new AuthenticationException('test') }
    redirectArgs.action == 'federatederror'
  }
  
  def '403 when local is disabled for local login'() {
    setup:
    mockConfig '''
      federation {
        enabled = true
        federationactive = false
        developmentactive = false
      }
    ''' 
    controller.metaClass.getGrailsApplication = { -> [config: ConfigurationHolder.config]}
    
    when:
    controller.locallogin()
    
    then:
    mockResponse.status == 403
    mockResponse.committed
  }
  
  
  def 'incomplete and redirect to federatedincomplete when principal not provided to locallogin'() {
    setup:
    mockConfig '''
      federation {
        enabled = true
        federationactive = false
        developmentactive = true
        
        request.attributes = true
        
        mapping {
          principal = 'persistent-id'   
          credential = 'Shib-Session-ID'
        }
      }
    ''' 
    controller.metaClass.getGrailsApplication = { -> [config: ConfigurationHolder.config]}
    
    when:
    controller.locallogin()
    
    then:
    renderArgs.view == "federatedincomplete"
    renderArgs.model.errors.contains("Unique subject identifier (principal) was not presented")
  }
  
  def 'incomplete and redirect to federatedincomplete when credential not provided to locallogin'() {
    setup:
    mockConfig '''
      federation {
        enabled = true
        federationactive = false
        developmentactive = true
        
        request.attributes = true
        
        mapping {
          principal = 'persistent-id'   
          credential = 'Shib-Session-ID'
        }
      }
    ''' 
    controller.metaClass.getGrailsApplication = { -> [config: ConfigurationHolder.config]}
    controller.params.principal = 'http://test.com!http://sp.test.com!1234'
    
    when:
    controller.locallogin()
    
    then:
    renderArgs.view == "federatedincomplete"
    !renderArgs.model.errors.contains("Unique subject identifier (principal) was not presented")
    renderArgs.model.errors.contains("Internal SAML session identifier (credential) was not presented")
  }
  
  def 'redirect to root URI when all is valid and no target supplied to locallogin'() {
    setup:
    mockConfig '''
      federation {
        enabled = true
        federationactive = false
        developmentactive = true
        
        request.attributes = true
        
        mapping {
          principal = 'persistent-id'   
          credential = 'Shib-Session-ID'
        }
      }
    ''' 
    controller.metaClass.getGrailsApplication = { -> [config: ConfigurationHolder.config]}
    controller.params.principal = 'http://test.com!http://sp.test.com!1234'
    controller.params.credential = '1234-mockid-5678'
    mockRequest.addHeader("User-Agent", "Google Chrome X.Y")
    
    def token
    Subject subject = Mock(Subject)
    shiroEnvironment.setSubject(subject)
    
    when:
    controller.locallogin()
    
    then:
    1 * subject.login( { t -> token = t; t instanceof FederatedToken } )
    token.principal == 'http://test.com!http://sp.test.com!1234'
    token.credential == '1234-mockid-5678'
    token.userAgent == "Google Chrome X.Y"
    redirectArgs.uri == '/'
  }
  
  def 'redirect to target URI when all is valid and target supplied to locallogin'() {
    setup:
    mockConfig '''
      federation {
        enabled = true
        federationactive = false
        developmentactive = true
        
        request.attributes = true
        
        mapping {
          principal = 'persistent-id'   
          credential = 'Shib-Session-ID'
        }
      }
    ''' 
    controller.metaClass.getGrailsApplication = { -> [config: ConfigurationHolder.config]}
    controller.params.principal = 'http://test.com!http://sp.test.com!1234'
    controller.params.credential = '1234-mockid-5678'
    mockRequest.addHeader("User-Agent", "Google Chrome X.Y")
    
    def token
    Subject subject = Mock(Subject)
    shiroEnvironment.setSubject(subject)
    
    mockSession['grails.controllers.AuthController.shiro:TARGET'] = '/some/test/content'
    
    when:
    controller.locallogin()
    
    then:
    1 * subject.login( { t -> token = t; t instanceof FederatedToken } )
    token.principal == 'http://test.com!http://sp.test.com!1234'
    token.credential == '1234-mockid-5678'
    token.userAgent == "Google Chrome X.Y"
    redirectArgs.uri == '/some/test/content'
  }
  
  def 'redirect to federatederror when IncorrectCredentialsException thrown in locallogin'() {
    setup:
    mockConfig '''
      federation {
        enabled = true
        federationactive = false
        developmentactive = true
        
        request.attributes = true
        
        mapping {
          principal = 'persistent-id'   
          credential = 'Shib-Session-ID'
        }
      }
    ''' 
    controller.metaClass.getGrailsApplication = { -> [config: ConfigurationHolder.config]}
    controller.params.principal = 'http://test.com!http://sp.test.com!1234'
    controller.params.credential = '1234-mockid-5678'
    mockRequest.addHeader("User-Agent", "Google Chrome X.Y")
    
    def token
    Subject subject = Mock(Subject)
    shiroEnvironment.setSubject(subject)
    
    mockSession['grails.controllers.AuthController.shiro:TARGET'] = '/some/test/content'
    
    when:
    controller.locallogin()
    
    then:
    1 * subject.login( _ as FederatedToken ) >> { throw new IncorrectCredentialsException('test') }
    redirectArgs.action == 'login'
  }
  
  def 'redirect to federatederror when DisabledAccountException thrown in locallogin'() {
    setup:
    mockConfig '''
      federation {
        enabled = true
        federationactive = false
        developmentactive = true
        
        request.attributes = true
        
        mapping {
          principal = 'persistent-id'   
          credential = 'Shib-Session-ID'
        }
      }
    ''' 
    controller.metaClass.getGrailsApplication = { -> [config: ConfigurationHolder.config]}
    controller.params.principal = 'http://test.com!http://sp.test.com!1234'
    controller.params.credential = '1234-mockid-5678'
    mockRequest.addHeader("User-Agent", "Google Chrome X.Y")
    
    def token
    Subject subject = Mock(Subject)
    shiroEnvironment.setSubject(subject)
    
    when:
    controller.locallogin()
    
    then:
    1 * subject.login( _ as FederatedToken ) >> { throw new DisabledAccountException('test') }
    redirectArgs.action == 'login'
  }
  
  def 'redirect to federatederror when AuthenticationException thrown in locallogin'() {
    setup:
    mockConfig '''
      federation {
        enabled = true
        federationactive = false
        developmentactive = true
        
        request.attributes = true
        
        mapping {
          principal = 'persistent-id'   
          credential = 'Shib-Session-ID'
        }
      }
    ''' 
    controller.metaClass.getGrailsApplication = { -> [config: ConfigurationHolder.config]}
    controller.params.principal = 'http://test.com!http://sp.test.com!1234'
    controller.params.credential = '1234-mockid-5678'
    mockRequest.addHeader("User-Agent", "Google Chrome X.Y")
    
    def token
    Subject subject = Mock(Subject)
    shiroEnvironment.setSubject(subject)
    
    when:
    controller.locallogin()
    
    then:
    1 * subject.login( _ as FederatedToken ) >> { throw new AuthenticationException('test') }
    redirectArgs.action == 'login'
  }
  
}