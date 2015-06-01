package io.getclump

import scala.collection.generic.CanBuildFrom

protected[getclump] trait Sources extends Tuples {

  /**
   * Create a clump source from a function that accepts inputs and returns a future list of values.
   * Since the order of the list of values is undefined, also must provide a function that takes the value and returns
   * the key used to get that value.
   */
  def source[V, K, C[_] <: Iterable[_]](fetch: C[K] => Future[Iterable[V]])(keyExtractor: V => K)
                      (implicit cbf: CanBuildFrom[Nothing, K, C[K]]): ClumpSource[K, V] =
    new ClumpSource(extractKeys(adaptInput(fetch), keyExtractor))


  def source[K, V](fetch: K => Future[V]): ClumpSource[K, V] = {
    def singletonFetch(keys: Set[K]) = fetch(keys.head).map(value => Map(keys.head -> value))
    val clumpSource: ClumpSource[K, V] = source(singletonFetch _)
    clumpSource.maxBatchSize(1)
  }
  /**
   * Similar to [[source]] but also accepts 1 extra params
   */
  def source[A, KS, V, K](fetch: (A, KS) => Future[Iterable[V]])(keyExtractor: V => K)
                         (implicit cbf: CanBuildFrom[Nothing, K, KS]): ClumpSource[(A, K), V] =
    new ClumpSource(parameterizeFetch(normalize1, denormalize1[A, K], fetch1(fetch), keyExtractor))

  /**
   * Similar to [[source]] but also accepts 2 extra params
   */
  def source[A, B, KS, V, K](fetch: (A, B, KS) => Future[Iterable[V]])(keyExtractor: V => K)
                            (implicit cbf: CanBuildFrom[Nothing, K, KS]): ClumpSource[(A, B, K), V] =
    new ClumpSource(parameterizeFetch(normalize2, denormalize2[A, B, K], fetch2(fetch), keyExtractor))

  /**
   * Similar to [[source]] but also accepts 3 extra params
   */
  def source[A, B, C, KS, V, K](fetch: (A, B, C, KS) => Future[Iterable[V]])(keyExtractor: V => K)
                               (implicit cbf: CanBuildFrom[Nothing, K, KS]): ClumpSource[(A, B, C, K), V] =
    new ClumpSource(parameterizeFetch(normalize3, denormalize3[A, B, C, K], fetch3(fetch), keyExtractor))

  /**
   * Similar to [[source]] but also accepts 4 extra params
   */
  def source[A, B, C, D, KS, V, K](fetch: (A, B, C, D, KS) => Future[Iterable[V]])(keyExtractor: V => K)
                                  (implicit cbf: CanBuildFrom[Nothing, K, KS]): ClumpSource[(A, B, C, D, K), V] =
    new ClumpSource(parameterizeFetch(normalize4, denormalize4[A, B, C, D, K], fetch4(fetch), keyExtractor))

  /**
   * Create a clump source from a function that accepts inputs and returns a future map from input to resulting value.
   */
  def source[K, V, C[_] <: Iterable[_]](fetch: C[K] => Future[Map[K, V]])
                      (implicit cbf: CanBuildFrom[Nothing, K, C[K]]): ClumpSource[K, V] =
    new ClumpSource(adaptOutput(adaptInput(fetch)))

  /**
   * Similar to [[source]] but also accepts 1 extra params
   */
  def source[A, KS, K, V](fetch: (A, KS) => Future[Map[K, V]])
                         (implicit cbf: CanBuildFrom[Nothing, K, KS]): ClumpSource[(A, K), V] =
    new ClumpSource(parameterizeFetch(normalize1, denormalize1[A, K], fetch1(fetch)))

  /**
   * Similar to [[source]] but also accepts 2 extra params
   */
  def source[A, B, KS, K, V](fetch: (A, B, KS) => Future[Map[K, V]])
                            (implicit cbf: CanBuildFrom[Nothing, K, KS]): ClumpSource[(A, B, K), V] =
    new ClumpSource(parameterizeFetch(normalize2, denormalize2[A, B, K], fetch2(fetch)))

  /**
   * Similar to [[source]] but also accepts 3 extra params
   */
  def source[A, B, C, KS, K, V](fetch: (A, B, C, KS) => Future[Map[K, V]])
                               (implicit cbf: CanBuildFrom[Nothing, K, KS]): ClumpSource[(A, B, C, K), V] =
    new ClumpSource(parameterizeFetch(normalize3, denormalize3[A, B, C, K], fetch3(fetch)))

  /**
   * Similar to [[source]] but also accepts 4 extra params
   */
  def source[A, B, C, D, KS, K, V](fetch: (A, B, C, D, KS) => Future[Map[K, V]])
                                  (implicit cbf: CanBuildFrom[Nothing, K, KS]): ClumpSource[(A, B, C, D, K), V] =
    new ClumpSource(parameterizeFetch(normalize4, denormalize4[A, B, C, D, K], fetch4(fetch)))

  /**
   * Create a clump source from a function that accepts inputs and returns a future list of values.
   * Unlike in [[source]], the order of the returned values must be the same as the list of keys that was passed in as
   * the key and value lists will be zipped together to create a map from key to value.
   */
  def sourceZip[K, V](fetch: List[K] => Future[List[V]]): ClumpSource[K, V] =
    new ClumpSource(zipped(fetch))

  /**
   * Similar to [[sourceZip]] but also accepts 1 extra params
   */
  def sourceZip[A, K, V](fetch: (A, List[K]) => Future[List[V]]): ClumpSource[(A, K), V] =
    new ClumpSource(parameterizeFetchZip(normalize1, fetch1(fetch)))

  /**
   * Similar to [[sourceZip]] but also accepts 2 extra params
   */
  def sourceZip[A, B, K, V](fetch: (A, B, List[K]) => Future[List[V]]): ClumpSource[(A, B, K), V] =
    new ClumpSource(parameterizeFetchZip(normalize2, fetch2(fetch)))

  /**
   * Similar to [[sourceZip]] but also accepts 3 extra params
   */
  def sourceZip[A, B, C, K, V](fetch: (A, B, C, List[K]) => Future[List[V]]): ClumpSource[(A, B, C, K), V] =
    new ClumpSource(parameterizeFetchZip(normalize3, fetch3(fetch)))

  /**
   * Similar to [[sourceZip]] but also accepts 4 extra params
   */
  def sourceZip[A, B, C, D, K, V](fetch: (A, B, C, D, List[K]) => Future[List[V]]): ClumpSource[(A, B, C, D, K), V] =
    new ClumpSource(parameterizeFetchZip(normalize4, fetch4(fetch)))

  private[this] def parameterizeFetch[I, P, O, T, C](normalize: I => (P, T), denormalize: (P, T) => I, fetch: (P, C) => Future[Iterable[O]], extractKey: O => T)
                                                    (implicit cbf: CanBuildFrom[Nothing, T, C]): List[I] => Future[Map[I, O]] =
    parameterizeFetch[I, P, O, T, C](normalize, denormalize, fetch = (params: P, coll: C) => fetch(params, coll).map(_.map(v => extractKey(v) -> v).toMap))

  private[this] def parameterizeFetch[I, P, O, T, C](normalize: I => (P, T), denormalize: (P, T) => I, fetch: (P, C) => Future[Iterable[(T, O)]])
                                                    (implicit cbf: CanBuildFrom[Nothing, T, C]): List[I] => Future[Map[I, O]] =
    (inputs: List[I]) => {
      val futures =
        inputs.map(normalize).groupBy { case (params, _) => params }.map {
          case (params, paramsAndKeys) =>
            fetch(params, cbf.apply().++=(paramsAndKeys.map { case (_, keys) => keys }).result()).map {
              _.map { case (key, value) => denormalize(params, key) -> value }
            }
        }.toSeq
      Future.sequence(futures).map(_.reduce(_ ++ _).toMap)
    }

  private[this] def parameterizeFetchZip[I, P, O, T](normalize: I => (P, T), fetch: (P, List[T]) => Future[Iterable[O]]): List[I] => Future[Map[I, O]] =
    (inputs: List[I]) => {
      val futures =
        inputs.map(normalize).groupBy { case (params, _) => params }.map {
          case (params, paramsAndKeys) =>
            fetch(params, paramsAndKeys.map { case (_, keys) => keys })
        }.toSeq
      val listOutputs = Future.sequence(futures).map(_.reduce(_ ++ _)).map(_.toList)
      listOutputs.map(inputs.zip(_).toMap)
    }

  private[this] def zipped[T, U](fetch: List[T] => Future[List[U]]) = {
    (inputs: List[T]) => fetch(inputs).map(inputs.zip(_).toMap)
  }

  private[this] def extractKeys[T, U](fetch: List[T] => Future[Iterable[U]], keyExtractor: U => T) =
    fetch.andThen(_.map(resultsToKeys(keyExtractor, _)))

  private[this] def resultsToKeys[U, T](keyExtractor: (U) => T, results: Iterable[U]) =
    results.map(v => (keyExtractor(v), v)).toMap

  private[this] def adaptInput[T, C, R](fetch: C => Future[R])(implicit cbf: CanBuildFrom[Nothing, T, C]) =
    (c: List[T]) => fetch(cbf.apply().++=(c).result())

  private[this] def adaptOutput[T, U, C](fetch: C => Future[Iterable[(T, U)]]) =
    fetch.andThen(_.map(_.toMap))

}
