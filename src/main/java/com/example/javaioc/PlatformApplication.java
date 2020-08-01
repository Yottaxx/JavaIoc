package com.example.javaioc;

import com.example.javaioc.tools.ConfigurationUtils;
import java.lang.reflect.Method;

public class PlatformApplication {

    public static void main(String[] args) throws Exception {
        // 从容器中获取对象(自动首字母小写)
//        ConfigurationUtils configurationUtils = new ConfigurationUtils("/application.properties");
//        Method method = configurationUtils.getClass().getDeclaredMethod("classLoader");
//        method.invoke(configurationUtils);

//        Class clazz = Class.forName("com.example.javaioc.tools.ConfigurationUtils");
//        Class configurationUtils =(Class) clazz.getDeclaredConstructor(String.class).newInstance("/application.properties");
//        Method method = configurationUtils.getDeclaredMethod("classLoader");
//        method.setAccessible(true);
//        method.invoke(configurationUtils);

        ClassLoader loader =Thread.currentThread().getContextClassLoader();
        Class clazz = loader.loadClass("com.example.javaioc.tools.ConfigurationUtils");
        ConfigurationUtils configurationUtils=(ConfigurationUtils)
                clazz.getDeclaredConstructor(String.class).newInstance("/application.properties");
        Method method = clazz.getDeclaredMethod("classLoader");
        method.setAccessible(true);
        method.invoke(configurationUtils);
    }

}