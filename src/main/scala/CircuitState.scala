trait CircuitState

object CircuitState {
  case object Open extends CircuitState
  case object Closed extends CircuitState
  case object HalfOpen extends CircuitState
}
