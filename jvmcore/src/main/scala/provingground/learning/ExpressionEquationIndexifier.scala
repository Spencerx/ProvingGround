package provingground.learning

import provingground.{FiniteDistribution => _, _}, HoTT._

import spire.math._
import spire.implicits._

import GeneratorVariables._, TermRandomVars._, Expression._

import scala.collection.parallel.CollectionConverters._
import scala.collection.parallel.immutable._

/**
  * Maps equations based on expressions in terms, randomvariables etc. to equations based on indices only,
  * for rapid computation. Also provides some helpers to relate index combinations and expressions.
  *
  * @param initMap
  * @param equationSet
  * @param params
  * @param initVariables
  */
class ExpressionEquationIndexifier(
    initMap: Map[Expression, Double],
    val equationVec: Vector[Equation],
    params: Coeff[_] => Option[Double],
    initVariables: Vector[Expression] = Vector()
) {
  import ExpressionEquationIndexifier._
  // lazy val equationVec: Vector[Equation] = equationSet.toVector //.par

  lazy val size = equationVec.size

  val numVars = size + initVariables.size

  lazy val varVec = equationVec.map(_.lhs)

  lazy val indexMap: Map[Expression, Int] = equationVec
    .map(_.lhs)
    .zipWithIndex
    .toMap ++ (
    initVariables.zipWithIndex.map {
      case (exp, n) => exp -> (n + size)
    }
  )

  def mapToIndexMap[V](m: Map[Expression, V]): Map[Int, V] =
    m.map { case (exp, v) => indexMap(exp) -> v }

  // Set total probability as 1 for each group
  lazy val randomVarIndices: ParVector[Vector[Int]] = indexedExprGroups(
    equationVec.map(_.lhs).zipWithIndex
  )

  // already orthonormal
  lazy val totalProbEquations: ParVector[ParVector[Double]] =
    randomVarIndices.map { gp =>
      val scaled = 1.0 / sqrt(gp.size.toDouble)
      val s      = gp.toSet
      ParVector.tabulate(size)(n => if (s.contains(n)) scaled else 0.0)
    }

  lazy val initPar = initMap.par

  def getProd(exp: Expression): ProductIndexExpression =
    initPar
      .get(exp)
      .map(c => ProductIndexExpression(c, Vector(), Vector()))
      .orElse(
        indexMap
          .get(exp)
          .map(j => ProductIndexExpression(1, Vector(j), Vector()))
      )
      .getOrElse(
        exp match {
          case cf @ Coeff(_) =>
            ProductIndexExpression(
              params(cf)
                .getOrElse(0),
              Vector(),
              Vector()
            )
          case Product(x, y)  => getProd(x) * getProd(y)
          case Quotient(x, y) => getProd(x) / getProd(y)
          case Literal(value) =>
            ProductIndexExpression(value, Vector(), Vector())
          case InitialVal(variable) =>
            ProductIndexExpression(0, Vector(), Vector())
          case _ =>
            JvmUtils.logger.warn(
              s"cannot decompose $exp as a product, though it is in the rhs of ${equationVec
                .find(eqq => (Expression.atoms(eqq.rhs).contains(exp)))}"
            )
            ProductIndexExpression(0, Vector(), Vector())
        }
      )

  def simplify(exp: Expression): SumIndexExpression =
    SumIndexExpression(
      Expression.sumTerms(exp).map(getProd(_)).filterNot(_.constant == 0)
    )

  lazy val rhsExprs: Vector[SumIndexExpression] =
    equationVec.map(eq => simplify(eq.rhs))

  lazy val rhsExprsPar = rhsExprs.par

  def rhsInvolves(js: Set[Int]): Set[Int] =
    (0 until (size))
      .filter(i => js.exists(j => rhsExprs(i).indices.contains(j)))
      .toSet

  lazy val constantEquations: Set[Int] =
    (0 until (size)).filter(i => rhsExprs(i).isConstant).toSet

  def proofData(typ: Typ[Term]): Vector[(Int, Equation)] =
    equationVec.zipWithIndex.collect {
      case (eq @ Equation(FinalVal(Elem(t: Term, Terms)), rhs), j)
          if t.typ == typ =>
        (j, eq)
    }

  def traceIndices(j: Int, depth: Int): Vector[Int] =
    if (depth < 1) Vector(j)
    else j +: rhsExprs(j).indices.flatMap(traceIndices(_, depth - 1))

  def nextTraceVector(current: Vector[Vector[Int]]): Vector[Vector[Int]] =
    current.flatMap { branch =>
      (0 until (branch.length)).flatMap { j =>
        val before    = branch.take(j)
        val after     = branch.drop(j + 1)
        val offspring = rhsExprs(j).terms
        offspring.map(pt => before ++ pt.indices ++ after)
      }
    }

  def nextTraceSet(
      current: Set[Set[Int]],
      relativeTo: Set[Int]
  ): Set[Set[Int]] =
    current.flatMap { branchSet =>
      val branch = branchSet.toVector
      (0 until (branch.length)).flatMap { j =>
        val rest      = branch.take(j).toSet union branch.drop(j + 1).toSet
        val offspring = rhsExprs(j).terms
        offspring.map(pt => (rest union pt.indices.toSet) -- relativeTo)
      }
    }

  @annotation.tailrec
  final def recTraceSet(
      current: Set[Set[Int]],
      depth: Int,
      relativeTo: Set[Int],
      accum: Set[Set[Int]]
  ): Set[Set[Int]] =
    if (depth < 1 || current.isEmpty) accum
    else {
      val next = nextTraceSet(current, relativeTo)
      recTraceSet(next, depth - 1, relativeTo, accum union (next))
    }

  def traceSet(
      elem: Expression,
      depth: Int,
      relativeTo: Set[Int]
  ): Set[Set[Int]] =
    indexMap
      .get(elem)
      .map(index => recTraceSet(Set(Set(index)), depth, relativeTo, Set()))
      .getOrElse(Set())

  // not orthonormal
  def equationGradients(
      v: collection.parallel.ParSeq[Double]
  ): ParVector[ParVector[Double]] = {
    ParVector.tabulate(size) { n =>
      val rhsGrad = rhsExprs(n).gradient(v)
      parVector(
        vecSum(Vector(rhsGrad, Vector(n -> -1.0))),
        size
      )
    }
  }

  def orthonormalGradients(
      v: collection.parallel.ParSeq[Double],
      cutoff: Double = 0.0
  ): ParVector[ParVector[Double]] =
    ParGramSchmidt.orthonormalize(
      equationGradients(v),
      totalProbEquations,
      cutoff
    )

  lazy val termIndices: Vector[Int] = {
    val pfn: PartialFunction[(Equation, Int), Int] = {
      case (Equation(FinalVal(Elem(_, Terms)), _), j) => j
    }
    equationVec.zipWithIndex.collect(pfn)
  }

  lazy val termIndexVec: Vector[(Term, Int)] = {
    val pfn: PartialFunction[(Equation, Int), (Term, Int)] = {
      case (Equation(FinalVal(Elem(x: Term, Terms)), _), j) => x -> j
    }
    equationVec.zipWithIndex.collect(pfn)
  }

  lazy val initTermIndices: Vector[Int] = {
    val pfn: PartialFunction[(Expression, Int), Int] = {
      case (InitialVal(Elem(_, Terms)), j) => j + size
    }
    initVariables.zipWithIndex.collect(pfn)
  }

  lazy val typIndices: Vector[Int] = {
    val pfn: PartialFunction[(Equation, Int), Int] = {
      case (Equation(FinalVal(Elem(_, Typs)), _), j) => j
    }
    equationVec.zipWithIndex.collect(pfn)
  }

  lazy val typIndexVec: Vector[(Typ[Term], Int)] = {
    val pfn: PartialFunction[(Equation, Int), (Typ[Term], Int)] = {
      case (Equation(FinalVal(Elem(x: Typ[u], Typs)), _), j) => x -> j
    }
    equationVec.zipWithIndex.collect(pfn)
  }

  lazy val initTypIndices: Vector[Int] = {
    val pfn: PartialFunction[(Expression, Int), Int] = {
      case (InitialVal(Elem(_, Typs)), j) => j + size
    }
    initVariables.zipWithIndex.collect(pfn)
  }

  lazy val thmPfIndices: Map[Int,Vector[Int]] =
    equationVec.zipWithIndex
      .collect {
        case (Equation(FinalVal(Elem(x: Term, Terms)), _), j) => j -> x.typ
      }
      .flatMap {
        case (j, t) => indexMap.get(FinalVal(Elem(t, Typs))).map(k => j -> k)
      }
      .groupMap(_._2)(_._1)

}

