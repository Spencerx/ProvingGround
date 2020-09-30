package provingground.learning
import provingground._

import HoTT._
import FiniteDistributionLearner._
import AdjDiffbleFunction._
import scala.util._
import FiniteDistribution._
import LinearStructure._
import scala.language.existentials

object HoTTgen {

  def isFunc(t: Term) = t.isInstanceOf[FuncLike[_, _]]

  def isTyp(t: Term) = t.isInstanceOf[Typ[_]]

  def inDomain: Term => Term => Boolean = {
    case (func: FuncLike[u, v]) => { (arg: Term) =>
      arg.typ == func.dom
    }
    case _ =>
      (_) =>
        false
  }

  val funcappl: (Term, Term) => Option[Term] = {
    case (f: FuncLike[u, _], a: Term) =>
      Try(f(a.asInstanceOf[u])).toOption
    case _ => None
  }

  val functyp: (Term, Term) => Option[Term] = {
    case (u: Typ[Term], v: Typ[Term]) if (u.typ == Type) => Some(FuncTyp(u, v))
    case _                                               => None
  }

  val pityp: Term => Option[Term] = {
    case fmly: Func[u, _] =>
      fmly.codom.typ match {
        case _: Typ[w] =>
          Try {
            val x     = fmly.dom.Var
            val y     = fmly(x).asInstanceOf[Typ[w with Subs[w]]]
            val fibre = lmbda(x)(y)
            PiDefn[u, w](fibre)
          }.toOption
        case _ => None
      }
    case _ => None
  }

  val sigmatyp: Term => Option[Term] = {
    case fmly: Func[u, _] =>
      fmly.codom.typ match {
        case _: Typ[w] =>
          Try {
            val x     = fmly.dom.Var
            val y     = fmly(x).asInstanceOf[Typ[w with Subs[w]]]
            val fibre = lmbda(x)(y)
            SigmaTyp[u, w](fibre)
          }.toOption
        case _ => None
      }
    case _ => None
  }

  val pairtyp: (Term, Term) => Option[Term] = {
    case (a: Typ[u], b: Typ[v]) if (a.typ == Type) && (b.typ == Type) =>
      Some(ProdTyp(a, b))
    case _ => None
  }

  val PairTerm: (Term, Term) => Option[Term] = {
    case (_: Universe, _) => None
    case (_, _: Universe) => None
    case (a, b)           => Some(pair(a, b))
  }

  val paircons: Term => Option[Term] = {
    case p: ProdTyp[_, _]  => Some(p.paircons)
    case p: SigmaTyp[_, _] => Some(p.paircons)
    case _                 => None
  }

  val icons: Term => Option[Term] = {
    case p: PlusTyp[u, v] => Some(p.incl1)
    case _                => None
  }

  val jcons: Term => Option[Term] = {
    case p: PlusTyp[u, v] => Some(p.incl2)
    case _                => None
  }

  /*
	object Move extends Enumeration{
	  lazy val lambda, appl, arrow, pi, sigma, pairt, pair, paircons, icons, jcons, id  = Value
	}
   */

  lazy val moves: List[
    (Move,
     AdjDiffbleFunction[FiniteDistribution[Term], FiniteDistribution[Term]])] =
    List(
      (Move.appl, CombinationFn(funcappl, isFunc)),
      (Move.arrow, CombinationFn(functyp, isTyp)),
      (Move.pi, MoveFn(pityp)),
      (Move.sigma, MoveFn(sigmatyp)),
      (Move.pairt, CombinationFn(pairtyp, isTyp)),
      (Move.pair, CombinationFn(PairTerm)),
      (Move.paircons, MoveFn(paircons)),
      (Move.icons, MoveFn(icons)),
      (Move.jcons, MoveFn(icons)),
      (Move.id, Id[FiniteDistribution[Term]]())
    )

  object Move {
    case object lambda extends Move

    case object appl extends Move

    case object arrow extends Move

    case object pi extends Move

    case object sigma extends Move

    case object pair extends Move

    case object pairt extends Move

    case object paircons extends Move

    case object icons extends Move

    case object jcons extends Move

    case object id extends Move
  }

  sealed trait Move

  val wtdDyn = weightedDyn[Move, FiniteDistribution[Term]]

  val wtdMoveList = for (mv <- moves) yield extendM(wtdDyn(mv._1, mv._2))

  val wtdMoveSum =
    vBigSum(wtdMoveList) andthen block(NormalizeFD[Move](), NormalizeFD[Term]())

  def lambdaFn[M](
      l: M,
      f: AdjDiffbleFunction[(FiniteDistribution[M], FiniteDistribution[Term]),
                            (FiniteDistribution[M], FiniteDistribution[Term])])(
      typ: Typ[Term]) = {
    import AdjDiffbleFunction._
    val x    = typ.Var
    val incl = (Evaluate(l) oplus id[FiniteDistribution[Term]])
    val init = NewVertex(x)
    val export = MoveFn(
      (t: Term) => if (t != Type) Some(lambda(x)(t): Term) else None)
    val head = incl andthen init
    extendM(head) andthen f andthen block(Id[FiniteDistribution[M]](), export)
  }

  def lambdaSum[M](l: M)(
      f: AdjDiffbleFunction[
        (FiniteDistribution[M], FiniteDistribution[Term]),
        (FiniteDistribution[M], FiniteDistribution[Term])]) = {
    val lambdas = (fd: (FiniteDistribution[M], FiniteDistribution[Term])) => {
      val terms = fd._2.supp
      val gettyps: PartialFunction[Term, Typ[Term]] = {
        case typ: Typ[_] => typ
      }
      val typs = terms collect gettyps
      typs map ((typ) => lambdaFn(l, f)(typ))
    }
    AdjDiffbleFunction.BigSum(lambdas)
  }
  /*
	def lambdaSumM[M](l : M)(
	    g: AdjDiffbleFunction[(FiniteDistribution[M], FiniteDistribution[Term]), (FiniteDistribution[M], FiniteDistribution[Term])]
	    ) = {
	  val p = AdjDiffbleFunction.Proj2[FiniteDistribution[M], FiniteDistribution[Term]]
	  val f = g andthen p
	  val withIsle = lambdaSum(l)(f)
	  extendM(withIsle)
	}
   */
  val hottDyn = AdjDiffbleFunction
    .mixinIsle[(FiniteDistribution[Move], FiniteDistribution[Term])](
      wtdMoveSum,
      lambdaSum(Move.lambda),
      block(NormalizeFD[Move](), NormalizeFD[Term]()))

  val mapTyp = MoveFn[Term, Typ[Term]]((t: Term) =>
    if (t.typ.typ == Type) Some(t.typ) else None)

  private def ifTyp: Term => Option[Typ[Term]] = {
    case typ: Typ[Term] if typ.typ == Type => Some(typ)
    case _                                 => None
  }

  def getTyps(d: FiniteDistribution[Term]) = d.mapOpt(ifTyp)

  val typFlow = (d: FiniteDistribution[Term]) => {
    val shift = mapTyp.func(d) rawfeedback (getTyps(d).getsum(_))
    mapTyp.adjDer(d)(shift)
  }

  def dynTypFlow(
      dyn: AdjDiffbleFunction[FiniteDistribution[Term],
                              FiniteDistribution[Term]]) = {
    typFlow ^: dyn
  }
}
