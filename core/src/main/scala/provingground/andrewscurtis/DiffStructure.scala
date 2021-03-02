package provingground.andrewscurtis

import provingground.andrewscurtis.FreeGroups._
import provingground.learning.FiniteDistributionLearner._
import provingground.learning.AdjDiffbleFunction._

import provingground._
import LinearStructure._

import provingground.{FiniteDistribution => FD}

object DiffStructure {

  /*
   * Type aliases
   */
  //  type FD[X] = FiniteDistribution[X]
  type M = AtomicMove
  type V = Moves
  type P = Presentation

  def genMoveFn(mf: M) = mf.movesDF

  def genWtDyn(mf: M) = {
    val wtdyn = weightedDyn[M, FD[V]]
    wtdyn(mf, genMoveFn(mf))
  }

  def genCollection(lst: List[M]) = lst map genWtDyn

  def addCollection(lst: List[M]) = vBigSum(genCollection(lst))

  def genExtendM(lst: List[M]) = extendM(addCollection(lst))

  def iterateDiff(lst: List[M], iterations: Int = 5) =
    iterate(genExtendM(lst))(iterations)

  def genPresentationMoveFn(rank: Int) = MoveFn(Moves.actOnTriv(rank)(_: V))

  def genProjectionMap(rank: Int, iterations: Int = 5)(lst: List[M]) = {
    iterateDiff(lst, iterations) andthen projectV andthen genPresentationMoveFn(
      rank)
  }

  def getFeedback(presCntn: Double, wrdCntn: Double, scale: Double = 1)(
      dist: FD[P]) = {
    val baseweight = presentationWeight(_: P, presCntn, wrdCntn)
    dist.rawfeedback(baseweight) * scale
  }

  def conjugateByFeedback(presCntn: Double, wrdCntn: Double, scale: Double = 1)(
      rank: Int,
      iterations: Int = 5)(lst: List[M]) = {
    val projectionMap = genProjectionMap(rank, iterations)(lst)
    projectionMap.^:(getFeedback(presCntn, wrdCntn, scale)(_: FD[P]))
  }

  def genDynamics(conjFunc: ((FD[M], FD[V])) => (FD[M], FD[V]))(
      orig: (FD[M], FD[V])) = {
    val sumRes = conjFunc(orig)
    (orig._1 ++ sumRes._1, orig._2 ++ sumRes._2)
  }
}
