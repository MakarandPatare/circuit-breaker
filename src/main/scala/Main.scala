import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

object Main extends App {

  implicit val sampleCircuit: Circuit[Int] = new Circuit[Int](-1, 5, 5.seconds)
  // to test concurrent executions
  val resultsF: Future[Seq[Int]] =
    Future.sequence((1 to 500).map(_ => sampleCircuit.executeWithCircuitBreaker(Future(2 + 2))))
  val results: Seq[Int] =
    Await.result(resultsF, 10.minutes).toList
  println(results)
}
