package provingground.andrewscurtis

import provingground._, learning._

import MoveGenerator._
import DiffStructure._

import FiniteDistributionLearner._

import upickle.default._

import upickle.default.{ReadWriter => RW, macroRW}

//import com.github.nscala_time.time.Imports._

/**
  * @author gadgil
  */
object SimpleAcEvolution {
  def toPresentation(rank: Int, fdV: FiniteDistribution[Moves]) =
    fdV map ((v: V) => Moves.actOnTriv(rank)(v).get)

  case class State(rank: Int,
                   fdM: FiniteDistribution[AtomicMove],
                   fdV: FiniteDistribution[Moves]) {
    def fdP = toPresentation(rank, fdV)

    def pair = (fdM, fdV)

    def pickle = {
      val pmfM = (for (Weighted(m, p) <- fdM.pmf)
        yield PickledWeighted(write(m), p)).toVector
      val pmfV = (for (Weighted(v, p) <- fdV.pmf)
        yield (PickledWeighted(write(v), p))).toVector
      PickledState(rank, pmfM, pmfV)
    }
  }

  object PickledState {
    implicit def rw: RW[PickledState] = macroRW
  }

  case class PickledState(rank: Int,
                          pmfM: Vector[PickledWeighted],
                          pmfV: Vector[PickledWeighted]) {
    def unpickle = {
      val fdM = {
        val pmf = for (PickledWeighted(x, p) <- pmfM) yield Weighted(x, p)
        FiniteDistribution(pmf) map ((s: String) => read[AtomicMove](s))
      }

      val fdV = {
        val pmf = for (PickledWeighted(x, p) <- pmfV)
          yield Weighted(read[Moves](x), p)
        FiniteDistribution(pmf)
      }

      State(rank, fdM, fdV)
    }
  }

  case class Path(rank: Int,
                  steps: Int,
                  wordCntn: Double,
                  size: Double,
                  scale: Double,
                  states: List[State],
                  evolvedStates: List[State],
                  id: String) {
    def pickle =
      PickledPath(rank,
                  steps,
                  wordCntn,
                  size,
                  scale,
                  states map (_.pickle),
                  evolvedStates map (_.pickle),
                  id)

    def length = states.size

    lazy val evolution = {
      import AdjDiffbleFunction._
      def iterateSampDiff(lst: List[M], iterations: Int = 5) =
        iterate(sampleV(size) andthen genExtendM(lst))(iterations)
      iterateSampDiff(allMoves(rank), steps)
    }

    val current = states.last

    val initV = current.fdV

    lazy val evolved = {
      val mv = evolution.func((unifMoves(rank), initV))
      State(rank, mv._1, mv._2)
    }

    lazy val imagePres = toPresentation(rank, evolved.fdV)

    lazy val presentationFeedback =
      imagePres.rawfeedback(FreeGroups.Presentation.weight(wordCntn))

    lazy val presFn = genPresentationMoveFn(rank)

    lazy val imageFeedbackV: FiniteDistribution[V] =
      presFn.adjDer(evolved.fdV)(presentationFeedback)

    lazy val imageFeedback = projectV.adjDer(evolved.pair)(imageFeedbackV)

    lazy val (feedbackM, feedbackV) =
      evolution.adjDer(current.pair)(imageFeedback)

    lazy val (nextM, nextV) =
      (current.fdM ++ (feedbackM * scale), current.fdV ++ (feedbackV * scale))

    lazy val nextState = State(rank, nextM, nextV)

    lazy val next = Path(rank: Int,
                         steps: Int,
                         wordCntn: Double,
                         size: Double,
                         scale: Double,
                         states :+ nextState: List[State],
                         evolvedStates :+ evolved: List[State],
                         id)

    @annotation.tailrec
    final def quickrun(n: Int): Path =
      if (n < 1) this else { next.quickrun(n - 1) }

    @annotation.tailrec
    final def run(n: Int, callback: Path => Unit = (p) => {}): Path = {
      callback(this)
      if (n < 1) this
      else next.run(n - 1, callback)
    }
  }

  object Path {
    def init(rank: Int,
             steps: Int,
             wordCntn: Double = 0.5,
             size: Double = 1000,
             scale: Double = 1.0,
             id: String = "ephemeral") = {
      val state = State(rank, unifMoves(rank), eVec)
      Path(rank, steps, wordCntn, size, scale, List(state), List(), id)
    }
  }

  object PickledPath {
    implicit def rw: RW[PickledPath] = macroRW
  }

  case class PickledPath(rank: Int,
                         steps: Int,
                         wordCntn: Double,
                         size: Double,
                         scale: Double,
                         states: List[PickledState],
                         evolvedStates: List[PickledState],
                         id: String) {
    def unpickle =
      Path(rank,
           steps,
           wordCntn,
           size,
           scale,
           states map (_.unpickle),
           evolvedStates map (_.unpickle),
           id)
  }
}
