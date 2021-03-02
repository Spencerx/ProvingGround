package provingground.learning
import provingground._

import provingground.{FiniteDistribution => FD, ProbabilityDistribution => PD}

import learning.{TangVec => T}

import cats.implicits._

import monix.eval._
// import monix.cats._

import monix.reactive._


import TangVec.{liftLinear => lin, liftBilinear => bil}

// import scala.language.existentials

import HoTT._

// import spire.algebra._

object TermEvolver {
  implicit class TPOps[A](pd: T[PD[A]]) {

    /**
      * generates from the mixed in distribution with probability _weight_,
      * otherwise defaults to this distribution;
      * as the mixed in distribution is called by name, it may depend on the present one.
      */
    def <+>(mixin: => T[PD[A]], weight: Double) =
      T(pd.point.<+>(mixin.point, weight), pd.vec.<+>(mixin.vec, weight))

    /**
      * generates from the mixed in optional valued distribution with probability `weight`,
      * otherwise, or if the optional returns None, defaults to this distribution;
      * the mixed in distribution is call by name, so may depend on this distribution.
      */
    def <+?>(mixin: => T[PD[Option[A]]], weight: Double) =
      T(pd.point.<+?>(mixin.point, weight), pd.vec.<+?>(mixin.vec, weight))

    def map[B](f: A => B): T[PD[B]] = lin((p: PD[A]) => p.map(f))(pd)

    def flatMap[B](f: A => T[PD[B]]): T[PD[B]] =
      TangVec(
        pd.point.flatMap((a) => f(a).point),
        pd.vec.flatMap((a) => f(a).vec)
      )

    def condMap[B](f: A => Option[B]): T[PD[B]] =
      lin((p: PD[A]) => p.condMap(f))(pd)

    def conditioned(pred: A => Boolean): T[PD[A]] =
      lin((p: PD[A]) => p.conditioned(pred))(pd)
  }

  def total[A](x: Vector[(A, Int)]): Int = (x map (_._2)).sum

  def toFD[A](sample: Map[A, Int]): FD[A] = {
    val tot = total(sample.toVector)
    FiniteDistribution(sample.toVector map {
      case (x, n) => Weighted(x, n.toDouble / tot)
    })
  }

  def justTerm[U <: Term with Subs[U]](x: U): Term = x: Term

  def theorems(fd: FD[Term]): FD[Typ[Term]] = {
    fd.filter(_.typ.typ == Type).map(_.typ: Typ[Term]).filter(fd(_) > 0)
  }

  def allTheorems(fd: FD[Term], n: Int = 25): Vector[Weighted[Typ[Term]]] =
    theorems(fd).entropyVec

  def topTheorems(fd: FD[Term], n: Int = 25): Vector[Weighted[Typ[Term]]] =
    theorems(fd).entropyVec.take(n)
}

trait TermEvolution {
  val evolve: T[FD[Term]] => T[PD[Term]]

  val evolveTyps: T[FD[Term]] => T[PD[Typ[Term]]]

  def baseEvolve(fd: FD[Term]): PD[Term] = evolve(T(fd, FD.empty[Term])).point

  def baseEvolveTyps(fd: FD[Term]): PD[Typ[Term]] =
    evolveTyps(T(fd, FD.empty[Term])).point

  def tangEvolve(base: FD[Term])(vec: FD[Term]): PD[Term] =
    evolve(T(base, vec)).vec

  def tangEvolveTyps(base: FD[Term])(vec: FD[Term]): PD[Typ[Term]] =
    evolveTyps(T(base, vec)).vec
}