object ExpressionEquationIndexifier {
  def vecSum(vecs: Vector[Vector[(Int, Double)]]): Vector[(Int, Double)] =
    vecs.reduce(_ ++ _).groupMapReduce(_._1)(_._2)(_ + _).toVector

  def varGroups(
      vars: Vector[GeneratorVariables.Variable[_]]
  ): Vector[Vector[GeneratorVariables.Variable[_]]] = {
    val elems      = vars collect { case el: Elem[_] => el }
    val elemGroups = elems.groupBy(_.randomVar).map(_._2).to(Vector)
    val isleVars = vars
      .collect { case isl: InIsle[_, _, _, _, _] => isl }
      .groupBy(isl => (isl.isle, isl.boat))
      .map(_._2)
      .toVector
    val isleGroups = isleVars.flatMap(gp => varGroups(gp))
    elemGroups ++ isleGroups
  }

  def indexedVarGroups(
      vars: Vector[(Int, GeneratorVariables.Variable[_])]
  ): ParVector[Vector[Int]] = {
    val elems = vars collect { case (n, el: Elem[_]) => n -> el }
    val elemGroups =
      elems.groupMap(_._2.randomVar)(_._1).map(_._2).to(ParVector)
    val isleVars: Vector[Vector[(Int, GeneratorVariables.Variable[_])]] = vars
      .collect { case (n, isl: InIsle[_, _, _, _, _]) => n -> isl }
      .groupMap { case (_, isl) => (isl.isle, isl.boat) } {
        case (n, isl) => (n, isl.isleVar)
      }
      .map(_._2)
      .toVector
    val isleGroups = isleVars.flatMap(gp => indexedVarGroups(gp))
    elemGroups ++ isleGroups
  }

