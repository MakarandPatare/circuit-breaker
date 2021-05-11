import org.scalatest.{FunSpec, Matchers}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class CircuitTest extends FunSpec with Matchers {

  describe("Circuit Breaker Closed") {
    it("Successful Invocation") {

      val sampleCircuit: Circuit[Int] = new Circuit[Int](-1, 5, 5.seconds)

      val resultsF: Future[Seq[Int]] = Future.sequence((1 to 500).map(_ => {
          sampleCircuit.executeWithCircuitBreaker(Future {2 + 2})}))

      val results: Seq[Int] = Await.result(resultsF, 10.minutes).toList
      results should be((1 to 500).map(_ => 4))
      sampleCircuit.getCircuitState should be (CircuitState.Closed)
    }

    it("Failures more than threshold") {
      val sampleCircuit: Circuit[Int] = new Circuit[Int](-1, 5, 5.seconds)

      Future.sequence(for {
        _ <- 1 to 5
      } yield sampleCircuit.executeWithCircuitBreaker(Future(1 / 0)))

      Thread.sleep(1000)
      sampleCircuit.getCircuitState should be (CircuitState.Open)
      Thread.sleep(5000)
      sampleCircuit.getCircuitState should be (CircuitState.HalfOpen)
      sampleCircuit.executeWithCircuitBreaker(Future(2 + 2))
      sampleCircuit.getCircuitState should be (CircuitState.Closed)
    }
  }
}