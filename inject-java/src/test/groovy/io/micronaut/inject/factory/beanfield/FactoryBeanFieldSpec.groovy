package io.micronaut.inject.factory.beanfield

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Prototype
import io.micronaut.core.annotation.AnnotationUtil
import io.micronaut.core.util.CollectionUtils
import io.micronaut.inject.BeanDefinition
import io.micronaut.inject.qualifiers.Qualifiers
import spock.lang.Issue
import spock.lang.Unroll

class FactoryBeanFieldSpec extends AbstractTypeElementSpec {

    void "test fail compilation for AOP advice for primitive array type from field"() {
        when:
        buildBeanDefinition('primitive.fields.factory.errors.PrimitiveFactory',"""
package primitive.fields.factory.errors;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import io.micronaut.aop.simple.Mutating;

@Factory
class PrimitiveFactory {
    @Bean
    @Named("totals")
    @Mutating("test")
    int[] totals = { 10 };
}
""")

        then:
        def e = thrown(RuntimeException)
        e.message.contains("Cannot apply AOP advice to arrays")
    }

    void "test fail compilation for AOP advice to primitive type from field"() {
        when:
        buildBeanDefinition('primitive.fields.factory.errors.PrimitiveFactory',"""
package primitive.fields.factory.errors;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import io.micronaut.aop.simple.Mutating;

@Factory
class PrimitiveFactory {
    @Bean
    @Named("total")
    @Mutating("test")
    int totals = 10;
}
""")

        then:
        def e = thrown(RuntimeException)
        e.message.contains("Cannot apply AOP advice to primitive beans")
    }

    void "test fail compilation when defining preDestroy for primitive type from field"() {
        when:
        buildBeanDefinition('primitive.fields.factory.errors.PrimitiveFactory',"""
package primitive.fields.factory.errors;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import io.micronaut.aop.simple.Mutating;

@Factory
class PrimitiveFactory {
    @Bean(preDestroy="close")
    @Named("total")
    int totals = 10;
}
""")

        then:
        def e = thrown(RuntimeException)
        e.message.contains("Using 'preDestroy' is not allowed on primitive type beans")
    }

    @Unroll
    void "test produce bean for primitive #primitiveType array type from field"() {
        given:
        def context = buildContext("""
package primitive.fields.factory;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Factory
class PrimitiveFactory {
    @Bean
    @Named("totals")
    $primitiveType[] totals = { 10 };
}

@Singleton
class MyBean {
    public final $primitiveType[] totals;

    @Inject
    @Named("totals")
    public $primitiveType[] totalsFromField;

    public $primitiveType[] totalsFromMethod;

    MyBean(@Named $primitiveType[] totals) {
        this.totals = totals;
    }
    
    @Inject
    void setTotals(@Named $primitiveType[] totals) {
        this.totalsFromMethod = totals;
    }
}
""")

        def bean = getBean(context, 'primitive.fields.factory.MyBean')

        expect:
        bean.totals[0] == 10
        bean.totalsFromField[0] == 10
        bean.totalsFromMethod[0] == 10

        where:
        primitiveType << ['int', 'short', 'long', 'double', 'float', 'byte']
    }

    @Unroll
    void "test produce bean for primitive #primitiveType matrix array type from field"() {
        given:
        def context = buildContext("""
package primitive.fields.factory;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Factory
class PrimitiveFactory {
    @Bean
    @Named("totals")
    $primitiveType[][] totals = { new $primitiveType[] { 10 } };
}

@Singleton
class MyBean {
    public final $primitiveType[][] totals;

    @Inject
    @Named("totals")
    public $primitiveType[][] totalsFromField;

    public $primitiveType[][] totalsFromMethod;

    MyBean(@Named $primitiveType[][] totals) {
        this.totals = totals;
    }
    
    @Inject
    void setTotals(@Named $primitiveType[][] totals) {
        this.totalsFromMethod = totals;
    }
}
""")

        def bean = getBean(context, 'primitive.fields.factory.MyBean')

        expect:
        bean.totals[0][0] == 10
        bean.totalsFromField[0][0] == 10
        bean.totalsFromMethod[0][0] == 10

        where:
        primitiveType << ['int', 'short', 'long', 'double', 'float', 'byte']
    }

