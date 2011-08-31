package grails.plugins.federatedgrails

class SessionRecord {
  
  static belongsTo = [subject: SubjectBase]

  String credential
  String remoteHost
  String userAgent
  
  Date dateCreated

  static constraints = {
    credential(nullable: false, blank: false)
    remoteHost(nullable: false, blank: false)
    userAgent(nullable: false, blank: false)
    }
}