package com.kixeye.core.scala.template

import org.springframework.context.annotation.{ComponentScan, Configuration}
import com.kixeye.chassis.scala.transport.ScalaTransportConfiguration
import com.kixeye.chassis.support.ChassisConfiguration
import javax.annotation.{PreDestroy, PostConstruct}
import com.kixeye.chassis.bootstrap.annotation.App

@App(
  name = "ScalaTemplateService",
  propertiesResourceLocation = "classpath:/template-defaults.properties",
  configurationClasses = Array(classOf[ChassisConfiguration], classOf[ScalaTransportConfiguration], classOf[TemplateConfiguration]),
  webapp = true)
@Configuration
@ComponentScan(basePackageClasses = Array(classOf[TemplateConfiguration]))
class TemplateConfiguration {

  @PostConstruct
  def init() {
    // Add some initialization logic here!!
  }

  @PreDestroy
  def destroy() {
    // Add some destruction code here!
  }

}