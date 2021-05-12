# circuit-breaker

This is a sample [circuit breaker](https://martinfowler.com/bliki/CircuitBreaker.html) implementation with below requirements which is implemented in simple Scala code without using any library.



## Requirements

* The circuit breaker can execute asynchronous programs in the form of F[A] where F can be in the form of e.g. Future or can be left abstract
* It can execute many asynchronous programs at the same time, e.g. many requests coming in through an REST API
* When everything is normal, circuit breaker is in Closed state
* When normal execution fails or takes longer than a configured executionTimeout, failure counter should be incremented
* Successful execution resets this failure counter
* Once the failure counter reaches configured maxAllowedFails the circuit breaker transitions to the Open state
* In Open state, all calls should be rejected
* After an openTime the circuit breaker goes into the HalfOpen state
* In HalfOpen state only the first call should be allowed. All other calls fail the same as in Open state
* If the allowed call is successful the circuit breaker transitions back to the Closed state, if the call fails or times out the circuit breaker transitions back into the Open state One example interface could be:

  def executeWithCircuitBreaker[A](program: => Future[A]): Future[A] = ???