class TermEvolver(
    unApp: Double = 0.1,
    appl: Double = 0.1,
    lambdaWeight: Double = 0.1,
    piWeight: Double = 0.1,
    varWeight: Double = 0.3
) extends TermEvolution {
  import TermEvolver._

  val evolve: T[FD[Term]] => T[PD[Term]] =
    (init: T[FD[Term]]) =>
      (init: T[PD[Term]])
        .<+?>(Tappln(evolveFuncs(init) && evolveWithTyp(init)), appl)
        .<+?>(TunifAppln(evolveFuncs(init) && evolve(init)), unApp)
        .<+>(lambdaMix(init), lambdaWeight)
        .<+>(piMix(init).map(justTerm[Typ[Term]]), piWeight)

  val evolveFuncs: T[FD[Term]] => T[PD[ExstFunc]] =
    (init: T[FD[Term]]) => {
      init
        .<+?>(TunifAppln(evolveFuncs(init) && evolve(init)), unApp)
        .<+?>(Tappln(evolveFuncs(init) && evolveWithTyp(init)), appl)
    }.condMap(ExstFunc.opt)

  val evolveTypFamilies: T[FD[Term]] => T[PD[ExstFunc]] =
    (init: T[FD[Term]]) => {
      init
        .<+?>(TunifAppln(evolveTypFamilies(init) && evolve(init)), unApp)
        .<+?>(Tappln(evolveTypFamilies(init) && evolveWithTyp(init)), appl)
    }.conditioned(isTypFamily).condMap(ExstFunc.opt)

  val evolveWithTyp: T[FD[Term]] => T[Typ[Term] => PD[Term]] =
    (tfd: T[FD[Term]]) =>
      TangVec(
        (tp: Typ[Term]) => evolveAtTyp(tp)(tfd).point,
        (tp: Typ[Term]) => evolveAtTyp(tp)(tfd).vec
      )

  val evolveTyps: T[FD[Term]] => T[PD[Typ[Term]]] =
    (init: T[FD[Term]]) => {
      evolve(init)
        .<+?>(TunifAppln(evolveTypFamilies(init) && evolve(init)), unApp)
        .<+?>(Tappln(evolveTypFamilies(init) && evolveWithTyp(init)), appl)
    }.condMap(typOpt).<+>(piMix(init), piWeight)

  def evolveAtTyp(typ: Typ[Term]): T[FD[Term]] => T[PD[Term]] =
    (tfd: T[FD[Term]]) => {
      evolve(tfd)
    }.conditioned(_.typ == typ)

  def unifAppln(x: PD[ExstFunc], y: PD[Term]): PD[Option[Term]] =
    (x product y).map {
      case (fn, arg) => Unify.appln(fn.func, arg)
    }

  val TunifAppln: T[(PD[ExstFunc], PD[Term])] => T[PD[Option[Term]]] = bil(
    unifAppln
  )

  def simpleAppln(
      funcs: PD[ExstFunc],
      args: Typ[Term] => PD[Term]
  ): PD[Option[Term]] =
    (funcs.fibProduct(_.func.dom, args)).map {
      case (fn, arg) => fn(arg)
    }

  val Tappln: T[(PD[ExstFunc], Typ[Term] => PD[Term])] => T[PD[Option[Term]]] =
    bil(simpleAppln)

  def lambdaMixVar(
      x: Term,
      wt: Double,
      base: => (T[FD[Term]] => T[PD[Term]])
  ): T[FD[Term]] => T[PD[Term]] =
    (tfd: T[FD[Term]]) => {
      val dist =
        lin((fd: FD[Term]) => fd .* (1 - wt) .+ (x, wt))(tfd)
      base(dist).map((y) => x :~> y: Term)
    }

  def piMixVar(
      x: Term,
      wt: Double,
      base: => (T[FD[Term]] => T[PD[Typ[Term]]])
  ): T[FD[Term]] => T[PD[Typ[Term]]] =
    (tfd: T[FD[Term]]) => {
      val dist =
        lin((fd: FD[Term]) => fd .* (1 - wt) .+ (x, wt))(tfd)
      base(dist).map((y) => x ~>: y: Typ[Term])
    }

  def lambdaMixTyp(
      typ: Typ[Term],
      wt: Double,
      base: => (T[FD[Term]] => T[PD[Term]])
  ): T[FD[Term]] => T[PD[Term]] = {
    val x = typ.Var
    lambdaMixVar(x, wt, base)
  }

  def piMixTyp(
      typ: Typ[Term],
      wt: Double,
      base: => (T[FD[Term]] => T[PD[Typ[Term]]])
  ): T[FD[Term]] => T[PD[Typ[Term]]] = {
    val x = typ.Var
    piMixVar(x, wt, base)
  }

  def lambdaMix(fd: T[FD[Term]]): T[PD[Term]] =
    evolveTyps(fd).flatMap((tp) => lambdaMixTyp(tp, varWeight, evolve)(fd))

  def lambdaForTyp(typ: Typ[Term])(fd: T[FD[Term]]): T[PD[Term]] = typ match {
    case FuncTyp(dom: Typ[u], codom: Typ[v]) =>
      lambdaMixTyp(dom, lambdaWeight, evolveAtTyp(codom))(fd)
    case PiDefn(variable: Term, value: Typ[u]) =>
      val x: Term = variable.typ.Var
      val cod     = value.replace(variable, x)
      lambdaMixTyp(variable.typ, lambdaWeight, evolveAtTyp(cod))(fd)
    case _ =>
      TangVec(FD.empty[Term], FD.empty[Term])
  }

  def piMix(fd: T[FD[Term]]): T[PD[Typ[Term]]] =
    evolveTyps(fd).flatMap((tp) => piMixTyp(tp, varWeight, evolveTyps)(fd))
}