    @Unroll
    void "test produce bean for primitive #primitiveType type from field"() {
        given:
        def context = buildContext("""
package primitive.fields.factory;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Factory
class PrimitiveFactory {
    @Bean
    @Named("total")
    $primitiveType total = 10;
}

@Singleton
class MyBean {
    public final $primitiveType total;

    @Inject
    @Named("total")
    public $primitiveType totalFromField;

    public $primitiveType totalFromMethod;

    MyBean(@Named $primitiveType total) {
        this.total = total;
    }
    
    @Inject
    void setTotal(@Named $primitiveType total) {
        this.totalFromMethod = total;
    }
}
""")

        def bean = getBean(context, 'primitive.fields.factory.MyBean')

        expect:
        bean.total == 10
        bean.totalFromField == 10

        where:
        primitiveType << ['int', 'short', 'long', 'double', 'float', 'byte']
    }

    void "test a factory bean can be supplied from a field"() {
        given:
        ApplicationContext context = buildContext('test.TestFactory$TestField', '''\
package test;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import io.micronaut.inject.annotation.*;
import io.micronaut.aop.*;
import io.micronaut.context.annotation.*;
import io.micronaut.inject.factory.enummethod.TestEnum;
import jakarta.inject.*;
import java.util.Locale;
import jakarta.inject.Singleton;

@Factory
class TestFactory$TestField {

    @Singleton
    @Bean
    @io.micronaut.context.annotation.Primary
    Foo one = new Foo("one");
    
    // final fields are implicitly singleton
    @Bean
    @Named("two")
    final Foo two = new Foo("two");
    
    // non-final fields are prototype
    @Bean
    @Named("three")
    Foo three = new Foo("three");
    
    @SomeMeta
    @Bean
    Foo four = new Foo("four");
    
    @Bean
    @Mutating
    Bar bar = new Bar();
}

class Bar {
    public String test(String test) {
        return test;
    }
}

class Foo {
    final String name;
    Foo(String name) {
        this.name = name;
    }
}

@Retention(RUNTIME)
@Singleton
@Named("four")
@AroundConstruct
@interface SomeMeta {
}

@Retention(RUNTIME)
@Singleton
@Around
@interface Mutating {
}

@Singleton
@InterceptorBean(SomeMeta.class)
class TestConstructInterceptor implements ConstructorInterceptor<Object> {
    boolean invoked = false;
    Object[] parameters;
    
    @Override
    public Object intercept(ConstructorInvocationContext<Object> context) {
        invoked = true;
        parameters = context.getParameterValues();
        return context.proceed();
    }
} 

@InterceptorBean(Mutating.class)
class TestInterceptor implements MethodInterceptor<Object, Object> {
    @Override public Object intercept(MethodInvocationContext<Object, Object> context) {
        final Object[] parameterValues = context.getParameterValues();
        parameterValues[0] = parameterValues[0].toString().toUpperCase(Locale.ENGLISH);
        System.out.println(parameterValues[0]);
        return context.proceed();
    }
}
''')

        def barBean = getBean(context, 'test.Bar')

        expect:
        barBean.test("good") == 'GOOD' // proxied
        getBean(context, "test.Foo").name == 'one'
        getBean(context, "test.Foo", Qualifiers.byName("two")).name == 'two'
        getBean(context, "test.Foo", Qualifiers.byName("two")).is(
                getBean(context, "test.Foo", Qualifiers.byName("two"))
        )
        getBean(context, "test.Foo", Qualifiers.byName("three")).is(
                getBean(context, "test.Foo", Qualifiers.byName("three"))
        )
        getBean(context, 'test.TestConstructInterceptor').invoked == false
        getBean(context, "test.Foo", Qualifiers.byName("four")) // around construct
        getBean(context, 'test.TestConstructInterceptor').invoked == true

        cleanup:
        context.close()
    }

    @Unroll
    void 'test fail compilation on invalid modifier #modifier'() {
        when:
        buildBeanDefinition('invalidmod.TestFactory', """
package invalidmod;

import io.micronaut.inject.annotation.*;
import io.micronaut.context.annotation.*;
import jakarta.inject.*;

@Factory
class TestFactory {
    @Bean
    $modifier Test test;
}

class Test {}
""")

        then:
        def e = thrown(RuntimeException)
        e.message.contains("cannot be ")
        e.message.contains(modifier)

        where:
        modifier << ['private', 'protected', 'static']
    }

