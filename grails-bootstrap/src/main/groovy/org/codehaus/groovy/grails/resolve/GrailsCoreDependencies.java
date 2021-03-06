/* Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.resolve;

import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.codehaus.groovy.grails.resolve.config.*;
import groovy.lang.Closure;

/**
 * Encapsulates information about the core dependencies of Grails.
 * 
 * This may eventually expand to expose information such as Spring version etc.
 * and be made available in the binding for user dependency declarations.
 */
public class GrailsCoreDependencies {
    
    public final String grailsVersion;
    
    public GrailsCoreDependencies(String grailsVersion) {
        this.grailsVersion = grailsVersion;
    }

    private void registerDependencies(IvyDependencyManager dependencyManager, String scope, ModuleRevisionId[] dependencies, String... excludes) {
        for (ModuleRevisionId mrid : dependencies) {
            EnhancedDefaultDependencyDescriptor descriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, false, scope);
            descriptor.setInherited(true);
            if (excludes != null) {
                for (String exclude : excludes) {
                    descriptor.exclude(exclude);
                }
            }
            dependencyManager.registerDependency(scope, descriptor);
        }
    }
    
    /**
     * Returns a closure suitable for passing to a DependencyDefinitionParser that will configure
     * the necessary core dependencies for Grails.
     */
    public Closure createDeclaration() {
        return new Closure(this, this) {
            public Object doCall() {
                DependencyConfigurationConfigurer rootDelegate = (DependencyConfigurationConfigurer)getDelegate();
                
                rootDelegate.log("warn");
                
                // Repositories
                
                rootDelegate.repositories(new Closure(this, GrailsCoreDependencies.this) {
                    public Object doCall() {
                        RepositoriesConfigurer repositoriesDelegate = (RepositoriesConfigurer)getDelegate();
                     
                        repositoriesDelegate.grailsPlugins();
                        repositoriesDelegate.grailsHome();
                        
                        return null;
                    }
                });
                
                // Dependencies
                
                rootDelegate.dependencies(new Closure(this, GrailsCoreDependencies.this) {
                    public Object doCall() {
                        JarDependenciesConfigurer dependenciesDelegate = (JarDependenciesConfigurer)getDelegate(); 
                        IvyDependencyManager dependencyManager = dependenciesDelegate.getDependencyManager();
                        
                        boolean defaultDependenciesProvided = dependencyManager.getDefaultDependenciesProvided();
                        String compileTimeDependenciesMethod = defaultDependenciesProvided ? "provided" : "compile";
                        String runtimeDependenciesMethod = defaultDependenciesProvided ? "provided" : "runtime";

                        // dependencies needed by the Grails build system
                        ModuleRevisionId[] buildDependencies = {
                            ModuleRevisionId.newInstance("org.tmatesoft.svnkit", "svnkit", "1.3.4"),
                            ModuleRevisionId.newInstance("org.apache.ant", "ant", "1.8.1"),
                            ModuleRevisionId.newInstance("org.apache.ant", "ant-launcher", "1.8.1"),
                            ModuleRevisionId.newInstance("org.apache.ant", "ant-junit", "1.8.1"),
                            ModuleRevisionId.newInstance("org.apache.ant", "ant-nodeps", "1.8.1"),
                            ModuleRevisionId.newInstance("org.apache.ant", "ant-trax", "1.7.1"),
                            ModuleRevisionId.newInstance("jline", "jline", "0.9.94"),
                            ModuleRevisionId.newInstance("org.fusesource.jansi", "jansi", "1.2.1"),
                            ModuleRevisionId.newInstance("xalan","serializer", "2.7.1"),
                            ModuleRevisionId.newInstance("org.grails", "grails-docs", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-bootstrap", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-scripts", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-core", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-resources", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-web", grailsVersion),
                            ModuleRevisionId.newInstance("org.slf4j", "slf4j-api", "1.6.1"),
                            ModuleRevisionId.newInstance("org.slf4j", "slf4j-log4j12", "1.6.1"),
                            ModuleRevisionId.newInstance("org.springframework", "spring-test", "3.0.5.RELEASE"),
                            ModuleRevisionId.newInstance("com.googlecode.concurrentlinkedhashmap", "concurrentlinkedhashmap-lru", "1.1_jdk5")
                        };
                        registerDependencies(dependencyManager, "build", buildDependencies);
                        
                        
                        // depenencies needed when creating docs
                        ModuleRevisionId[] docDependencies = {
                            ModuleRevisionId.newInstance("org.xhtmlrenderer", "core-renderer","R8"),
                            ModuleRevisionId.newInstance("com.lowagie","itext", "2.0.8"),
                            ModuleRevisionId.newInstance("org.grails", "grails-radeox", "1.0-b4")
                        };
                        registerDependencies(dependencyManager, "docs", docDependencies);
                        
                        
                        // dependencies needed during development, but not for deployment
                        ModuleRevisionId[] providedDependencies = {
                            ModuleRevisionId.newInstance("javax.servlet", "servlet-api", "2.5"),
                            ModuleRevisionId.newInstance("javax.servlet.jsp", "jsp-api","2.1")
                        };
                        registerDependencies(dependencyManager, "provided", providedDependencies);
                        
                        
                        // dependencies needed at compile time
                        ModuleRevisionId[] groovyDependencies = {
                            ModuleRevisionId.newInstance("org.codehaus.groovy", "groovy-all", "1.8.0-rc-1")
                        };                        
                        registerDependencies(dependencyManager, compileTimeDependenciesMethod, groovyDependencies, "jline");
                        
                        ModuleRevisionId[] commonsExcludingLoggingAndXmlApis = {
                            ModuleRevisionId.newInstance("commons-beanutils", "commons-beanutils", "1.8.0"),
                            ModuleRevisionId.newInstance("commons-el", "commons-el", "1.0"),
                            ModuleRevisionId.newInstance("commons-validator", "commons-validator", "1.3.1")
                        };
                        registerDependencies(dependencyManager, compileTimeDependenciesMethod, commonsExcludingLoggingAndXmlApis, "commons-logging", "xml-apis");

                        ModuleRevisionId[] compileDependencies = {
                            ModuleRevisionId.newInstance("org.coconut.forkjoin", "jsr166y", "070108"),
                            ModuleRevisionId.newInstance("org.codehaus.gpars", "gpars", "0.9"),
                            ModuleRevisionId.newInstance("aopalliance", "aopalliance", "1.0"),
                            ModuleRevisionId.newInstance("com.googlecode.concurrentlinkedhashmap", "concurrentlinkedhashmap-lru", "1.1_jdk5"),
                            ModuleRevisionId.newInstance("commons-codec", "commons-codec", "1.4"),
                            ModuleRevisionId.newInstance("commons-collections", "commons-collections", "3.2.1"),
                            ModuleRevisionId.newInstance("commons-io", "commons-io", "1.4"),
                            ModuleRevisionId.newInstance("commons-lang", "commons-lang", "2.4"),
                            ModuleRevisionId.newInstance("javax.transaction", "jta", "1.1"),
                            ModuleRevisionId.newInstance("javax.persistence", "persistence-api", "1.0"),
                            ModuleRevisionId.newInstance("opensymphony", "sitemesh", "2.4"),
                            ModuleRevisionId.newInstance("org.grails", "grails-bootstrap", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-core", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-crud", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-hibernate", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-resources", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-spring", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-web", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-datastore-gorm", "1.0.0.BUILD-SNAPSHOT"),
                            
                            // Plugins
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-codecs", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-controllers", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-domain-class", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-converters", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-datasource", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-filters", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-gsp", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-i18n", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-logging", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-scaffolding", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-services", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-servlets", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-url-mappings", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-validation", grailsVersion),
                            ModuleRevisionId.newInstance("org.springframework", "spring-core", "3.0.5.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "spring-aop", "3.0.5.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "spring-aspects", "3.0.5.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "spring-asm", "3.0.5.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "spring-beans", "3.0.5.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "spring-context", "3.0.5.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "spring-expression", "3.0.5.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "spring-instrument", "3.0.5.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "spring-jdbc", "3.0.5.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "spring-jms", "3.0.5.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "spring-orm", "3.0.5.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "spring-oxm", "3.0.5.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "spring-tx", "3.0.5.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "spring-web", "3.0.5.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "spring-webmvc", "3.0.5.RELEASE"),
                            ModuleRevisionId.newInstance("org.springframework", "spring-datastore-core", "1.0.0.BUILD-SNAPSHOT"),
                            ModuleRevisionId.newInstance("org.slf4j", "slf4j-api", "1.6.1")
                        };
                        registerDependencies(dependencyManager, compileTimeDependenciesMethod, compileDependencies);
                        
                        
                        // dependencies needed for running tests
                        ModuleRevisionId[] testDependencies = {
                            ModuleRevisionId.newInstance("junit", "junit", "4.8.1"),
                            ModuleRevisionId.newInstance("org.grails", "grails-plugin-testing", grailsVersion),
                            ModuleRevisionId.newInstance("org.grails", "grails-test", grailsVersion),
                            ModuleRevisionId.newInstance("org.springframework", "spring-test", "3.0.5.RELEASE")
                        };
                        registerDependencies(dependencyManager, "test", testDependencies);
                        
                        
                        // dependencies needed at runtime only
                        ModuleRevisionId[] runtimeDependencies = {
                            ModuleRevisionId.newInstance("org.aspectj", "aspectjweaver", "1.6.10"),
                            ModuleRevisionId.newInstance("org.aspectj", "aspectjrt", "1.6.10"),
                            ModuleRevisionId.newInstance("cglib", "cglib-nodep", "2.1_3"),
                            ModuleRevisionId.newInstance("commons-fileupload", "commons-fileupload", "1.2.1"),
                            ModuleRevisionId.newInstance("oro", "oro", "2.0.8"),
                            ModuleRevisionId.newInstance("javax.servlet", "jstl", "1.1.2"),
                            // data source
                            ModuleRevisionId.newInstance("commons-dbcp", "commons-dbcp", "1.3"),
                            ModuleRevisionId.newInstance("commons-pool", "commons-pool", "1.5.5"),
                            ModuleRevisionId.newInstance("hsqldb", "hsqldb", "1.8.0.10"),
                            ModuleRevisionId.newInstance("com.h2database", "h2", "1.2.147"),
                            // JSP support
                            ModuleRevisionId.newInstance("taglibs", "standard", "1.1.2"),
                            ModuleRevisionId.newInstance("xpp3", "xpp3_min", "1.1.4c")
                        };
                        registerDependencies(dependencyManager, runtimeDependenciesMethod, runtimeDependencies);
                        
                        ModuleRevisionId[] ehcacheDependencies = {
                            ModuleRevisionId.newInstance("net.sf.ehcache", "ehcache-core", "2.3.1")
                        };
                        registerDependencies(dependencyManager, runtimeDependenciesMethod, ehcacheDependencies, "jms", "commons-logging", "servlet-api");

                        ModuleRevisionId[] loggingDependencies = {
                            ModuleRevisionId.newInstance("log4j", "log4j", "1.2.16"),
                            ModuleRevisionId.newInstance("org.slf4j", "jcl-over-slf4j", "1.6.1"),
                            ModuleRevisionId.newInstance("org.slf4j", "jul-to-slf4j", "1.6.1"),
                            ModuleRevisionId.newInstance("org.slf4j", "slf4j-log4j12", "1.6.1")
                        };
                        registerDependencies(dependencyManager, runtimeDependenciesMethod, loggingDependencies, "mail", "jms", "jmxtools", "jmxri");

                        return null;
                        
                    } 
                }); // end depenencies closure
                
                return null;
            }
            
        }; // end root closure 

    }
    
}