import com.example.Subject

class ExampleController {

    def index = {
      def subjects = Subject.list()
      [subjects:subjects]
    }

}