  def exprGroups(exps: Vector[Expression]): Vector[Vector[Expression]] = {
    val finGps =
      varGroups(exps.collect { case FinalVal(variable) => variable }).map(
        vv => vv.map(exp => FinalVal(exp))
      )
    val initGps = varGroups(exps.collect {
      case InitialVal(variable) => variable
    }).map(
      vv => vv.map(exp => InitialVal(exp))
    )
    finGps ++ initGps
  }

  def indexedExprGroups(
      exps: Vector[(Expression, Int)]
  ): ParVector[Vector[Int]] = {
    val finGps = indexedVarGroups(exps.collect {
      case (FinalVal(variable), n) => n -> variable
    })
    val initGps = indexedVarGroups(exps.collect {
      case (InitialVal(variable), n) => n -> variable
    })
    finGps ++ initGps
  }

  def parVector(v: Vector[(Int, Double)], size: Int): ParVector[Double] = {
    val m = v.toMap
    ParVector.tabulate(size)(n => m.getOrElse(n, 0.0))
  }
}

case class ProductIndexExpression(
    constant: Double,
    indices: Vector[Int],
    negIndices: Vector[Int]
) {
  val isPositiveConstant = indices.isEmpty && constant > 0

  val isConstant = indices.isEmpty

  val initialValue = if (isConstant) constant else 0.0

  def eval(v: ParVector[Double]): Double = {
    val subTerms = (indices.map(j => v(j)) ++ negIndices.map { j =>
      val y = v(j)
      if (y == 0) 1.0
      else {
        val rec = 1.0 / y
        if ((rec.isNaN() || rec.isInfinite()) && !y.isNaN)
          JvmUtils.logger.error(
            s"the reciprocal of $y is not a number or is infinite"
          )
        rec
      }
    })
    val result = subTerms.product * constant
    if ((result.isNaN() || result
          .isInfinite()) && !subTerms.exists(_.isNaN()) && !constant.isNaN())
      JvmUtils.logger.error(
        s"the product of $subTerms  and constant $constant is not a number or is infinite in $this"
      )
    if (result.isNaN()) 0 else result
  }

  def gradient(v: collection.parallel.ParSeq[Double]): Vector[(Int, Double)] = {
    val numeratorTerms = indices.map(v(_))
    val denominatorTerms = negIndices.map { j =>
      val y = v(j)
      if (y > 0) 1.0 / y else 1.0
    }
    val numerator   = numeratorTerms.product
    val denominator = denominatorTerms.product
    val posLiebnitz = Vector.tabulate(numeratorTerms.size) { j =>
      j -> (numeratorTerms.take(j) ++ numeratorTerms.drop(j + 1)).product * constant * denominator
    }
    val negLiebnitz = Vector.tabulate(denominatorTerms.size) { j =>
      j -> -(denominatorTerms.take(j) ++ denominatorTerms.drop(j + 1)).product / (denominatorTerms(
        j
      ) * denominatorTerms(j)) * constant * numerator
    }
    ExpressionEquationIndexifier.vecSum(Vector(posLiebnitz, negLiebnitz))
  }

  def evaluate(m: Map[Int, Double]): Double = {
    val subTerms = (indices.map(j => m.getOrElse(j, 0.0)) ++ negIndices.map {
      j =>
        val y = m.getOrElse(j, 0.0)
        if (y == 0) 1.0
        else {
          val rec = 1.0 / y
          if ((rec.isNaN() || rec.isInfinite()) && !y.isNaN)
            JvmUtils.logger.error(
              s"the reciprocal of $y is not a number or is infinite"
            )
          rec
        }
    })
    val result = subTerms.product * constant
    if (result.isNaN() && !subTerms.exists(_.isNaN()) && !constant.isNaN())
      JvmUtils.logger.error(
        s"the product of $subTerms  and constant $constant is not a number"
      )
    result
  }

  val numSupport = indices
    .map { j =>
      s"X($j)"
    }
    .mkString(" * ")
  val denSupport =
    if (negIndices.isEmpty) ""
    else
      negIndices
        .map { j =>
          s"X($j)"
        }
        .mkString("/(", " * ", ")")

  override def toString() =
    s"($constant * $numSupport $denSupport)"

  def *(that: ProductIndexExpression) =
    ProductIndexExpression(
      constant * that.constant,
      indices ++ that.indices,
      negIndices ++ that.negIndices
    )

  def /(that: ProductIndexExpression) =
    ProductIndexExpression(
      if (that.constant > 0) constant / that.constant else constant,
      indices ++ that.negIndices,
      negIndices ++ that.indices
    )
}

