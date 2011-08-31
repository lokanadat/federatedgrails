
import spock.lang.*
import grails.plugin.spock.*

import org.apache.shiro.subject.SubjectBase
import org.apache.shiro.authc.AuthenticationException
import org.apache.shiro.authc.UnknownAccountException
import org.apache.shiro.authc.DisabledAccountException
import org.apache.shiro.authc.IncorrectCredentialsException
import org.apache.shiro.authc.SimpleAccount
import org.apache.shiro.authz.permission.WildcardPermissionResolver
import org.apache.shiro.authz.permission.WildcardPermission

import org.codehaus.groovy.grails.commons.ConfigurationHolder

import grails.plugins.federatedgrails.SubjectBase
import grails.plugins.federatedgrails.SessionRecord
import grails.plugins.federatedgrails.FederatedToken
import grails.plugins.federatedgrails.Role

class DatabaseRealmSpec extends IntegrationSpec {
  
  def 'positive result when subject has the required role'() {
    setup:
    def realm = new DatabaseRealm()
    def subject = new SubjectBase(principal:'http://test.com!http://sp.test.com!1234').save()
    def role = new Role(name:'testrole').save()
    role.addToSubjects(subject)
    role.save()
    
    when:
    def hasRole = realm.hasRole(subject.id, 'testrole')
    
    then:
    hasRole
  }
  
  def 'negative result when subject doesnt have the required role'() {
    setup:
    def realm = new DatabaseRealm()
    def subject = new SubjectBase(principal:'http://test.com!http://sp.test.com!1234').save()
    def role = new Role(name:'testrole').save()
    role.addToSubjects(subject)
    role.save()
    
    when:
    def hasRole = realm.hasRole(subject.id, 'testrole2')
    
    then:
    !hasRole
  }
  
  def 'positive result when subject has all the required roles'() {
    setup:
    def realm = new DatabaseRealm()
    def subject = new SubjectBase(principal:'http://test.com!http://sp.test.com!1234').save()
    def role = new Role(name:'testrole').save()
    role.addToSubjects(subject)
    role.save()
    
    def role2 = new Role(name:'testrole2').save()
    role2.addToSubjects(subject)
    role2.save()
    
    when:
    def hasRole = realm.hasAllRoles(subject.id, ["testrole", "testrole2"])
    
    then:
    hasRole
  }
  
  def 'negative result when subject doesnt have all the required roles'() {
    setup:
    def realm = new DatabaseRealm()
    def subject = new SubjectBase(principal:'http://test.com!http://sp.test.com!1234').save()
    def role = new Role(name:'testrole').save()
    role.addToSubjects(subject)
    role.save()
    
    when:
    String[] roles = ['testrole', 'testrole2']
    def hasRole = realm.hasAllRoles(subject.id, roles)
    
    then:
    !hasRole
  }
  
  def 'positive result when subject has directly assigned permission'() {
    setup:
    def realm = new DatabaseRealm()
    realm.shiroPermissionResolver = new WildcardPermissionResolver()
    def subject = new SubjectBase(principal:'http://test.com!http://sp.test.com!1234').save()
    subject.addToPermissions("test:domain:action:id")
    subject.save()
    
    when:
    def isPermitted = realm.isPermitted(subject.id, new WildcardPermission('test:domain:action:id'))
    
    then:
    isPermitted
  }
  
  def 'negative result when subject isnt directly assigned permission'() {
    setup:
    def realm = new DatabaseRealm()
    realm.shiroPermissionResolver = new WildcardPermissionResolver()
    def subject = new SubjectBase(principal:'http://test.com!http://sp.test.com!1234').save()
    subject.addToPermissions("test:domain:action:id")
    subject.save()
    
    when:
    def isPermitted = realm.isPermitted(subject.id, new WildcardPermission('test:domain:action2:id'))
    
    then:
    !isPermitted
  }
  
  def 'positive result when subject has wildcard assigned permission'() {
    setup:
    def realm = new DatabaseRealm()
    realm.shiroPermissionResolver = new WildcardPermissionResolver()
    def subject = new SubjectBase(principal:'http://test.com!http://sp.test.com!1234').save()
    subject.addToPermissions("test:domain:*")
    subject.save()
    
    when:
    def isPermitted = realm.isPermitted(subject.id, new WildcardPermission('test:domain:action:id'))
    
    then:
    isPermitted
  }
  