    void "if a factory field bean defines Bean and Prototype scopes and the original bean type scope is Singleton BeanDefinition getScope returns Prototype and BeanDefinition getAnnotationNamesByStereotype for javax.inject.Scope returns Prototype The original qualifier is not present if the factory field bean does not define a Qualifier"() {

        given:
        ApplicationContext context = buildContext('test.TestFactory$TestField', '''\
package test;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import io.micronaut.inject.annotation.*;
import io.micronaut.aop.*;
import io.micronaut.context.annotation.*;
import jakarta.inject.*;
import jakarta.inject.Singleton;

@Some
@Factory
class TestFactory$TestField {

    @Bean
    @Prototype
    Bar1 bar = new Bar1();
}

@Abc
@Singleton
class Bar1 {
}

@Retention(RUNTIME)
@Qualifier
@interface Abc {
}

@Retention(RUNTIME)
@Qualifier
@interface Some {
}

''')

        when:
        BeanDefinition bar1BeanDefinition = context.getBeanDefinitions(context.classLoader.loadClass('test.Bar1'))
                .find { it.getDeclaringType().get().simpleName.contains("TestFactory") }
        then:
        bar1BeanDefinition.getScope().get() == Prototype.class
        bar1BeanDefinition.declaredQualifier == null
        bar1BeanDefinition.getAnnotationNamesByStereotype(AnnotationUtil.SCOPE).size() == 1

        when:
        Collection<String> annotationNamesByScopeStereoType = bar1BeanDefinition.getAnnotationNamesByStereotype(AnnotationUtil.SCOPE).asCollection()

        then:
        annotationNamesByScopeStereoType.any {it == "io.micronaut.context.annotation.Prototype" }

        cleanup:
        context.close()
    }

    void "if a factory field bean defines a @Bean and @Singleton scopes, BeanDefinition::getScope return empty but BeanDefinition::getAnnotationNamesByStereotype for javax.inject.Scope returns javax.inject.Singleton The original qualifier is not present if the factory field bean does not define a Qualifier"() {
        given:
        ApplicationContext context = buildContext('test.TestFactory$TestField', '''\
package test;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import io.micronaut.inject.annotation.*;
import io.micronaut.aop.*;
import io.micronaut.context.annotation.*;
import jakarta.inject.*;
import jakarta.inject.Singleton;

@Some
@Factory
class TestFactory$TestField {
    @Bean
    @Singleton
    Bar2 bar2 = new Bar2();
}

@Abc
class Bar2 {
}

@Retention(RUNTIME)
@Qualifier
@interface Abc {
}

@Retention(RUNTIME)
@Qualifier
@interface Some {
}
''')
        when:
        BeanDefinition bar2BeanDefinition = context.getBeanDefinitions(context.classLoader.loadClass('test.Bar2'))
                .find { it.getDeclaringType().get().simpleName.contains("TestFactory") }

        then:
        !bar2BeanDefinition.getScope().isPresent() // javax.inject.Singleton is not present :-/
        bar2BeanDefinition.singleton
        bar2BeanDefinition.declaredQualifier == null
        bar2BeanDefinition.getAnnotationNamesByStereotype(AnnotationUtil.SCOPE).size() == 1
        bar2BeanDefinition.getAnnotationNamesByStereotype(AnnotationUtil.SCOPE).iterator().next() == AnnotationUtil.SINGLETON

        cleanup:
        context.close()
    }

    void "if a factory field bean defines a @Bean scope and the original type did not have any scope, BeanDefinition::getScope and BeanDefinition::getAnnotationNamesByStereotype for javax.inject.Scope return empty. if factory field bean defines a qualifier, the original Qualifier is overridden"() {
        given:
        ApplicationContext context = buildContext('test.TestFactory$TestField', '''\
package test;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import io.micronaut.inject.annotation.*;
import io.micronaut.aop.*;
import io.micronaut.context.annotation.*;
import jakarta.inject.*;
import jakarta.inject.Singleton;

@Some
@Factory
class TestFactory$TestField {

    @Bean
    @Xyz
    Bar3 bar3 = new Bar3();
}

@Abc
class Bar3 {
}

@Retention(RUNTIME)
@Qualifier
@interface Abc {
}

@Retention(RUNTIME)
@Qualifier
@interface Xyz {
}

@Retention(RUNTIME)
@Qualifier
@interface Some {
}

''')
        when:
        BeanDefinition bar3BeanDefinition = context.getBeanDefinitions(context.classLoader.loadClass('test.Bar3'))
                .find { it.getDeclaringType().get().simpleName.contains("TestFactory") }

        then:
        !bar3BeanDefinition.getScope().isPresent()
        bar3BeanDefinition.declaredQualifier.toString() == "@Xyz"
        bar3BeanDefinition.getAnnotationNamesByStereotype(AnnotationUtil.SCOPE).size() == 0

        cleanup:
        context.close()
    }

