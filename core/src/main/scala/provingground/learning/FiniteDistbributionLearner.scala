package provingground.learning

import provingground._

import AdjDiffbleFunction._

import Collections._

import LinearStructure._



import FiniteDistribution._

case class SnapShot[X, P](state: X, name: String, loops: Int, param: P)

/**
  * A combinator for learning systems with state finite distributions on vertices.
  * Systems are built from components labeled by elements of a set M.
  * The state also has weights for these components.
  * The components are built from: moves (partial functions), partial combinations and islands.
  *
  */
object FiniteDistributionLearner {

  /**
    * An atom for a finite distribution
    */
  case class Atom[V](x: V)
      extends AdjDiffbleFunction[Double, FiniteDistribution[V]] {
    val func = (w: Double) => FiniteDistribution[V](Vector(Weighted(x, w)))

    val adjDer = (w: Double) => (p: FiniteDistribution[V]) => p(x)
  }

  /**
    * Evaluation at a point for a finite distribution
    */
  case class Evaluate[V](x: V)
      extends AdjDiffbleFunction[FiniteDistribution[V], Double] {
    val func = (p: FiniteDistribution[V]) => p(x)

    val adjDer = (p: FiniteDistribution[V]) =>
      (w: Double) => FiniteDistribution[V](Set(Weighted(x, w)))
  }

  case class PtwiseProd[V](sc: V => Double)
      extends AdjDiffbleFunction[FiniteDistribution[V], FiniteDistribution[V]] {
    val func = (p: FiniteDistribution[V]) => {
      val pmf = for (Weighted(x, w) <- p.pmf) yield Weighted(x, w * sc(x))
      FiniteDistribution(pmf)
    }

    val adjDer = (q: FiniteDistribution[V]) =>
      (p: FiniteDistribution[V]) => {
        val pmf = for (Weighted(x, w) <- p.pmf) yield Weighted(x, w * sc(x))
        FiniteDistribution(pmf)
    }
  }

  case class Sample[X](N: Double)
      extends FormalExtension[FiniteDistribution[X]] {
    val func = (d: FiniteDistribution[X]) =>
      d.flatten.filter ((x: X) => random.nextDouble() < d(x) * N).normalized ()
  }

  def sample[X](N: Double)
    : AdjDiffbleFunction[FiniteDistribution[X], FiniteDistribution[X]] =
    Sample(N)

  import AdjDiffbleFunction._

  /**
    * Normalizing a finite distribution.
    */
  case class NormalizeFD[V]()
      extends AdjDiffbleFunction[FiniteDistribution[V], FiniteDistribution[V]] {
    val func = (d: FiniteDistribution[V]) => d.normalized().flatten

    val adjDer = (d: FiniteDistribution[V]) =>
      (w: FiniteDistribution[V]) => w * (1 / d.norm)
  }

  /**
    * purging (randomly) a finite distribution.
    * @param size upper bound on the expected size of the support.
    */
  def purgeFD[V](size: Int)(fd: FiniteDistribution[V]) =
    fd filter ((x: V) => fd(x) * size > random.nextDouble())

  /**
    * smooth function applying move wherever applicable
    */
  case class MoveFn[V, W](f: V => Option[W])
      extends AdjDiffbleFunction[FiniteDistribution[V], FiniteDistribution[W]] {
    val func = (d: FiniteDistribution[V]) => {
      d mapOpt (f)
    }

    val adjDer = (d: FiniteDistribution[V]) =>
      (w: FiniteDistribution[W]) => {
        w.invmapOpt(f, d.supp)
    }
  }