  def 'negative result when subject isnt wildcard assigned permission'() {
    setup:
    def realm = new DatabaseRealm()
    realm.shiroPermissionResolver = new WildcardPermissionResolver()
    def subject = new SubjectBase(principal:'http://test.com!http://sp.test.com!1234').save()
    subject.addToPermissions("test:domain:*")
    subject.save()
    
    when:
    def isPermitted = realm.isPermitted(subject.id, new WildcardPermission('test:domain2:action2:id'))
    
    then:
    !isPermitted
  }
  
  def 'positive result when subject has role that has directly assigned permission'() {
    setup:
    def realm = new DatabaseRealm()
    realm.shiroPermissionResolver = new WildcardPermissionResolver()
    def subject = new SubjectBase(principal:'http://test.com!http://sp.test.com!1234').save()
    def role = new Role(name:'testrole').save()
    role.addToSubjects(subject)
    role.addToPermissions("test:domain:action:id")
    role.save()
    
    when:
    def isPermitted = realm.isPermitted(subject.id, new WildcardPermission('test:domain:action:id'))
    
    then:
    isPermitted
  }
  
  def 'negative result when subject has role but neither are directly assigned permission'() {
    setup:
    def realm = new DatabaseRealm()
    realm.shiroPermissionResolver = new WildcardPermissionResolver()
    def subject = new SubjectBase(principal:'http://test.com!http://sp.test.com!1234').save()
    def role = new Role(name:'testrole').save()
    role.addToSubjects(subject)
    role.addToPermissions("test:domain:action:id")
    role.save()
    
    when:
    def isPermitted = realm.isPermitted(subject.id, new WildcardPermission('test:domain:action2:id'))
    
    then:
    !isPermitted
  }
  
  def 'positive result when subject and role have directly assigned permission'() {
    setup:
    def realm = new DatabaseRealm()
    realm.shiroPermissionResolver = new WildcardPermissionResolver()
    def subject = new SubjectBase(principal:'http://test.com!http://sp.test.com!1234').save()
    subject.addToPermissions("test:domain:action:id")
    subject.save()
    def role = new Role(name:'testrole').save()
    role.addToSubjects(subject)
    role.addToPermissions("test:domain:action:id")
    role.save()
    
    when:
    def isPermitted = realm.isPermitted(subject.id, new WildcardPermission('test:domain:action:id'))
    
    then:
    isPermitted
  }
  
  def 'positive result when subject has role that has wildcard assigned permission'() {
    setup:
    def realm = new DatabaseRealm()
    realm.shiroPermissionResolver = new WildcardPermissionResolver()
    def subject = new SubjectBase(principal:'http://test.com!http://sp.test.com!1234').save()
    def role = new Role(name:'testrole').save()
    role.addToSubjects(subject)
    role.addToPermissions("test:domain:action:*")
    role.save()
    
    when:
    def isPermitted = realm.isPermitted(subject.id, new WildcardPermission('test:domain:action:id'))
    
    then:
    isPermitted
  }
  
  def 'negative result when subject has role but neither are wildcard assigned permission'() {
    setup:
    def realm = new DatabaseRealm()
    realm.shiroPermissionResolver = new WildcardPermissionResolver()
    def subject = new SubjectBase(principal:'http://test.com!http://sp.test.com!1234').save()
    def role = new Role(name:'testrole').save()
    role.addToSubjects(subject)
    role.addToPermissions("test:domain:*")
    role.save()
    
    when:
    def isPermitted = realm.isPermitted(subject.id, new WildcardPermission('test:domain2:action:id'))
    
    then:
    !isPermitted
  }
  
  def 'positive result when subject and role have wildcard assigned permission'() {
    setup:
    def realm = new DatabaseRealm()
    realm.shiroPermissionResolver = new WildcardPermissionResolver()
    def subject = new SubjectBase(principal:'http://test.com!http://sp.test.com!1234').save()
    subject.addToPermissions("test:domain:action:*")
    subject.save()
    def role = new Role(name:'testrole').save()
    role.addToSubjects(subject)
    role.addToPermissions("test:domain:action:*")
    role.save()
    
    when:
    def isPermitted = realm.isPermitted(subject.id, new WildcardPermission('test:domain:action:id'))
    
    then:
    isPermitted
  }
  
