import java.util.{Timer, TimerTask}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicLong, AtomicReference}

import CircuitState._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration

class Circuit[A](defaultAction: => A, maxAllowedFails: Int, openTime: Duration) (implicit ec: ExecutionContext) {

  private val circuitState: AtomicReference[CircuitState] = new AtomicReference[CircuitState](CircuitState.Closed)
  def getCircuitState: CircuitState = circuitState.get()

  private val isHalfStateFirstCall = new AtomicBoolean(true)
  private val consecutiveFailureCountInClosedState = new AtomicInteger(0)
  private val lastOpenTime = new AtomicLong(Long.MaxValue)

  def isOpenTimeoutOver: Boolean = System.currentTimeMillis() - lastOpenTime.get() > openTime.toMillis

  /* ToDo: Dependency on the timer can be removed if we check for the timeout
   *  for each execution with Circuit Breaker in the Open state.
   *  But in that case, Circuit State won't change automatically after the timeout
   *  until there is a request to execute with Circuit Breaker
   */
  new Timer("Open to Half Open after openTime").scheduleAtFixedRate(new TimerTask {
    override def run(): Unit = circuitState.get() match {
      case Open if isOpenTimeoutOver =>
        updateCircuitStateTo(HalfOpen)
      case _ => ()
    }
  }, 0L, 10L)

  def executeWithCircuitBreaker(program: => Future[A]): Future[A] = getCircuitState match {
      case Closed =>   handleClosedState(program)
      case HalfOpen => HalfOpenState.handleHalfOpenState(program)
      case Open =>     handleOpenState
    }

  def updateCircuitStateTo(state: CircuitState): Unit = state match {
    case Closed =>
      circuitState.set(Closed)
      lastOpenTime.set(Long.MaxValue)
      consecutiveFailureCountInClosedState.set(0)
    case HalfOpen =>
      isHalfStateFirstCall.set(true)
      circuitState.set(HalfOpen)
    case Open =>
      circuitState.set(Open)
      lastOpenTime.set(System.currentTimeMillis())
  }

  def handleOpenState: Future[A] = {
    Future.successful(defaultAction)
  }

  private object HalfOpenState {
    def handleHalfOpenState(program: => Future[A]): Future[A] = {
      if (isHalfStateFirstCall.get()) {
        isHalfStateFirstCall.set(false)
        program.flatMap { value =>
          updateCircuitStateTo(Closed)
          Future.successful(value)
        }(ec).recoverWith {
          case _ =>
            updateCircuitStateTo(Open)
            handleOpenState
        }
      } else {
        handleOpenState
      }
    }
  }

  private def handleClosedState(program: => Future[A]): Future[A] = {
     program.flatMap{ value =>
       consecutiveFailureCountInClosedState.set(0)
       Future.successful(value)
     } (ec).recoverWith {
         case _ => if (consecutiveFailureCountInClosedState.incrementAndGet() >= maxAllowedFails)
                      updateCircuitStateTo(Open)
                   Future.successful(defaultAction)
     }
  }

}
