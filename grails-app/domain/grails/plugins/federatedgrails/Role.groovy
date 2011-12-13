package grails.plugins.federatedgrails

class Role {
    String name
    String description
    
    boolean protect

    static hasMany = [ subjects: SubjectBase, permissions: Permission ]

    static constraints = {
        name(nullable: false, blank: false, unique: true)
        description(nullable:true)
    }
}