case class SumIndexExpression(terms: Vector[ProductIndexExpression]) {
  val constantTerm: Double = terms.filter(_.isConstant).map(_.constant).sum
  val indices: Vector[Int] = terms.flatMap(_.indices).distinct
  val hasConstant: Boolean = terms.exists(_.isPositiveConstant)
  val isPositiveConstant: Boolean = terms.forall(_.isConstant) && terms.exists(
    _.isPositiveConstant
  )
  val isConstant: Boolean = terms.forall(_.isConstant)

  val initialValue =
    if (isPositiveConstant)(terms.map(_.initialValue)).sum else 0.0

  def eval(v: ParVector[Double]): Double = {
    val subTerms = terms.map(_.eval(v))
    val result   = subTerms.sum
    // if (result < 0)
    //   JvmUtils.logger.error(
    //     s"Negative value for expression with terms $terms, values $subTerms"
    //   )
    if (result.isNaN() && !subTerms.exists(_.isNaN()))
      JvmUtils.logger.error(s"the sum of $subTerms is not a number")
    result
  }

  def gradient(v: collection.parallel.ParSeq[Double]): Vector[(Int, Double)] =
    ExpressionEquationIndexifier.vecSum(terms.map(_.gradient(v)))

  def evaluate(m: Map[Int, Double]): Double = {
    val subTerms = terms.map(_.evaluate(m))
    val result   = subTerms.sum
    if (result.isNaN() && !subTerms.exists(_.isNaN()))
      JvmUtils.logger.error(s"the sum of $subTerms is not a number")
    result
  }

  override def toString() = terms.mkString(" + ")
}
