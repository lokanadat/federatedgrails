package grails.plugins.federatedgrails

public class FederatedToken implements org.apache.shiro.authc.AuthenticationToken {

    def principal, credential, attributes, remoteHost, userAgent

  public Object getPrincipal() {
      return this.principal
  }
  
    public Object getCredentials() {
      return null
  }
}