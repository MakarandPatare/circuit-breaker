import java.util.{Timer, TimerTask}
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong, AtomicReference}

import CircuitState._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration

class Circuit[A](defaultAction: => A, maxAllowedFails: Int, openTime: Duration) (implicit ec: ExecutionContext) {

  private val circuitState = new AtomicReference[CircuitState](CircuitState.Closed)
  def getCircuitState = circuitState.get()

  private val closedConsecutiveFailureCount = new AtomicInteger(0)
  private val lastOpenTime = new AtomicLong(Long.MaxValue)

  def isOpenTimeoutOver: Boolean = System.currentTimeMillis() - lastOpenTime.get() > openTime.toMillis

  new Timer(s"circuit-opener").scheduleAtFixedRate(new TimerTask {
    override def run(): Unit = circuitState.get() match {
      case Open if isOpenTimeoutOver =>
        updateCircuitStateTo(HalfOpen)
      case _ => ()
    }
  }, 0L, 10L)

  def executeWithCircuitBreaker(program: => Future[A]): Future[A] = circuitState.get() match {
      case Closed => handleClosedState(program)
      case HalfOpen => handleHalfOpenState(program)
      case Open => handleOpenState
    }

  def updateCircuitStateTo(state: CircuitState): Unit = state match {
    case Closed => circuitState.set(Closed);
      lastOpenTime.set(Long.MaxValue)
      closedConsecutiveFailureCount.set(0)
    case HalfOpen => circuitState.set(HalfOpen)
    case Open => circuitState.set(Open)
      lastOpenTime.set(System.currentTimeMillis())
  }

  def handleOpenState: Future[A] = {
    Future.successful(defaultAction)
  }

  def handleHalfOpenState(program: => Future[A]): Future[A] = {
    program.flatMap { value =>
        updateCircuitStateTo(Closed)
        Future.successful(value)
      }(ec).recoverWith {
        case _ => handleFailure(program, new AtomicInteger(0), 1)
      }
  }

  private def handleClosedState(program: => Future[A]): Future[A] = {
     program.flatMap{ value =>
       closedConsecutiveFailureCount.set(0)
       Future.successful(value) } (ec)
       .recoverWith {
         case _ => handleFailure(program, closedConsecutiveFailureCount, maxAllowedFails)
    }
  }

  private def handleFailure(program: => Future[A],
                                 atomicCounter: AtomicInteger,
                                 maxFailures: Int): Future[A] = {

    val currentFailureCount = atomicCounter.incrementAndGet()

    if (currentFailureCount > maxFailures) {
      updateCircuitStateTo(Open)
      executeWithCircuitBreaker(program) // Doubtful
    } else {
      closedConsecutiveFailureCount.addAndGet(1)
      executeWithCircuitBreaker(program)
    }
  }

}
