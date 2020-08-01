import com.example.javaioc.myannotation.MyAutoWired;
import com.example.javaioc.myannotation.MyService;

@MyService(value = "com/example/javaioc/test")
public class LoginServiceImpl implements LoginService {

    @MyAutoWired
    private LoginMapping loginMapping;
    @Override
    public String login() {
        return  loginMapping.login();
    }
}


