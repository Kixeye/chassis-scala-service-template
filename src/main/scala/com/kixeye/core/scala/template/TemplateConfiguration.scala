package com.kixeye.core.scala.template

import com.kixeye.chassis.bootstrap.annotation.SpringApp
import com.kixeye.chassis.transport.TransportConfiguration
import org.springframework.context.annotation.{ComponentScan, Configuration}
import com.kixeye.chassis.scala.transport.ScalaTransportConfiguration
import com.kixeye.chassis.scala.transport.ScalaTransportConfiguration
import com.kixeye.chassis.transport.TransportConfiguration
import com.kixeye.chassis.bootstrap.annotation.SpringApp
import com.kixeye.chassis.support.ChassisConfiguration

@SpringApp(
  name = "ScalaTemplateService",
  propertiesResourceLocation = "classpath:/template-defaults.properties",
  configurationClasses = Array(classOf[ChassisConfiguration],classOf[ScalaTransportConfiguration],classOf[TemplateConfiguration]),
  webapp = true)
@Configuration
@ComponentScan(basePackageClasses = Array(classOf[TemplateConfiguration]))
class TemplateConfiguration {
}