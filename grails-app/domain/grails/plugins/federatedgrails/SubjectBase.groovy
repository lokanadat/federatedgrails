package grails.plugins.federatedgrails

class SubjectBase {    

  String principal
  Map attributes = [:]
  
  boolean enabled
  
  static belongsTo = grails.plugins.federatedgrails.Role

  static hasMany = [ 
    sessionRecords: SessionRecord, 
    roles: Role, 
    permissions: String 
  ]

  static constraints = {
    principal(nullable: false, blank: false, unique:true)
  }

}
