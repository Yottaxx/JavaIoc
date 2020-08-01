import com.example.javaioc.tools.ConfigurationUtils;

import java.lang.reflect.Method;

public class PlatformApplication {

    public static void main(String[] args) throws Exception {
        // 从容器中获取对象(自动首字母小写)
        ConfigurationUtils configurationUtils = new ConfigurationUtils("");
        Method method = configurationUtils.getClass().getMethod("classLoader");
        method.invoke(configurationUtils);
    }

}