  def 'positive result when subject and role have multiple permissions only 1 of which matches'() {
    setup:
    def realm = new DatabaseRealm()
    realm.shiroPermissionResolver = new WildcardPermissionResolver()
    def subject = new SubjectBase(principal:'http://test.com!http://sp.test.com!1234').save()
    subject.addToPermissions("test:domain:action:id")
    subject.addToPermissions("test:otherdomain:action:*")
    subject.addToPermissions("test:otherdomain2:action:*")
    subject.addToPermissions("test:domain:action:read")
    subject.save()
    def role = new Role(name:'testrole').save()
    role.addToSubjects(subject)
    role.addToPermissions("test:domain:action:*")
    role.addToPermissions("test:domain:action:id")
    role.addToPermissions("test:otherdomain:action:*")
    role.addToPermissions("test:otherdomain2:action:*")
    role.save()
    
    when:
    def isPermitted = realm.isPermitted(subject.id, new WildcardPermission('test:domain:action:read'))
    
    then:
    isPermitted
  }
  
  def 'positive result when subject has multiple roles that have multiple permissions only 1 of which matches'() {
    setup:
    def realm = new DatabaseRealm()
    realm.shiroPermissionResolver = new WildcardPermissionResolver()
    def subject = new SubjectBase(principal:'http://test.com!http://sp.test.com!1234').save()
    subject.addToPermissions("test:domain:action:id")
    subject.save()
    def role = new Role(name:'testrole').save()
    role.addToSubjects(subject)
    role.addToPermissions("test:domain:action:id")
    role.addToPermissions("test:otherdomain:action:*")
    role.addToPermissions("test:otherdomain:action:*")
    role.save()
    
    def role2 = new Role(name:'testrole2').save()
    role2.addToSubjects(subject)
    role2.addToPermissions("test:domain2:action:*")
    role2.addToPermissions("test:domain2:action:id")
    role2.addToPermissions("test:otherdomain2:action:*")
    role2.addToPermissions("test:otherdomain2:action:*")
    role2.save()
    
    def role3 = new Role(name:'testrole3').save()
    role3.addToSubjects(subject)
    role3.addToPermissions("test:domain3:action:*")
    role3.addToPermissions("test:domain3:action:id")
    role3.addToPermissions("test:otherdomain3:action:*")
    role3.addToPermissions("test:domain:action:read")
    role3.save()
    
    when:
    def isPermitted = realm.isPermitted(subject.id, new WildcardPermission('test:domain:action:read'))
    
    then:
    isPermitted
  }
  
  def 'negative result when subject has multiple roles that have multiple permissions of which none match'() {
    setup:
    def realm = new DatabaseRealm()
    realm.shiroPermissionResolver = new WildcardPermissionResolver()
    def subject = new SubjectBase(principal:'http://test.com!http://sp.test.com!1234').save()
    subject.addToPermissions("test:domain:action:id")
    subject.save()
    def role = new Role(name:'testrole').save()
    role.addToSubjects(subject)
    role.addToPermissions("test:domain:action:id")
    role.addToPermissions("test:otherdomain:action:*")
    role.addToPermissions("test:otherdomain:action:*")
    role.save()
    
    def role2 = new Role(name:'testrole2').save()
    role2.addToSubjects(subject)
    role2.addToPermissions("test:domain2:action:*")
    role2.addToPermissions("test:domain2:action:id")
    role2.addToPermissions("test:otherdomain2:action:*")
    role2.addToPermissions("test:otherdomain2:action:*")
    role2.save()
    
    def role3 = new Role(name:'testrole3').save()
    role3.addToSubjects(subject)
    role3.addToPermissions("test:domain3:action:*")
    role3.addToPermissions("test:domain3:action:id")
    role3.addToPermissions("test:otherdomain3:action:*")
    role3.addToPermissions("test:domain:action:read")
    role3.save()
    
    when:
    def isPermitted = realm.isPermitted(subject.id, new WildcardPermission('test:domain:action:write'))
    
    then:
    !isPermitted
  }
  
}