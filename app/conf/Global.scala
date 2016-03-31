//package conf
//
//import play.api.Application
//import scheduled.ScheduledJob
//import uk.gov.hmrc.play.audit.filters.AuditFilter
//import uk.gov.hmrc.play.audit.http.connector.AuditConnector
//import uk.gov.hmrc.play.http.logging.filters.LoggingFilter
//import uk.gov.hmrc.play.microservice.bootstrap.DefaultMicroserviceGlobal
//import uk.gov.hmrc.play.scheduling.RunningOfScheduledJobs
//
//
//object Global extends DefaultMicroserviceGlobal with RunningOfScheduledJobs  {
//
//  override def microserviceMetricsConfig(implicit app: Application) = app.configuration.getConfig(s"microservice.metrics")
//
//
//
//  override def authFilter = None
//
//  override lazy val scheduledJobs = Seq(ScheduledJob)
//
//  override def auditConnector: AuditConnector = ???
//
//  override def loggingFilter: LoggingFilter = ???
//
//  override def microserviceAuditFilter: AuditFilter = ???
//}