object TermEvolutionStep {
  case class Param(
      vars: Vector[Term] = Vector(),
      size: Int = 1000,
      derTotalSize: Int = 1000,
      epsilon: Double = 0.2,
      inertia: Double = 0.3,
      scale: Double = 1.0,
      thmScale: Double = 0.3,
      thmTarget: Double = 0.2
  )

  // implicit val taskMonad = implicitly[Monad[Task]]

  def obserEv(p: FD[Term], param: Param = Param())(
      implicit ms: MonixSamples
  ): Observable[TermEvolutionStep[Task]] =
    Observable
      .fromAsyncStateAction[TermEvolutionStep[Task], TermEvolutionStep[Task]](
        (st: TermEvolutionStep[Task]) => st.succ.map((x) => (x, x))
      )(new TermEvolutionStep(p, new TermEvolver(), param)(ms))

  def observable(p: FD[Term], param: Param = Param())(
      implicit ms: MonixSamples
  ): Observable[FD[Term]] =
    Observable.fromAsyncStateAction[TermEvolutionStep[Task], FD[Term]](
      (st: TermEvolutionStep[Task]) => st.succ.map((x) => (x.p, x))
    )(new TermEvolutionStep(p, new TermEvolver(), param)(ms))

}

class TermEvolutionStep[X[_]](
    val p: FD[Term],
    ev: TermEvolution = new TermEvolver(),
    val param: TermEvolutionStep.Param = TermEvolutionStep.Param()
)(implicit val samp: TangSamples[X]) {
  import samp._, param._
  lazy val init: PD[Term] = ev.baseEvolve(p)

  lazy val nextFD: X[FD[Term]] =
    for (samp <- sampFD(init, size))
      yield samp * (1.0 - inertia) ++ (p * inertia)

  lazy val nextTypFD: X[FD[Typ[Term]]] = sampFD(ev.baseEvolveTyps(p), size)

  lazy val thmFeedback: X[TheoremFeedback] =
    for {
      nFD  <- nextFD
      ntFD <- nextTypFD
    } yield TheoremFeedback(nFD, ntFD, vars, scale, thmScale, thmTarget)

  def derivativePD(tang: FD[Term]): PD[Term] = ev.tangEvolve(p)(tang)

  def derivativeFD(tang: FD[Term], n: Int): X[FD[Term]] =
    sampFD(derivativePD(tang), n)

  def derivativeTypsFD(tang: FD[Term], n: Int): X[FD[Typ[Term]]] =
    sampFD(ev.tangEvolveTyps(p)(tang), n)

  lazy val tangSamples: X[Vector[(FD[Term], Int)]] =
    for (nfd <- nextFD; ts <- tangSizes(derTotalSize)(nfd)) yield ts

  def derFDX(
      vec: Vector[(FD[Term], Int)]
  ): X[Vector[(FD[Term], (FD[Term], FD[Typ[Term]]))]] =
    sequence {
      for {
        (fd, n) <- vec
      } yield
        for {
          dfd  <- derivativeFD(fd, n)
          dtfd <- derivativeTypsFD(fd, n)
        } yield (fd, (dfd, dtfd))
    }

  lazy val derivativeFDs: X[Vector[(FD[Term], (FD[Term], FD[Typ[Term]]))]] =
    tangSamples.flatMap(derFDX)

  lazy val feedBacks: X[Vector[(FD[Term], Double)]] =
    for {
      derFDs <- derivativeFDs
      thmFb  <- thmFeedback
    } yield
      for { (x, (tfd, tpfd)) <- derFDs } yield
        (x, thmFb.feedbackTermDist(tfd, tpfd))

  lazy val succFD: X[FD[Term]] =
    for {
      fbs <- feedBacks
      nfd <- nextFD
    } yield fbs.foldRight(nfd) { case ((t, w), fd) => fd ++ (t * w) }

  def newp(np: FD[Term]) = new TermEvolutionStep(np, ev, param)

  lazy val succ: X[TermEvolutionStep[X]] = succFD.map(newp)

  def next: X[TermEvolutionStep[X]] = succ

}
