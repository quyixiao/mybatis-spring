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

import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.support.SqlSessionDaoSupport;
import org.springframework.beans.factory.FactoryBean;

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
