/**
 *    Copyright 2010-2016 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.spring.mapper;

import static org.springframework.util.Assert.notNull;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;

/**
 * BeanDefinitionRegistryPostProcessor that searches recursively starting from a base package for
 * interfaces and registers them as {@code MapperFactoryBean}. Note that only interfaces with at
 * least one method will be registered; concrete classes will be ignored.
 * <p>
 * This class was a {code BeanFactoryPostProcessor} until 1.0.1 version. It changed to  
 * {@code BeanDefinitionRegistryPostProcessor} in 1.0.2. See https://jira.springsource.org/browse/SPR-8269
 * for the details.
 * <p>
 * The {@code basePackage} property can contain more than one package name, separated by either
 * commas or semicolons.
 * <p>
 * This class supports filtering the mappers created by either specifying a marker interface or an
 * annotation. The {@code annotationClass} property specifies an annotation to search for. The
 * {@code markerInterface} property specifies a parent interface to search for. If both properties
 * are specified, mappers are added for interfaces that match <em>either</em> criteria. By default,
 * these two properties are null, so all interfaces in the given {@code basePackage} are added as
 * mappers.
 * <p>
 * This configurer enables autowire for all the beans that it creates so that they are
 * automatically autowired with the proper {@code SqlSessionFactory} or {@code SqlSessionTemplate}.
 * If there is more than one {@code SqlSessionFactory} in the application, however, autowiring
 * cannot be used. In this case you must explicitly specify either an {@code SqlSessionFactory} or
 * an {@code SqlSessionTemplate} to use via the <em>bean name</em> properties. Bean names are used
 * rather than actual objects because Spring does not initialize property placeholders until after
 * this class is processed. 
 * <p>
 * Passing in an actual object which may require placeholders (i.e. DB user password) will fail. 
 * Using bean names defers actual object creation until later in the startup
 * process, after all placeholder substituation is completed. However, note that this configurer
 * does support property placeholders of its <em>own</em> properties. The <code>basePackage</code>
 * and bean name properties all support <code>${property}</code> style substitution.
 * <p>
 * Configuration sample:
 * <p>
 *
 * <pre class="code">
 * {@code
 *   <bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
 *       <property name="basePackage" value="org.mybatis.spring.sample.mapper" />
 *       <!-- optional unless there are multiple session factories defined -->
 *       <property name="sqlSessionFactoryBeanName" value="sqlSessionFactory" />
 *   </bean>
 * }
 * </pre>
 *
 * @author Hunter Presnall
 * @author Eduardo Macarron
 *
 * @see MapperFactoryBean
 * @see ClassPathMapperScanner
 */
public class MapperScannerConfigurer implements BeanDefinitionRegistryPostProcessor, InitializingBean, ApplicationContextAware, BeanNameAware {

  private String basePackage;

  private boolean addToConfig = true;

  private SqlSessionFactory sqlSessionFactory;

  private SqlSessionTemplate sqlSessionTemplate;

  private String sqlSessionFactoryBeanName;

  private String sqlSessionTemplateBeanName;

  private Class<? extends Annotation> annotationClass;

  private Class<?> markerInterface;

  private ApplicationContext applicationContext;

  private String beanName;

  private boolean processPropertyPlaceHolders;

  private BeanNameGenerator nameGenerator;

  /**
   * This property lets you set the base package for your mapper interface files.
   * <p>
   * You can set more than one package by using a semicolon or comma as a separator.
   * <p>
   * Mappers will be searched for recursively starting in the specified package(s).
   *
   * @param basePackage base package name
   */
  public void setBasePackage(String basePackage) {
    this.basePackage = basePackage;
  }

  /**
   * Same as {@code MapperFactoryBean#setAddToConfig(boolean)}.
   *
   * @param addToConfig
   * @see MapperFactoryBean#setAddToConfig(boolean)
   */
  public void setAddToConfig(boolean addToConfig) {
    this.addToConfig = addToConfig;
  }

  /**
   * This property specifies the annotation that the scanner will search for.
   * <p>
   * The scanner will register all interfaces in the base package that also have the
   * specified annotation.
   * <p>
   * Note this can be combined with markerInterface.
   *
   * @param annotationClass annotation class
   */
  public void setAnnotationClass(Class<? extends Annotation> annotationClass) {
    this.annotationClass = annotationClass;
  }

  /**
   * This property specifies the parent that the scanner will search for.
   * <p>
   * The scanner will register all interfaces in the base package that also have the
   * specified interface class as a parent.
   * <p>
   * Note this can be combined with annotationClass.
   *
   * @param superClass parent class
   */
  public void setMarkerInterface(Class<?> superClass) {
    this.markerInterface = superClass;
  }

