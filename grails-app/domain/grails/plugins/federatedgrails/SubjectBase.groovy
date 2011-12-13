package grails.plugins.federatedgrails

class SubjectBase {    

  String principal
  boolean enabled
  
  static belongsTo = grails.plugins.federatedgrails.Role

  static hasMany = [ 
    sessionRecords: SessionRecord, 
    roles: Role, 
    permissions: Permission 
  ]

  static constraints = {
    principal(nullable: false, blank: false, unique:true)
  }

}
