import com.example.javaioc.myannotation.MyAutoWired;
import com.example.javaioc.myannotation.MyController;
import com.example.javaioc.myannotation.Value;

@MyController
public class LoginController {

    @Value(value = "ioc.scan.pathTest")
    private String test;

    @MyAutoWired(value = "com/example/javaioc/test")
    private LoginService loginService;

    public String login() {
        return loginService.login();
    }

}
