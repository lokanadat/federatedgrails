package grails.plugins.federatedgrails

class Role {
    String name

    static hasMany = [ subjects: SubjectBase, permissions: String ]

    static constraints = {
        name(nullable: false, blank: false, unique: true)
    }
}
