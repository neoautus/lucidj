<?xml version="1.0"?>
<project>

<!-- **********************************************************************
      DEFAULT PROPERTIES
     ********************************************************************** -->

    <!-- Project directories -->
    <property name="tools.dir" value="${basedir}/tools"/>
    <property name="misc.dir" value="${basedir}/misc"/>
    <property name="src.dir" value="${basedir}/src"/>
    <property name="interfaces.src.dir" value="${basedir}/interfaces"/>
    <property name="libraries.src.dir" value="${basedir}/libraries"/>
    <property name="components.src.dir" value="${basedir}/components"/>
    <property name="product.dir" value="${basedir}/product"/>
    <property name="stage.dir" value="${basedir}/stage"/>
    <property name="runtime.dir" value="${stage.dir}/runtime"/>
    <property name="bootstrap.dir" value="${stage.dir}/runtime/bootstrap"/>
    <property name="application.dir" value="${stage.dir}/runtime/application-dev"/>
    <property name="cache.dir" value="${basedir}/cache"/>
    <property name="libcache.dir" value="${cache.dir}/lib"/>
    <property name="maven2.preload.dir" value="${cache.dir}/maven2_preload"/>
    <property name="dist.dir" value="${basedir}/dist"/>
    <property name="build.dir" value="${basedir}/build"/>

    <property name="vaadin.static" value="vaadin_static_files"/>
    <property name="vaadin.static.jar" value="${dist.dir}/${vaadin.static}.jar"/>

    <property name="widgetset.dir" value="${build.dir}/widgetset"/>
    <property name="widgets.src" value="${basedir}/widgets"/>
    <property name="widgetset.root.dir" value="${widgetset.dir}/root"/><!-- REFACTOR -->
    <property name="temp.dir" value="${build.dir}/temp"/>

    <property name="interfaces.class" value="${build.dir}/interfaces.class"/>
    <property name="libraries.class" value="${build.dir}/libraries.class"/>

    <!-- Apache Karaf -->
    <property name="karaf.release" value="4.0.5"/>
    <property name="karaf.dist.zip" value="apache-karaf-${karaf.release}.zip"/>
    <property name="karaf.dist.url"
              value="http://www.apache.org/dist/karaf/${karaf.release}/${karaf.dist.zip}"/>

    <!-- Compilation flags -->
    <property name="javac.debug" value="true"/>
    <property name="javac.debuglevel" value="lines,vars,source"/>
    <property name="javac.deprecation" value="on"/>
    <property name="javac.optional" value="-Xlint:unchecked"/>
    <property name="javac.source" value="1.7"/>
    <property name="javac.target" value="1.7"/>
    <property name="javac.bootclasspath" value="${basedir}/extras/rt-1.7/rt.jar:${basedir}/extras/rt-1.7/jce.jar"/>

    <property name="configure.ok.file" value="${cache.dir}/configure.ok"/>

</project>