    void "if factory field bean defines a @Bean scope and no qualifier, the original Scope and Qualifier are used"() {
        given:
        ApplicationContext context = buildContext('test.TestFactory$TestField', '''\
package test;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import io.micronaut.inject.annotation.*;
import io.micronaut.aop.*;
import io.micronaut.context.annotation.*;
import jakarta.inject.*;
import jakarta.inject.Singleton;

@Some
@Factory
class TestFactory$TestField {

    @Bean
    Bar4 bar4 = new Bar4();
}

@Abc
@Singleton
class Bar4 {
}

@Retention(RUNTIME)
@Qualifier
@interface Abc {
}

@Retention(RUNTIME)
@Qualifier
@interface Some {
}

''')
        when:
        BeanDefinition bar4BeanDefinition = context.getBeanDefinitions(context.classLoader.loadClass('test.Bar4'))
                .find { it.getDeclaringType().get().simpleName.contains("TestFactory") }

        then:
        !bar4BeanDefinition.getScope().isPresent()
        bar4BeanDefinition.singleton
        bar4BeanDefinition.declaredQualifier.toString() == "@Abc"
        bar4BeanDefinition.getAnnotationNamesByStereotype(AnnotationUtil.SCOPE).size() == 1
        bar4BeanDefinition.getAnnotationNamesByStereotype(AnnotationUtil.SCOPE).iterator().next() == AnnotationUtil.SINGLETON

        cleanup:
        context.close()
    }

    void "if factory field bean defines a @Bean scope, BeanDefinition::getScope and BeanDefinition::getAnnotationNamesByStereotype for javax.inject.Scope return empty, even if original bean defines @Singleton If factory field bean defines a qualifier, the original Qualifier is overridden"() {
        given:
        ApplicationContext context = buildContext('test.TestFactory$TestField', '''\
package test;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import io.micronaut.inject.annotation.*;
import io.micronaut.aop.*;
import io.micronaut.context.annotation.*;
import jakarta.inject.*;
import jakarta.inject.Singleton;

@Some
@Factory
class TestFactory$TestField {
    @Bean
    @Xyz
    Bar5 bar5 = new Bar5();
}

@Abc
@Singleton
class Bar5 {
}

@Retention(RUNTIME)
@Qualifier
@interface Abc {
}

@Retention(RUNTIME)
@Qualifier
@interface Xyz {
}

@Retention(RUNTIME)
@Qualifier
@interface Some {
}

''')
        when:
        BeanDefinition bar5BeanDefinition = context.getBeanDefinitions(context.classLoader.loadClass('test.Bar5'))
                .find {it.getDeclaringType().get().simpleName.contains("TestFactory")}

        then:
        !bar5BeanDefinition.getScope().isPresent()
        bar5BeanDefinition.declaredQualifier.toString() == "@Xyz"
        CollectionUtils.isEmpty(bar5BeanDefinition.getAnnotationNamesByStereotype(AnnotationUtil.SCOPE))

        cleanup:
        context.close()
    }

    @Issue("https://github.com/micronaut-projects/micronaut-core/pull/7165")
    void "if factory field bean defines a @Bean and @Prototype scopes, BeanDefinition::getScope returns Prototype and BeanDefinition::getAnnotationNamesByStereotype for javax.inject.Scope returns Prototype If factory field bean defines a qualifier, the original Qualifier is overridden"() {
        given:
        ApplicationContext context = buildContext('test.TestFactory$TestField', '''\
package test;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import io.micronaut.inject.annotation.*;
import io.micronaut.aop.*;
import io.micronaut.context.annotation.*;
import jakarta.inject.*;
import jakarta.inject.Singleton;

@Some
@Factory
class TestFactory$TestField {

    @Bean
    @Xyz
    @Prototype
    Bar6 bar6 = new Bar6();
}

@Abc
@Singleton
class Bar6 {
}

@Retention(RUNTIME)
@Qualifier
@interface Abc {
}

@Retention(RUNTIME)
@Qualifier
@interface Xyz {
}

@Retention(RUNTIME)
@Qualifier
@interface Some {
}

''')
        when:
        BeanDefinition bar6BeanDefinition = context.getBeanDefinitions(context.classLoader.loadClass('test.Bar6'))
                .find {it.getDeclaringType().get().simpleName.contains("TestFactory")}
        then:
        bar6BeanDefinition.getScope().get() == Prototype.class
        bar6BeanDefinition.declaredQualifier.toString() == "@Xyz"

        when:
        Collection<String> annotationNamesByScopeStereoType = bar6BeanDefinition.getAnnotationNamesByStereotype(AnnotationUtil.SCOPE).asCollection()

        then:
        annotationNamesByScopeStereoType.size() == 1
        annotationNamesByScopeStereoType.any {it == "io.micronaut.context.annotation.Prototype" }

        cleanup:
        context.close()
    }