  /**
   * Specifies which {@code SqlSessionTemplate} to use in the case that there is
   * more than one in the spring context. Usually this is only needed when you
   * have more than one datasource.
   * <p>
   * @deprecated Use {@link #setSqlSessionTemplateBeanName(String)} instead
   *
   * @param sqlSessionTemplate
   */
  @Deprecated
  public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
    this.sqlSessionTemplate = sqlSessionTemplate;
  }

  /**
   * Specifies which {@code SqlSessionTemplate} to use in the case that there is
   * more than one in the spring context. Usually this is only needed when you
   * have more than one datasource.
   * <p>
   * Note bean names are used, not bean references. This is because the scanner
   * loads early during the start process and it is too early to build mybatis
   * object instances.
   *
   * @since 1.1.0
   *
   * @param sqlSessionTemplateName Bean name of the {@code SqlSessionTemplate}
   */
  public void setSqlSessionTemplateBeanName(String sqlSessionTemplateName) {
    this.sqlSessionTemplateBeanName = sqlSessionTemplateName;
  }

  /**
   * Specifies which {@code SqlSessionFactory} to use in the case that there is
   * more than one in the spring context. Usually this is only needed when you
   * have more than one datasource.
   * <p>
   * @deprecated Use {@link #setSqlSessionFactoryBeanName(String)} instead.
   *
   * @param sqlSessionFactory
   */
  @Deprecated
  public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
  }

  /**
   * Specifies which {@code SqlSessionFactory} to use in the case that there is
   * more than one in the spring context. Usually this is only needed when you
   * have more than one datasource.
   * <p>
   * Note bean names are used, not bean references. This is because the scanner
   * loads early during the start process and it is too early to build mybatis
   * object instances.
   *
   * @since 1.1.0
   *
   * @param sqlSessionFactoryName Bean name of the {@code SqlSessionFactory}
   */
  public void setSqlSessionFactoryBeanName(String sqlSessionFactoryName) {
    this.sqlSessionFactoryBeanName = sqlSessionFactoryName;
  }

  /**
   *
   * @since 1.1.1
   *
   * @param processPropertyPlaceHolders
   */
  public void setProcessPropertyPlaceHolders(boolean processPropertyPlaceHolders) {
    this.processPropertyPlaceHolders = processPropertyPlaceHolders;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setBeanName(String name) {
    this.beanName = name;
  }

  /**
   * Gets beanNameGenerator to be used while running the scanner.
   *
   * @return the beanNameGenerator BeanNameGenerator that has been configured
   * @since 1.2.0
   */
  public BeanNameGenerator getNameGenerator() {
    return nameGenerator;
  }

  /**
   * Sets beanNameGenerator to be used while running the scanner.
   *
   * @param nameGenerator the beanNameGenerator to set
   * @since 1.2.0
   */
  public void setNameGenerator(BeanNameGenerator nameGenerator) {
    this.nameGenerator = nameGenerator;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void afterPropertiesSet() throws Exception {
    notNull(this.basePackage, "Property 'basePackage' is required");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    // left intentionally blank
  }

  /**
   * {@inheritDoc}
   * 
   * @since 1.0.2
   * 没有任何逻辑的实现，只能说明我们找错地方了，继续找，查看MapperScannerConfigurer类中对于BeanDefinitionRegisterPostProcessor
   * 接口中的实现。
   *
   * Bingo! 这次找对地方了，大致看一下代码的实现，正是完成对指定路径的扫描的逻辑，那么，我们就以此为入口，详细的分析
   * MapperScannerConfigurer所提供的逻辑实现。
   *
   *
   */
  @Override
  public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
    if (this.processPropertyPlaceHolders) {
      // processPropertyPlaceHolders属性的处理
      // 首先，难题就是processPropertyPlaceHolders属性的处理，或许读者并未过多的接触此属性，我们只看看查看
      // processPropertyPlaceHolders()函数搂反推此属性的代表的功能。

      processPropertyPlaceHolders();
    }

    ClassPathMapperScanner scanner = new ClassPathMapperScanner(registry);
    scanner.setAddToConfig(this.addToConfig);
    scanner.setAnnotationClass(this.annotationClass);
    scanner.setMarkerInterface(this.markerInterface);
    scanner.setSqlSessionFactory(this.sqlSessionFactory);
    scanner.setSqlSessionTemplate(this.sqlSessionTemplate);
    scanner.setSqlSessionFactoryBeanName(this.sqlSessionFactoryBeanName);
    scanner.setSqlSessionTemplateBeanName(this.sqlSessionTemplateBeanName);
    scanner.setResourceLoader(this.applicationContext);
    scanner.setBeanNameGenerator(this.nameGenerator);
    scanner.registerFilters();
    scanner.scan(StringUtils.tokenizeToStringArray(this.basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
  }

  /*
   * BeanDefinitionRegistries are called early in application startup, before
   * BeanFactoryPostProcessors. This means that PropertyResourceConfigurers will not have been
   * loaded and any property substitution of this class' properties will fail. To avoid this, find
   * any PropertyResourceConfigurers defined in the context and run them on this class' bean
   * definition. Then update the values.
   *
   *
   * 首先，难题就是processPropertyPlaceHolders属性的处理，或许读者并未过多的接触属性，我们只能查看
   * processPropertyPlaceHolders()函数来反推此属性所代表的功能
   *
   * 不知道读者是否悟出了此函数的作用呢，或许此函数的说明会给我们一些提示，BeanDefinitionRegistries会在应用启动的时候调用
   * ，并且会早于BeanFactoryPostProcessors的调用，这就意味着PropertyResourceConfigurers还没有被加载所有的属性的文件引用
   * 将会失效
   *  为了避免此种情况的发生，此方法手动的找出定义的PropertyResourceConfigurers 并进行提前调用以保证于属性的引用可以正常
   *  工作的。
   *  我想读者已经有所感悟了，结合之前讲过的PropertyResourceConfigurer的用法，举例说明一下，如果要创建配置文件如test.properties
   *  并添加属性对
   *   basePackage=test.mybatis.dao
   *  然后Spring的配置文件中加入了属性文件解析器
   *  <bean id="mesHandler" class="org.Springframework.beans.factory.config.PropertyPlaceholderConfigurer">
   *    <property>
   *         <list>
   *             <value="">config/test.properties</value>
   *         </list>
   *    </property>
   *  </bean>
   *
   *  修改MapperScannerConfigurer类型中的bean的定义
   *
   *  <bean class="org.mybatis.Spring.mapper.MapperScannerConfigurer">
   *      <property name="basePackage" value="${basePackage}"/>
   *  </bean>
   *
   *
   *  此时你会发现，这个配置并没有达到预期的效果，因为在解析${basePackage}的时候PropertyPlaceholderConfigurer 还没有被调用的
   *  ，也就是属性文件中的属性还没有加载到内存中，Spring还是不能直接使用它的，为了解决这个问题，Spring提供了processPropertyPlaceHolders
   *  属性，你需要这样配置MapperScannerConfigurer类型的bean
   *
   *  <bean class="org.mybatis.Spring.mapper.MapperScannerConfigurer">
   *      <property name="basePackage" value="test.mybatis.dao"/>
   *      <property name="processPropertyPlaceHolders" value="true"/>
   *  </bean>
   *
   *  通过processPropertyPlaceHolders 属性的配置，将程序引入 我们正在分析的processPropertyPlaceHolder函数中来完成属性文件的加载
   *  ，至此，我们终于理清了这个属性的作用，再次回顾这个函数的所有事情
   *
   *  1.找到所有已经注册的PropertyResourceConfigurer类型的bean
   *  2.模似Spring中的环境用来处理器，这里通过使用new DefaultListtableBeanFactory()来模拟Spring中的环境，完成处理器的调用后便
   *  失效，将映射的bean ，也就是MapperScannerConfigurer类型的bean注册到环境中来进行后处理器调用，处理器PropertyPlaceHolderConfigurer
   *  调用完成的功能，即找出所有的bean 中完成应用属性文件的变量替换，也就是说，在处理调用后，模拟环境拟的MapperScannerConfigurer类型
   *  的bean如果有引入属性那已经替换了，这时，再将模拟bean中相关的属性提取出来应用在真实的bean中
   *
   *  根据配置属性生成过虑器
   *  在postProcessBeanDefinitionRegistry方法中可以看到，配置中支持多属性的设定，但是我们感兴趣的或者说影响扫描结果的并不多，属性设置
   *  后通过在scanner.registerFilters()代码中生成对就的过虑器来控制扫描结果
   *
   *
   *
   *
   */
  private void processPropertyPlaceHolders() {
    Map<String, PropertyResourceConfigurer> prcs = applicationContext.getBeansOfType(PropertyResourceConfigurer.class);

    if (!prcs.isEmpty() && applicationContext instanceof ConfigurableApplicationContext) {
      BeanDefinition mapperScannerBean = ((ConfigurableApplicationContext) applicationContext)
          .getBeanFactory().getBeanDefinition(beanName);

      // PropertyResourceConfigurer does not expose any methods to explicitly perform
      // property placeholder substitution. Instead, create a BeanFactory that just
      // contains this mapper scanner and post process the factory.
      DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
      factory.registerBeanDefinition(beanName, mapperScannerBean);

      for (PropertyResourceConfigurer prc : prcs.values()) {
        prc.postProcessBeanFactory(factory);
      }

      PropertyValues values = mapperScannerBean.getPropertyValues();

      this.basePackage = updatePropertyValue("basePackage", values);
      this.sqlSessionFactoryBeanName = updatePropertyValue("sqlSessionFactoryBeanName", values);
      this.sqlSessionTemplateBeanName = updatePropertyValue("sqlSessionTemplateBeanName", values);
    }
  }

  private String updatePropertyValue(String propertyName, PropertyValues values) {
    PropertyValue property = values.getPropertyValue(propertyName);

    if (property == null) {
      return null;
    }

    Object value = property.getValue();

    if (value == null) {
      return null;
    } else if (value instanceof String) {
      return value.toString();
    } else if (value instanceof TypedStringValue) {
      return ((TypedStringValue) value).getValue();
    } else {
      return null;
    }
  }

}
