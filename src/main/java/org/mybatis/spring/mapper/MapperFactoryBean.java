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

import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.support.SqlSessionDaoSupport;
import org.springframework.beans.factory.FactoryBean;

import static org.springframework.util.Assert.notNull;

/**
 * BeanFactory that enables injection of MyBatis mapper interfaces. It can be set up with a
 * SqlSessionFactory or a pre-configured SqlSessionTemplate.
 * <p>
 * Sample configuration:
 *
 * <pre class="code">
 * {@code
 *   <bean id="baseMapper" class="org.mybatis.spring.mapper.MapperFactoryBean" abstract="true" lazy-init="true">
 *     <property name="sqlSessionFactory" ref="sqlSessionFactory" />
 *   </bean>
 *
 *   <bean id="oneMapper" parent="baseMapper">
 *     <property name="mapperInterface" value="my.package.MyMapperInterface" />
 *   </bean>
 *
 *   <bean id="anotherMapper" parent="baseMapper">
 *     <property name="mapperInterface" value="my.package.MyAnotherMapperInterface" />
 *   </bean>
 * }
 * </pre>
 * <p>
 * Note that this factory can only inject <em>interfaces</em>, not concrete classes.
 *
 * @author Eduardo Macarron
 *
 * @see SqlSessionTemplate
 *
 *
 * 为了使用 MyBatis功能，示例中 Spring 配置文件提供了两上 bean 除了之前的分析的
 * SqlSessionFactoryBean 类型的 bean 以外，还有一个是 MapperFactoryBean 类型的 bean
 * 结合两个测试用例的综合分析，对于单独使用的 MyBatis 的时候调用数据库接口的方式是
 *
 *    UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
 *
 * 而这一个过程中，其实是 MyBatis 在获取映射的过程中根据配置信息为 UserMapper类型
 * 动态创建了代理类，而对于 Spring 的创建方式
 *
 *    UserMapper userMapper = （UserMapper） context.getBean("userMapper");
 *
 * Spring 中获取名为 userMapper 和 bean ，其实是与单独使用 Mybatis 完成了一样的功能，那么我们可以推断，在 bean 的创建过程中一定是使用了
 * MyBatis 中原生方法 sqlSession.getMapper(UserMapper.class) 进行了一次封装，结合配置文件，我们把分析目标转向 org.mybatis.Spring
 * mapper.MapperFactoyBean ，初步推测其中的逻辑应该在此类中实现，同样，还是首先查看的，初步推测其中的逻辑应该逻辑就方在此类中实现，
 * 同样，还是首先查看的类层次结构图，MapperFactoryBean ,如图 9-3 所示
 *  同样的，在实现的接口中发现了我们感兴趣的，两个接口InitializingBean 与 FactoryBean ，我们分析还是从 bean 的初始化开始
 *  MapperFactoryBean 的初始化
 *  因为实现了 InitializingBean 接口，Spring 会保证在 bean 的初始化的时首先调用 afterProperiesSet 方法来完成其初始化的逻辑，追踪这个
 *  父类，发现 afterPropertiesSet方法，是在 daoSuppoert 类中实现如下,
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
public class MapperFactoryBean<T> extends SqlSessionDaoSupport implements FactoryBean<T> {

  private Class<T> mapperInterface;

  private boolean addToConfig = true;

  public MapperFactoryBean() {
    //intentionally empty 
  }
  
  public MapperFactoryBean(Class<T> mapperInterface) {
    this.mapperInterface = mapperInterface;
  }

  /**
   * {@inheritDoc}
   * 但是从函数的名称来看我们大体推测 MapperFactoryBean 的初始化方法对 DAO 配置的验证以及 Dao 的初始化的工作，其中的 InitDao()
   * 方法的模板方法，设计为了久给子类做和些进一步的的逻辑处理
   *
   *
   *
   *
   * 结合代码我们了解到了对于 Dao 配置验证，Spring 做了以下的几个方面的工作
   * 父类中对于 sqlSession 不为空的验证
   *
   * sqlSession 作为根据接口的创建映射器代理接触类一定不可以为空，而 sqlSession 初始化的工作是在设定其 sqlSessionFactory 属性时完成的
   *
   * public void setSqlSessionFactory(SqlsessionFactory sqlSessionFactory){
   *     if(!this.externalSqlSession){
   *         this.sqlSession = new SqlSessionTemplate(sqlSessionFactory);
   *     }
   * }
   *
   * 也就是说，对于下面的配置，如果忽略了对于 sqlSessionFactory 属性的设置，那么在此会被检测出来
   *
   * <bean id="userMapper" class="org.mybatis.Spring.mapper.MapperFactoryBean">
   *    <property name="mapperInterface" vlaue="test.mybatis.dao.UserMapper"></property>
   *    <property name="sqlSessionFactory" ref="sqlSessionFactory"></property>
   * </bean>
   *
   * 映射接口的验证
   * 接口是映射器的基础，sqlSession 会根据接口动态的创建相应的代理类，所以接口必不可少的
   * 映射接口及验证
   * 对于函数，前半部分的验证我们都很容易理解，无非是配置文件中的属性是否存在做难，但是后面的部分完成了什么方面的验证呢，如果读者读过
   * MyBatis源码，你就会知道，在 MyBatis 实现有福利吗并没有手动调用 configuration.addMapper方法，而在在映射文件中读取过程中一旦
   * 解析到了如 <mapper namespace="Mapper.UserMapper"></mapper>，便会自动进行类型的映射注册，那么，Spring 中为什么会把这个功能
   * 单独拿出来验证呢，这是不是多此一举呢，
   *      在上面的函数中，configuration.AddMapper(this.mapperInterface)其实就是将 UserMapper 注册到映射类型中，如果你可以保证这个
   * 接口一定存在对应的映射文件，那么这个难并没有必要，但是，由于这个是我们自行决定的配置，无法保证这里配置的接口一定存在对应的映射文件
   * 所以这里非常有必要验证，在执行此代码的时候，MyBatis 会检查嵌入的映射接口是否存在对应的映射文件，如果没有回抛异常，Spring 正是用
   * 这种方式来完成接口的对应的映射文件存在性验证，
   *  获取 MapperFactoryBean
   *    由于 MapperFactoryBean 实现了 FactoryBean 接口，所以当通过 getBean 方法获取地对应实例的时候其实是获取该类的 getObject()
   * 函数返回实例
   *   这段代码正是我们提供了 MyBatis 独立使用的时候的一个代码调用，Spring 通过 FactoryBean 进行封装
   *
   *   MapperScannerConfigurer
   *   我们在 applicationContext.xml 中配置了 userMapper 供需要的使用，但是如果需要用到了映射器较多的话，采用这种配置方式的就显得很
   *  低效，为了解决这个问题，我们可以使用 MapperScannerConfigurer，让它扫描特定的包，自动的帮我们成批的创建映射器，这样一来，就能大
   *  大的减少配置的工作量，比如我们 applicationContext.xml ，文件的配置改成如下。
   *
   *
   *
   * <?xml version="1.0" encoding="UTF-8"?>
   * <beans xmlns="http://www.springframework.org/schema/beans"
   * xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   * xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd
   * ">
   *
   *
   *<bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource" init-method="init" destroy-method="close">
   *  <property name="url">
   *    <value>
   *      <![CDATA[${lsdapi.db.url}?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&zeroDateTimeBehavior=convertToNull]]>
   *    </value>
   *  </property>
   *  <property name="username" value="${lsdapi.db.username}" />
   *  <property name="password" value="${lsdapi.db.password}" />
   *  <property name="maxActive" value="100" />
   *  <property name="initialSize" value="1" />
   *  <property name="maxWait" value="6000" />
   *  <property name="minIdle" value="1" />
   *  <property name="testWhileIdle" value="true" />
   *  <property name="testOnBorrow" value="false" />
   *  <property name="testOnReturn" value="false" />
   *  <property name="validationQuery" value="select 1 from dual" />
   *  <property name="timeBetweenEvictionRunsMillis" value="60000" />
   *  <property name="minEvictableIdleTimeMillis" value="300000" />
   *  <property name="poolPreparedStatements" value="true" />
   *  <property name="maxOpenPreparedStatements" value="20" />
   *  <property name="proxyFilters">
   *  <list>
   *  <ref bean="stat-filter" />
   *  </list>
   *  </property>
   *</bean>
   *
   *<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
   *  <property name="dataSource" ref="dataSource" />
   *  <property name="configLocation" value="classpath:mybatis-config.xml" />
   *  <property name="mapperLocations" value="classpath:sqlmap/*.xml" />
   *</bean>
   *
   *
   *<bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
   *  <property name="sqlSessionFactoryBeanName" value="sqlSessionFactory"></property>
   *  <property name="basePackage" value="com.ls.lsd.api.dal.dao;com.ls.lsd.api.dal.daoV2"></property>
   *</bean>
   *
   *</beans>
   *
   * 上面的配置中，我们屏蔽掉了最原始的代码，（userMapper的创建，）而增加了MapperScannerConfigurer的配置，basePackage 属性是让你
   * 映射器接口文件设置基本包路径，你可以使用分号或逗号作为分隔符设置多于一个的包路径，每个映射器将会在指定包路径中递归地找搜索到，被发现
   * 的映射器将会使用Spring对自动侦测组默认的命名策略来命名的，也就是说，如果没有发现注解，它就是使用映射器非常大的写的非常完全限定类名
   * ，但是我发现了@component或JSR-330@Named注解，它获取名称
   *
   * 通过上面的配置，Spring就会帮助我们对test.mybatis.dao 下面的所有的接口进行自动的注入，而不需要为每个接口重复在Spring配置文件中进行
   * 声明了，那么这个功能又是如何做到的呢，MapperScanner Configurer中又是有哪些核心操作呢，同样，首先来看看类的层次结构，
   * 我们又看到了令人感兴趣的InitializaingBean 马上查找类的afterPropertiesSet方法来看看类的初始化逻辑
   *    public void afterPropertiesSet() throws Exception(){
   *        notNull(this.basePackage,"Property 'base Package' is required ");
   *    }
   *  很遗憾，分析并没有我们相像中的那样顺利，afterPropertiesSet()方法除了一句对basePackage 属性的验证代码外并没有太多的
   *  逻辑实现，好吧，让我们回过头看看MapperScannerConfigurer 类的层次结构图中感兴趣的接口，于是我们发现了
   *  BeanDefinitionRegistryPostProcessor 与BeanFactoryPostProcessor，Spring在初始化的过程中山同样的会保证这两个
   *  接口的调用
   *    首先来看看MapperScannerConfigurer类中对于BeanFactoryPostProcessor接口中的实现
   *    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory){
   *        // left intentionally blank
   *    }
   *
   *
   *
   *
   *
   *
   *
   *
   *
   *
   */
  @Override
  protected void checkDaoConfig() {

    notNull(this.mapperInterface, "Property 'mapperInterface' is required");

    Configuration configuration = getSqlSession().getConfiguration();
    if (this.addToConfig && !configuration.hasMapper(this.mapperInterface)) {
      try {
        configuration.addMapper(this.mapperInterface);
      } catch (Exception e) {
        logger.error("Error while adding the mapper '" + this.mapperInterface + "' to configuration.", e);
        throw new IllegalArgumentException(e);
      } finally {
        ErrorContext.instance().reset();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T getObject() throws Exception {
    return getSqlSession().getMapper(this.mapperInterface);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<T> getObjectType() {
    return this.mapperInterface;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSingleton() {
    return true;
  }

  //------------- mutators --------------

  /**
   * Sets the mapper interface of the MyBatis mapper
   *
   * @param mapperInterface class of the interface
   */
  public void setMapperInterface(Class<T> mapperInterface) {
    this.mapperInterface = mapperInterface;
  }

  /**
   * Return the mapper interface of the MyBatis mapper
   *
   * @return class of the interface
   */
  public Class<T> getMapperInterface() {
    return mapperInterface;
  }

  /**
   * If addToConfig is false the mapper will not be added to MyBatis. This means
   * it must have been included in mybatis-config.xml.
   * <p/>
   * If it is true, the mapper will be added to MyBatis in the case it is not already
   * registered.
   * <p/>
   * By default addToCofig is true.
   *
   * @param addToConfig
   */
  public void setAddToConfig(boolean addToConfig) {
    this.addToConfig = addToConfig;
  }

  /**
   * Return the flag for addition into MyBatis config.
   *
   * @return true if the mapper will be added to MyBatis in the case it is not already
   * registered.
   */
  public boolean isAddToConfig() {
    return addToConfig;
  }
}
