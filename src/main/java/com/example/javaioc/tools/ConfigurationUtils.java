package com.example.javaioc.tools;


import com.example.javaioc.myannotation.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigurationUtils {
    public static Properties properties;

    private HashSet<String> classSet = new HashSet<String>();
    private Map<String,Object> iocBeanMap = new ConcurrentHashMap<String, Object>(32);
    public ConfigurationUtils(String propertiesPath)
    {
        properties = this.getBeanScanPath(propertiesPath);
    }

    private Properties getBeanScanPath(String propertiesPath)
    {
        if(propertiesPath.isEmpty())
        {
            propertiesPath ="src/main/resources/application.properties";
        }
        System.out.println("properties paht: "+propertiesPath);
        Properties properties = new Properties();
        InputStream in = ConfigurationUtils.class.getResourceAsStream(propertiesPath);
        try {
            System.out.println("loading application.properties");
            properties.load(in);
            return properties;
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if(in!=null)
                {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return properties;
    }

    public static Object getPropertiesByKey(String propertiesKey)
    {
        if(properties.size()>0)
            return properties.get(propertiesKey);
        return null;
    }

    private void classLoader() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        //加载配置文件所有配置信息
        String classScanPath = (String) this.properties.get("ioc.scan.path");

        //获取扫描包路径
        if(!classScanPath.isEmpty())
        {
            classScanPath = classScanPath.replace(".","/");
        }
        else
        {
            throw new RuntimeException("请配置项目包扫描路径 ioc.scan.path");
        }

        //扫描项目根目录中所有的clas文件
        getPackageClassFile(classScanPath);
        for(String className : classSet )
        {
            addServiceToIoc(Class.forName(className)); //反射机制通过jvm的classLoader反射类
        }

        //将带有MyService注解类的所有带有MyAutowired注解的属性 进行实例化
        Set<String> beanKeySet = iocBeanMap.keySet();
        for(String beanName : beanKeySet){
            addAutowiredToField(iocBeanMap.get(beanName));
        }

    }


    //将所有以class结尾的文件 添加到全局的set中 包的全路径名——>java反射class.forName
    private void getPackageClassFile(String packageName)
    {
        //返回文件的URL对象
        URL url = this.getClass().getClassLoader().getResource(packageName);
        System.out.println("将所有以class结尾的文件 添加到全局的set中:"+packageName);

        assert url != null;
        File file = new File(url.getFile());

        if(file.exists() && file.isDirectory())
        {
            File[] files = file.listFiles();
            for(File fileSon : files)
            {
                if(fileSon.isDirectory())
                {
                    getPackageClassFile(packageName+"/"+fileSon.getName());
                }
                else
                {
                    if (fileSon.getName().endsWith(".class")) {
                        System.out.println("正在加载: " + packageName.replace("/", ".")
                                + "." + fileSon.getName());
                        classSet.add(packageName.replace("/", ".") + "."
                                + fileSon.getName().replace(".class", ""));
                    }
                }
            }
        }
        else
        {
            throw new RuntimeException("no directory to scan");
        }

    }


    //将注解实例注入ioc容器中
    private void addServiceToIoc(Class clazz) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        if(clazz.getAnnotation(MyController.class)!=null)
        {
            iocBeanMap.put(toLowercaseIndex(clazz.getSimpleName()),clazz.getDeclaredConstructor().newInstance());
            System.out.println("将注解实例注入ioc容器中: "+toLowercaseIndex(clazz.getSimpleName()));
        }
        else if(clazz.getAnnotation(MyService.class)!=null)
        {
            MyService myService = (MyService)clazz.getAnnotation(MyService.class);
            iocBeanMap.put(myService.value().isEmpty() ? toLowercaseIndex(clazz.getSimpleName()) :
                    toLowercaseIndex(myService.value()), clazz.getDeclaredConstructor().newInstance());
            System.out.println("将注解实例注入ioc容器中: "+clazz.getSimpleName());
        }
        else if(clazz.getAnnotation(MyMapping.class)!=null)
        {
            MyMapping myMapping = (MyMapping) clazz.getAnnotation(MyMapping.class);
            iocBeanMap.put(myMapping.value().isEmpty() ? toLowercaseIndex(clazz.getSimpleName()) :
                    toLowercaseIndex(myMapping.value()), clazz.getDeclaredConstructor().newInstance());
            System.out.println("将注解实例注入ioc容器中: "+clazz.getSimpleName());

        }
    }

    //类名首字母小写
    public static String toLowercaseIndex(String name)
    {
        if(!name.isEmpty())
            return name.substring(0,1).toLowerCase()+name.substring(1,name.length());
        return name;
    }


    //依赖注入
    private void addAutowiredToField(Object obj) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        //Field java.lang.reflect包下。在Java反射中
        // Field类描述的是类的属性信息
        //获取当前对象的成员变量的类型
        //对成员变量重新设值
        //获取对象的成员变量
        Field[] fields = obj.getClass().getDeclaredFields();
        for(Field field : fields)
        {

            if(field.getAnnotation(MyAutoWired.class)!=null)
            {
                field.setAccessible(true);
                MyAutoWired myAutoWired = field.getAnnotation(MyAutoWired.class);
                Class<?> fieldClass = field.getType();
                //接口无法实例化
                if(fieldClass.isInterface())
                {
                    if(!myAutoWired.value().isEmpty()) {
                        field.set(obj, iocBeanMap.get(myAutoWired.value()));
                        System.out.println("依赖注入 value实例类-实现接口"+field.getName());
                    }
                    else
                    {
                        List<Object> list = findSuperInterfaceByIoc(fieldClass);
                        if (list.size() > 0) {
                            if (list.size() > 1) {
                                throw new RuntimeException(obj.getClass() + "  注入接口 " + field.getType() + "   失败，请在注解中指定需要注入的具体实现类");
                            } else {
                                field.set(obj, list.get(0));
                                // 递归依赖注入
                                System.out.println("依赖注入-单接口"+field.getName());
                                addAutowiredToField(field.getType());
                            }
                        } else {
                            throw new RuntimeException("当前类" + obj.getClass() + "  不能注入接口 " + field.getType().getClass() + "  ， 接口没有实现类不能被实例化");
                        }
                    }
                }else //加载类实例
                {
                    String beanName = myAutoWired.value().isEmpty() ?
                            toLowercaseIndex(field.getName()) : toLowercaseIndex(myAutoWired.value());
                    Object beanObj = iocBeanMap.get(beanName);

                    //成员变量在ioc容器中未被实例化时 新建实例
                    field.set(obj, beanObj == null ? field.getType().getDeclaredConstructor().newInstance() : beanObj);
                    System.out.println("依赖注入类实例"+field.getName());
                }
                addAutowiredToField(field.getType());
            }

            //配置文件 properties
            if(field.getAnnotation(Value.class)!=null)
            {
                field.setAccessible(true);
                Value value = field.getAnnotation(Value.class);
                field.set(obj, !value.value().isEmpty() ? getPropertiesByKey(value.value()) : null);
                System.out.println("loading 配置文件"+obj.getClass()+"loading 配置属性"+value.value());
            }
        }
    }


    private List<Object> findSuperInterfaceByIoc(Class clazz)
    {
        Set<String> beanNameList = iocBeanMap.keySet();
        ArrayList<Object> objectArrayList =new ArrayList<Object>();

        //all bean
        for(String beanName : beanNameList)
        {
            Object obj = iocBeanMap.get(beanName);
            Class<?>[] interfaces = obj.getClass().getInterfaces();
            for(Class<?> inter :interfaces)
            {
                //implement the interface: clazz
                if(inter.isAssignableFrom(clazz))
                {
                    objectArrayList.add(obj);
                    break;
                }
            }
        }
        return objectArrayList;
    }

}
