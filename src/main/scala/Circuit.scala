import java.util.concurrent.atomic.{AtomicInteger, AtomicLong, AtomicReference}

import scala.concurrent.Future
import scala.concurrent.duration.Duration

class Circuit[A](executionTimeout: Duration, maxAllowedFails: Int) {

  private val circuitState = new AtomicReference[CircuitState](CircuitState.Closed)
  private val closedConsecutiveFailureCount = new AtomicInteger(0)
  private val lastOpenTime = new AtomicLong(Long.MaxValue)

  def isTimeoutOver: Boolean = ???

  def getState: CircuitState = ???

  def executeWithCircuitBreaker(program: => Future[A]): Future[A] = ???

  def updateCircuitState: Unit = ???

  def handleOpenState(program: => Future[A]): Future[A] = ???

  def handleHalfOpenState(program: => Future[A]): Future[A] = ???

  def handleClosedState(program: => Future[A]): Future[A] = ???

}