    @Issue("https://github.com/micronaut-projects/micronaut-core/pull/7165")
    void "if factory has a qualifier it is inherited by the factory field beans"() {
        given:
        ApplicationContext context = buildContext('test.TestFactory$TestField', '''\
package test;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import io.micronaut.inject.annotation.*;
import io.micronaut.aop.*;
import io.micronaut.context.annotation.*;
import jakarta.inject.*;
import jakarta.inject.Singleton;

@Some
@Factory
class TestFactory$TestField {
    @Prototype
    Bar7 bar7 = new Bar7();
}

class Bar7 {
}

@Retention(RUNTIME)
@Qualifier
@interface Some {
}

''')
        when:
        BeanDefinition bar7BeanDefinition = context.getBeanDefinitions(context.classLoader.loadClass('test.Bar7'))
                .find {it.getDeclaringType().get().simpleName.contains("TestFactory")}
        then:
        bar7BeanDefinition.getScope().get() == Prototype.class
        bar7BeanDefinition.declaredQualifier == null

        when:
        Collection<String> annotationNamesByScopeStereoType = bar7BeanDefinition.getAnnotationNamesByStereotype(AnnotationUtil.SCOPE).asCollection()

        then:
        annotationNamesByScopeStereoType.size() == 1
        annotationNamesByScopeStereoType.any {it == "io.micronaut.context.annotation.Prototype" }

        cleanup:
        context.close()
    }

    void "newly introduced annotation should not break inherited qualifier/scope"() {
        given:
            ApplicationContext context = buildContext('test.TestFactory$TestField', '''\
package test;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import io.micronaut.inject.annotation.*;
import io.micronaut.aop.*;
import io.micronaut.context.annotation.*;
import jakarta.inject.*;
import jakarta.inject.Singleton;

@Some
@Factory
class TestFactory$TestField {

    @io.micronaut.inject.factory.RemappedAnnotation
    @Bean
    @Xyz
    @Prototype
    Bar7 bar7 = new Bar7();
    
    @Bean
    @Xyz
    @Prototype
    Bar8 bar8 = new Bar8();
}

@Abc
@Singleton
class Bar7 {
}

@Abc
@Singleton
@io.micronaut.inject.factory.RemappedAnnotation
class Bar8 {
}

@Retention(RUNTIME)
@Qualifier
@interface Abc {
}

@Retention(RUNTIME)
@Qualifier
@interface Xyz {
}

@Retention(RUNTIME)
@Qualifier
@interface Some {
}

''')
        when:
            def bar7BeanDefinition = context.getBeanDefinitions(context.classLoader.loadClass('test.Bar7'))
                    .find {it.getDeclaringType().get().simpleName.contains("TestFactory")}

            def bar8BeanDefinition = context.getBeanDefinitions(context.classLoader.loadClass('test.Bar8'))
                    .find {it.getDeclaringType().get().simpleName.contains("TestFactory")}

        then:
            bar7BeanDefinition.getScope().get() == Prototype.class
            bar7BeanDefinition.declaredQualifier.toString() == "@Xyz"
            bar7BeanDefinition.getAnnotationNamesByStereotype(AnnotationUtil.SCOPE).size() == 1
            bar7BeanDefinition.hasAnnotation(io.micronaut.inject.factory.RemappedAnnotation)
        and:
            bar8BeanDefinition.getScope().get() == Prototype.class
            bar8BeanDefinition.declaredQualifier.toString() == "@Xyz"
            bar8BeanDefinition.getAnnotationNamesByStereotype(AnnotationUtil.SCOPE).size() == 1
            bar8BeanDefinition.hasAnnotation(io.micronaut.inject.factory.RemappedAnnotation)

        cleanup:
        context.close()
    }
}