  case class CombinationFn[V](f: (V, V) => Option[V],
                              firstFilter: V => Boolean = (a: V) => true)
      extends AdjDiffbleFunction[FiniteDistribution[V], FiniteDistribution[V]] {
    val func = (d: FiniteDistribution[V]) => {
      d filter (firstFilter) flatMap ((v: V) => d mapOpt ((w: V) => f(v, w)))
    }

    val adjDer = (d: FiniteDistribution[V]) =>
      (w: FiniteDistribution[V]) => {
        val fstsum = invFlatMap(
          (a: V) => (w.invmapOpt((b: V) => f(a, b), d.supp)) * d(a),
          d.filter(firstFilter).supp)

        val scndDists =
          d.filter(firstFilter).supp map
            ((a: V) => (w.invmapOpt((b: V) => f(a, b), d.supp)) * d(a))
        val scndsum = invFlatMap(
          (a: V) => (w.invmapOpt((b: V) => f(b, a), d.supp)) * d(a),
          d.supp)

        fstsum ++ scndsum
    }
  }

  /**
    * Add a new vertex, mainly for lambdas
    */
  case class NewVertex[V](v: V)
      extends AdjDiffbleFunction[(Double, FiniteDistribution[V]),
                                 FiniteDistribution[V]] {
    val func = (wp: (Double, FiniteDistribution[V])) =>
      wp._2 .* (1 - wp._1) .+ (v, wp._1)

    val adjDer = (wp: (Double, FiniteDistribution[V])) =>
      (q: FiniteDistribution[V]) => {
        val nov = q filter ((x: V) => x != v)
        (q(v), nov * wp._1)
    }
  }

  /**
    * Returns a smooth function (FD[M], X) => X, given a parameter index m : M and a dynamical system f: X => X;
    * the system f should correspond to m. For a distribution p in FD[M], if p(m) denotes the value at m,
    * the smooth function being defined is p(m)f.
    */
  def weightedDyn[M, X: LinearStructure: InnerProduct]
    : (M, AdjDiffbleFunction[X, X]) => AdjDiffbleFunction[
      (FiniteDistribution[M], X),
      X] =
    (m, fn) => {
      val pm  = Proj1[FiniteDistribution[M], X]()
      val scm = Evaluate(m)
      val atM = pm andthen scm andthen Incl1[Double, X]()
      val pv  = Proj2[FiniteDistribution[M], X]()
      val fv  = pv andthen fn andthen Incl2[Double, X]()
      val fnsum =
        vsum[AdjDiffbleFunction[(FiniteDistribution[M], X), (Double, X)]]
      fnsum(atM, fv) andthen ScProd[X]()
    }

  case class ExtendM[M, X](
      fn: AdjDiffbleFunction[(FiniteDistribution[M], X), X])
      extends AdjDiffbleFunction[(FiniteDistribution[M], X),
                                 (FiniteDistribution[M], X)] {
    val func = (mv: (FiniteDistribution[M], X)) => (mv._1, fn.func(mv))

    val adjDer = (mv: (FiniteDistribution[M], X)) =>
      (mw: (FiniteDistribution[M], X)) =>
        (mw._1 ++ fn.adjDer(mv)(mw._2)._1, fn.adjDer(mv)(mw._2)._2)
  }

  /**
    * Extend differentiable function by identity on M.
    */
  def extendM[M, X](fn: AdjDiffbleFunction[(FiniteDistribution[M], X), X])
    : AdjDiffbleFunction[(FiniteDistribution[M], X),
                         (FiniteDistribution[M], X)] = {
    ExtendM(fn)
  }

  case class ProjectV[M, X]()
      extends AdjDiffbleFunction[(FiniteDistribution[M], X), X] {
    val func = (mv: (FiniteDistribution[M], X)) => mv._2

    //      type X = FiniteDistribution[V]

    //     val zero = implicitly[LinearStructure[FiniteDistribution[M]]].zero

    val adjDer = (mv: (FiniteDistribution[M], X)) =>
      (v: X) => (FiniteDistribution.empty[M], v)
  }

  def projectV[M, X]: AdjDiffbleFunction[(FiniteDistribution[M], X), X] =
    ProjectV[M, X]()

  def sampleV[M, V](N: Double)
    : AdjDiffbleFunction[(FiniteDistribution[M], FiniteDistribution[V]),
                         (FiniteDistribution[M], FiniteDistribution[V])] =
    block(id[FiniteDistribution[M]], sample(N))
}
